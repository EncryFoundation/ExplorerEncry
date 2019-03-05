package encry.parser

import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.duration._
import akka.actor.{Actor, ActorRef}
import com.typesafe.scalalogging.StrictLogging
import encry.blockchain.modifiers.{Block, Header}
import encry.blockchain.nodeRoutes.InfoRoute
import encry.database.DBActor.ActivateNodeAndGetNodeInfo
import encry.parser.NodeParser.{BlockFromNode, PingNode, SetNodeParams}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class NodeParser(node: InetSocketAddress, parserContoller: ActorRef, dbActor: ActorRef) extends Actor with StrictLogging {

  val parserRequests: ParserRequests = ParserRequests(node)
  var currentNodeInfo: InfoRoute = InfoRoute.empty
  var currentNodeBestBlock: Block = Block.empty
  var currentNodeBestBlockId: String = ""
  var currentBestBlockHeight: Int = -1
  val isRecovering: AtomicBoolean = new AtomicBoolean(false)


  override def preStart(): Unit = {
    logger.info(s"Start monitoring: ${node.getAddress}")
    context.system.scheduler.schedule(
      10 seconds,
      10 seconds
    )(self ! PingNode)
  }

  override def receive: Receive = prepareCycle

  def prepareCycle: Receive = {
    case PingNode =>
      parserRequests.getInfo match {
        case Left(err) => logger.info(s"Error during request to $node: ${err.getMessage}")
        case Right(infoRoute) =>
          logger.info(s"Get node info on $node during prepare status")
          dbActor ! ActivateNodeAndGetNodeInfo(node, infoRoute)
      }
    case SetNodeParams(bestBlock, bestHeight) =>
      currentNodeBestBlockId = bestBlock
      currentBestBlockHeight = bestHeight
      logger.info(s"Get currentNodeBestBlockId: $currentNodeBestBlockId. currentBestBlockHeight: $currentBestBlockHeight")
      context.become(workingCycle)
  }

  def workingCycle: Receive = {
    case PingNode =>
      parserRequests.getInfo match {
        case Left(err) => logger.info(s"Error during request to $node: ${err.getMessage}")
        case Right(newInfoRoute) => if (newInfoRoute == currentNodeInfo)
          logger.info(s"info route on node $node don't change")
        else {
          logger.info(s"Update node info on $node to $newInfoRoute|${newInfoRoute == currentNodeInfo}")
          currentNodeInfo = newInfoRoute
          if (!isRecovering.get()) recoverNodeChain(currentBestBlockHeight, newInfoRoute.fullHeight)
        }
      }
    case _ =>
  }

  //side effect
  def updateBestBlock(): Unit = {
    parserRequests.getBlock(currentNodeInfo.bestFullHeaderId) match {
      case Left(err) => logger.info(s"Err: $err during update best block on parser $node")
      case Right(block) =>
        currentNodeBestBlock = block
        logger.info(s"Successfully update best block on $node to ${block.header.id}")
    }
  }

  def recoverNodeChain(start: Int, end: Int): Unit = {
    isRecovering.set(true)
    (start to end).foreach{ height =>
      val blocksAtHeight: List[String] = parserRequests.getBlocksAtHeight(height) match {
        case Left(err) => logger.info(s"Err: $err during get block at height $height")
          List.empty
        case Right(blocks) => blocks
      }
      blocksAtHeight.headOption.foreach(blockId =>
        parserRequests.getBlock(blockId) match {
          case Left(err) => logger.info(s"Error during getting block $blockId: ${err.getMessage}")
          case Right(block) =>
            currentNodeBestBlockId = block.header.id
            currentBestBlockHeight = block.header.height
            dbActor ! BlockFromNode(block, node)
        }
      )
    }
    isRecovering.set(false)
  }
}

object NodeParser {

  case object PingNode

  case object CheckForRollback

  case class SetNodeParams(bestFullBlock: String, bestHeaderHeight: Int)

  case class BlockFromNode(block: Block, nodeAddr: InetSocketAddress)
}

