package encry.parser

import java.net.{InetAddress, InetSocketAddress}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import akka.actor.SupervisorStrategy.Resume
import scala.concurrent.duration._
import akka.actor.{Actor, ActorRef, OneForOneStrategy, SupervisorStrategy}
import com.typesafe.scalalogging.StrictLogging
import encry.blockchain.modifiers.{Block, Header}
import encry.blockchain.nodeRoutes.InfoRoute
import encry.blockchain.nodeRoutes.apiEntities.Peer
import encry.database.DBActor.{ActivateNodeAndGetNodeInfo, DropBlocksFromNode}
import encry.parser.NodeParser._
import encry.settings.ParseSettings
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

class NodeParser(node: InetSocketAddress,
                 parserContoller: ActorRef,
                 dbActor: ActorRef,
                 settings: ParseSettings) extends Actor with StrictLogging {

  val parserRequests: ParserRequests = ParserRequests(node)
  var currentNodeInfo: InfoRoute = InfoRoute.empty
  var currentNodeBestBlock: Block = Block.empty
  var currentNodeBestBlockId: String = ""
  var currentBestBlockHeight: AtomicInteger = new AtomicInteger(-1)
  val isRecovering: AtomicBoolean = new AtomicBoolean(false)
  var lastIds: List[String] = List.empty[String]
  var lastHeaders: List[Header] = List.empty[Header]

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
      currentBestBlockHeight.set(bestHeight)
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
          if (currentNodeInfo.fullHeight > currentBestBlockHeight.get()) self ! Recover
        }
      }
      parserRequests.getLastIds(100, currentNodeInfo.fullHeight) match {
        case Left(err) => logger.info(s"Error during request to $node: ${err.getMessage}")
        case Right(newLastHeaders) =>
          if (isRecovering.get() || currentBestBlockHeight.get() != currentNodeInfo.fullHeight)
            logger.info("Get last headers, but node is recovering, so ignore them")
          else {
            if (lastIds.nonEmpty) {
              val commonPoint = lastIds.reverse(lastIds.reverse.takeWhile(elem => !newLastHeaders.contains(elem)).length)
              val toDel = lastIds.reverse.takeWhile(_ != commonPoint)
              if (toDel.nonEmpty) self ! ResolveFork(commonPoint, toDel)
            }
            lastIds = newLastHeaders
            logger.info(s"Current last id is: ${lastIds.last}")
          }
      }
      parserRequests.getPeers match {
        case Left(err) => logger.info(s"Error during request to $node: ${err.getMessage}")
        case Right(peersList) =>
          parserContoller ! PeersList(peersList.collect {
            case peer  => {
              peer.address.getAddress}
          })
          logger.info(s"Send peer list: ${
            peersList.collect {
              case peer if peer.connectionType == "Outgoing" => peer.address.getAddress
            }
          } to parserController.")
      }
    case ResolveFork(fromBlock, toDel) =>
      logger.info(s"Resolving fork from block: $fromBlock")
      parserRequests.getBlock(fromBlock) match {
        case Left(err) => logger.info(s"Error during request to $node: ${err.getMessage}")
        case Right(block) =>
          currentNodeBestBlockId = block.header.id
          currentBestBlockHeight.set(block.header.height)
          dbActor ! DropBlocksFromNode(node, toDel)
          self ! Recover
      }
    case Recover if !isRecovering.get() =>
      recoverNodeChain(currentBestBlockHeight.get(), currentNodeInfo.fullHeight)
    case Recover => logger.info("Trying to recover, but recovering process is started")
    case _ =>
  }

  //side effect
  def updateBestBlock(): Unit = {
    parserRequests.getBlock(currentNodeInfo.bestFullHeaderId) match {
      case Left(err) => logger.info(s"Err: $err during update best block on parser $node")
      case Right(block) =>
        currentNodeBestBlock = block
        logger.info(s"Successfully update best block on $node to ${
          block.header.id
        }")
    }
  }

  def recoverNodeChain(start: Int, end: Int): Unit = {
    Future {
      isRecovering.set(true)
      (start to (start + settings.recoverBatchSize)).foreach {
        height =>
          val blocksAtHeight: List[String] = parserRequests.getBlocksAtHeight(height) match {
            case Left(err) => logger.info(s"Err: $err during get block at height $height")
              List.empty
            case Right(blocks) => blocks
          }
          blocksAtHeight.headOption.foreach(blockId =>
            parserRequests.getBlock(blockId) match {
              case Left(err) => logger.info(s"Error during getting block $blockId: ${
                err.getMessage
              }")
              case Right(block) =>
                if (currentBestBlockHeight.get() != (start + settings.recoverBatchSize)) {
                  currentNodeBestBlockId = block.header.id
                  currentBestBlockHeight.set(block.header.height)
                  dbActor ! BlockFromNode(block, node, currentNodeInfo)
                  context.become(awaitDb)
                }
            }
          )
      }
      isRecovering.set(false)
    }
  }

  def awaitDb: Receive = {
    case GetCurrentHeight(height: Int) =>
      if (height == currentBestBlockHeight.get()) {
        context.become(workingCycle)
        if (height != currentNodeInfo.fullHeight) { self ! Recover }
      }
    case _ =>
  }

}


object NodeParser {

  case class PeersList(peers: List[InetAddress])

  case object PingNode

  case class GetCurrentHeight(height: Int)

  case object CheckForRollback

  case class SetNodeParams(bestFullBlock: String, bestHeaderHeight: Int)

  case class BlockFromNode(block: Block, nodeAddr: InetSocketAddress, nodeInfo: InfoRoute)

  case class ResolveFork(fromBlock: String, toDel: List[String])

  case object Recover

}