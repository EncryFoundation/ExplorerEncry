package encry.parser

import java.net.{InetAddress, InetSocketAddress}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import scala.concurrent.duration._
import akka.actor.{Actor, ActorRef}
import com.typesafe.scalalogging.StrictLogging
import encry.blockchain.modifiers.{Block, Header}
import encry.blockchain.nodeRoutes.InfoRoute
import encry.database.DBActor.{ActivateNodeAndGetNodeInfo, DropBlocksFromNode, UpdatedInfoAboutNode}
import encry.parser.NodeParser._
import encry.settings.ParseSettings

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

class NodeParser(node: InetSocketAddress,
                 parserController: ActorRef,
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
  var numberOfRejectedRequests: Int = 0

  override def preStart(): Unit = {
    logger.info(s"Start monitoring: ${node.getAddress}")
    context.system.scheduler.schedule(10.seconds, 10.seconds)(self ! PingNode)
  }

  override def postStop(): Unit = {
    logger.info(s"Actor $node stopped")
      dbActor ! UpdatedInfoAboutNode(node, currentNodeInfo, status = false)
  }

  override def receive: Receive = prepareCycle

  def prepareCycle: Receive = {
    case PingNode  =>
      parserRequests.getInfo match {
        case Left(err) =>
          println(s"$err with $sender")
          numberOfRejectedRequests += 1
          logger.info(s"Error during request to $node: ${err.getMessage}")
        case Right(infoRoute) =>
          logger.info(s"Get node info on $node during prepare status")
          dbActor ! ActivateNodeAndGetNodeInfo(node, infoRoute)
      }
//    case PingNode =>
//      logger.info(s"Number of attempts has expired! Stop self actor for: $node and remove this node from listening peers!")
//      parserController ! PeerForRemove(node.getAddress)
//      context.stop(self)

    case SetNodeParams(bestBlock, bestHeight) =>
      currentNodeBestBlockId = bestBlock
      currentBestBlockHeight.set(bestHeight)
      logger.info(s"Get currentNodeBestBlockId: $currentNodeBestBlockId. currentBestBlockHeight: $currentBestBlockHeight")
      context.become(workingCycle)
  }

  def workingCycle: Receive = {
    case PingNode if numberOfRejectedRequests < 3 =>
      parserRequests.getInfo match {
        case Left(err) =>
//          numberOfRejectedRequests += 1
          logger.info(s"workingCycle Error during request to $node: ${err.getMessage}")
        case Right(newInfoRoute) if newInfoRoute != currentNodeInfo =>
          logger.info(s"workingCycle Update node info on $node to $newInfoRoute|${newInfoRoute == currentNodeInfo}")
            dbActor ! UpdatedInfoAboutNode(node, newInfoRoute, status = true)
            currentNodeInfo = newInfoRoute
          if (currentNodeInfo.fullHeight > currentBestBlockHeight.get()) self ! Recover

        case Right(_) => logger.info(s"Info route on node $node don't change")
      }

      parserRequests.getPeers match {
        case Left(err) =>
          numberOfRejectedRequests += 1
          logger.info(s"Error during getting Peers request to $node: ${err.getMessage} from SimpleParserController.")
        case Right(peersList) =>
          val peersCollection: Set[InetAddress] = peersList.collect {
            case peer =>
              peer.address.getAddress
          }.toSet
          logger.info(s"Got new peers: ${peersCollection.mkString(",")} from Api on NP for: $node. " +
            s"Sending new peers to parser controller.")
          parserController ! PeersFromApi(peersCollection)
      }

      parserRequests.getLastIds(100, currentNodeInfo.fullHeight) match {
        case Left(err) =>
          numberOfRejectedRequests += 1
          logger.info(s"Error during request to $node: ${err.getMessage}")
        case Right(newLastHeaders) =>
          if (isRecovering.get() || currentBestBlockHeight.get() != currentNodeInfo.fullHeight)
            logger.info("Get last headers, but node is recovering, so ignore them")
          else {
            if (lastIds.nonEmpty) {
              val commonPoint: String = lastIds.reverse(lastIds.reverse.takeWhile(elem => !newLastHeaders.contains(elem)).length)
              val toDelIds: List[String] = lastIds.reverse.takeWhile(_ != commonPoint)
              logger.info(s"common point = $commonPoint / toDel = $toDelIds")
              val toDel = toDelIds.map(parserRequests.getBlock).collect{ case Right(blockToDrop) => blockToDrop }
              if (toDel.nonEmpty && toDelIds.length == toDel.length) self ! ResolveFork(commonPoint, toDel)
            }
            lastIds = newLastHeaders
            logger.info(s"Current last id is: ${lastIds.last}")
          }
      }

//    case PingNode =>
//      logger.info(s"Number of attempts has expired! Stop self actor for: $node and remove this node from listening peers!")
//      parserController ! PeerForRemove(node.getAddress)
//      context.stop(self)

    case ResolveFork(fromBlock, toDel) =>
      logger.info(s"Resolving fork from block: $fromBlock")
      parserRequests.getBlock(fromBlock) match {
        case Left(err) => logger.info(s"Error during request to $node: ${err.getMessage}")
        case Right(block) =>
          logger.info(s"RIGHT LOOP")
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

  def recoverNodeChain(start: Int, end: Int): Unit = Future {
    isRecovering.set(true)
    (start to (start + settings.recoverBatchSize)).foreach { height =>
      val blocksAtHeight: List[String] = parserRequests.getBlocksAtHeight(height) match {
        case Left(err) =>
          logger.info(s"Err: $err during get block at height $height")
          List.empty
        case Right(blocks) => blocks
      }
      blocksAtHeight.headOption.foreach(blockId =>
        parserRequests.getBlock(blockId) match {
          case Left(err) => logger.info(s"Error during getting block $blockId: ${err.getMessage}")
          case Right(block) =>
            if (currentBestBlockHeight.get() != (start + settings.recoverBatchSize)) {
              currentNodeBestBlockId = block.header.id
              currentBestBlockHeight.set(block.header.height)
              dbActor ! BlockFromNode(block, node, currentNodeInfo)
            }
        })
    }
    isRecovering.set(false)
    if (currentBestBlockHeight.get() == currentNodeInfo.fullHeight) context.become(workingCycle)
    if (currentBestBlockHeight.get() == (start + settings.recoverBatchSize)) {context.become(awaitDb)}
  }

  def awaitDb: Receive = {
    case GetCurrentHeight(height) =>
      if (height == currentBestBlockHeight.get()) {
        context.become(workingCycle)
        if (height != currentNodeInfo.fullHeight) {
          self ! Recover
        }
      }
    case _ =>
  }
}


object NodeParser {

  case class PeersFromApi(peers: Set[InetAddress])

  case object PingNode

  case class GetCurrentHeight(height: Int)

  case object CheckForRollback

  case class SetNodeParams(bestFullBlock: String, bestHeaderHeight: Int)

  case class BlockFromNode(block: Block, nodeAddr: InetSocketAddress, nodeInfo: InfoRoute)

  case class ResolveFork(fromBlock: String, toDel: List[Block])

  case object Recover
}