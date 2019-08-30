package encry.parser

import java.net.{InetAddress, InetSocketAddress}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import scala.concurrent.duration._
import akka.actor.{Actor, ActorRef}
import cats.kernel.Monoid
import com.typesafe.scalalogging.StrictLogging
import encry.blockchain.modifiers.{Block, Header}
import encry.blockchain.nodeRoutes.InfoRoute
import encry.database.DBActor.{ActivateNodeAndGetNodeInfo, DropBlocksFromNode, RecoveryMode, RequestBlocksIds, RequestedIdsToDelete, UpdatedInfoAboutNode}
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
  val maxNumberOfRejects: Option[Int] = if (settings.infinitePing) None else settings.numberOfAttempts
  var blocksToReask: Set[Int] = Set.empty
  var blocksToWrite: Set[String] = Set.empty

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
    case PingNode if !settings.infinitePing && maxNumberOfRejects.exists(_ >= numberOfRejectedRequests) =>
      logger.info(s"No response from: $node. Stop self")
      context.stop(self)
    case PingNode =>
      parserRequests.getInfo match {
        case Left(th) =>
          if (!settings.infinitePing) numberOfRejectedRequests += 1
          logger.warn(s"Error during request to during prepareCycle $node", th)
        case Right(infoRoute) =>
          logger.info(s"Get node info on $node during prepare status")
          dbActor ! ActivateNodeAndGetNodeInfo(node, infoRoute)
      }

    case SetNodeParams(bestBlock, bestHeight) =>
      currentNodeBestBlockId = bestBlock
      currentBestBlockHeight.set(bestHeight)
      numberOfRejectedRequests = 0
      logger.info(s"Get currentNodeBestBlockId: $currentNodeBestBlockId. currentBestBlockHeight: $currentBestBlockHeight")
      context.become(workingCycle)
  }

  def workingCycle: Receive = {
    case GetCurrentHeight(_, blockId) => blocksToWrite -= blockId

    case PingNode if !settings.infinitePing && maxNumberOfRejects.exists(_ >= numberOfRejectedRequests) =>
      logger.info(s"No response from: $node. Stop self")
      context.stop(self)

    case PingNode if !isRecovering.get() =>
      reaskBlocks

      parserRequests.getInfo match {
        case Left(th) =>
          if (!settings.infinitePing) numberOfRejectedRequests += 1
          logger.warn(s"workingCycle error during request to $node", th.getMessage)
        case Right(newInfoRoute) if newInfoRoute != currentNodeInfo =>
          logger.info(s"workingCycle Update node info on $node to $newInfoRoute|${newInfoRoute == currentNodeInfo}")
            dbActor ! UpdatedInfoAboutNode(node, newInfoRoute, status = true)
            currentNodeInfo = newInfoRoute
          if (currentNodeInfo.fullHeight > currentBestBlockHeight.get()) self ! Recover

        case Right(_) => logger.info(s"Info route on node $node don't change")
      }

      parserRequests.getPeers match {
        case Left(th) =>
          if (!settings.infinitePing) numberOfRejectedRequests += 1
          logger.warn(s"Error during getting Peers request to $node", th.getMessage)
        case Right(peersList) =>
          val peersCollection: Set[InetAddress] = peersList.collect {
            case peer =>
              peer.address.getAddress
          }.toSet
          logger.info(s"Got new peers: ${peersCollection.mkString(",")} from Api on NP for: $node. " +
            s"Sending new peers to parser controller.")
          parserController ! PeersFromApi(peersCollection)
      }

      calculateCommonPoint(15)

    case PingNode => reaskBlocks

    case ResolveFork(fromBlock, toDel) =>
      logger.info(s"Resolving fork from block: $fromBlock")
      parserRequests.getBlock(fromBlock) match {
        case Left(th) =>
          if (!settings.infinitePing) numberOfRejectedRequests += 1
          logger.warn(s"Error during getting block $fromBlock during fork resolution to $node", th.getMessage)
        case Right(block) =>
          logger.info(s"RIGHT LOOP")
          currentNodeBestBlockId = block.header.id
          currentBestBlockHeight.set(block.header.height)
          dbActor ! DropBlocksFromNode(node, toDel)
          requestBlockIdsFromDb(currentBestBlockHeight.get() + 1, currentNodeInfo.fullHeight)
      }
    case Recover if !isRecovering.get() =>
      logger.info("Starting recovery process")
      recoverNodeChain(currentBestBlockHeight.get() + 1, currentNodeInfo.fullHeight)
    case Recover => logger.info("Trying to recover, but recovering process is started")
    case RequestedIdsToDelete(from, to, ids) if isRecovering.get() =>
      val blocksToDelete = ids.map(parserRequests.getBlock).collect{ case Right(blockToDrop) => blockToDrop }
      dbActor ! DropBlocksFromNode(node, blocksToDelete)
      recoverNodeChain(from, to)
    case _ =>
  }

  def calculateCommonPoint(depth: Int): Unit = parserRequests.getLastIds(depth, currentNodeInfo.fullHeight) match {
    case Left(th) =>
      if (!settings.infinitePing) numberOfRejectedRequests += 1
      logger.warn(s"Error during getting last ids to $node", th.getCause)
    case Right(newLastHeaders) =>
      if (isRecovering.get() || currentBestBlockHeight.get() != currentNodeInfo.fullHeight)
        logger.info("Get last headers, but node is recovering, so ignore them")
      else {
        if (lastIds.nonEmpty) {
          val commonPointOpt: Option[String] = lastIds.reverse.lift(lastIds.reverse.takeWhile(elem => !newLastHeaders.contains(elem)).length)
          commonPointOpt match {
            case Some(commonPoint) =>
              val toDelIds: List[String] = lastIds.reverse.takeWhile(_ != commonPoint)
              if (toDelIds.nonEmpty) logger.info(s"common point = $commonPoint / toDel = $toDelIds")
              val toDel: List[Block] = toDelIds.map(parserRequests.getBlock).collect{ case Right(blockToDrop) => blockToDrop }
              if (toDel.nonEmpty && toDelIds.length == toDel.length) self ! ResolveFork(commonPoint, toDel)
            case None if depth + 15 <= currentNodeInfo.fullHeight => calculateCommonPoint(depth + 100)
            case None =>
          }
        }
        lastIds = newLastHeaders
        logger.info(s"Current last id is: ${lastIds.last}")
      }
  }

  //side effect
  def updateBestBlock(): Unit = {
    parserRequests.getBlock(currentNodeInfo.bestFullHeaderId) match {
      case Left(th) =>
        if (!settings.infinitePing) numberOfRejectedRequests += 1
        logger.warn(s"Error during update best block on parser $node", th.getMessage)
      case Right(block) =>
        currentNodeBestBlock = block
        logger.info(s"Successfully update best block on $node to ${block.header.id}")
    }
  }

  def reaskBlocks(): Unit = blocksToReask.foreach { height =>
    logger.info(s"Going to reask ${blocksToReask.size} blocks")
    parserRequests.getBlocksAtHeight(height) match {
      case Left(th) =>
        if (!settings.infinitePing) numberOfRejectedRequests += 1
        logger.warn(s"Error during receiving list of block at height $height", th.getMessage)
      case Right(blocks) => blocks.headOption.foreach { blockId =>
        parserRequests.getBlock(blockId).map { block =>
          blocksToReask -= height
          dbActor ! BlockFromNode(block, node, currentNodeInfo)
        }
      }
    }
    //parserRequests.getBlock(blockId) match {
    //  case Left(th) =>
    //    if (!settings.infinitePing) numberOfRejectedRequests += 1
    //    logger.warn(s"Error during getting block $blockId", th.getCause)
    //  case Right(block) =>
    //    blocksToReask -= blockId
    //    dbActor ! BlockFromNode(block, node, currentNodeInfo)
    //}
  }

  def requestBlockIdsFromDb(start: Int, end: Int): Unit = {
    isRecovering.set(true)
    dbActor ! RecoveryMode(true)
    val realEnd = math.min(start + settings.recoverBatchSize, end)
    logger.info(s"Requesting ids of blocks to delete starting from height $start to $realEnd")
    dbActor ! RequestBlocksIds(start, realEnd)
  }

  def recoverNodeChain(start: Int, end: Int): Unit = Future {
    isRecovering.set(true)
    dbActor ! RecoveryMode(true)
    val realEnd = math.min(start + settings.recoverBatchSize, end)
    logger.info(s"Recovering from $start to $realEnd")
    (start to realEnd).foreach { height =>
      val blocksAtHeight: List[String] = parserRequests.getBlocksAtHeight(height) match {
        case Left(th) =>
          if (!settings.infinitePing) numberOfRejectedRequests += 1
          logger.warn(s"Error during receiving list of block at height $height", th.getMessage)
          List.empty
        case Right(blocks) => blocks
      }
      blocksAtHeight.headOption.foreach { blockId =>
        logger.info(s"Best block at $height is $blockId")
        parserRequests.getBlock(blockId) match {
          case Left(th) =>
            if (!settings.infinitePing) numberOfRejectedRequests += 1
            logger.warn(s"Error during getting block $blockId", th)
            blocksToReask += height
          case Right(block) =>
            if (currentBestBlockHeight.get() != realEnd) {
              logger.info(s"Got block $blockId at height $height, writing to db")
              currentNodeBestBlockId = block.header.id
              currentBestBlockHeight.set(block.header.height)
              blocksToWrite += blockId
              dbActor ! BlockFromNode(block, node, currentNodeInfo)
            }
        }}
    }
    if (currentBestBlockHeight.get() == realEnd) {
      logger.info("Switching to awaitDb")
      context.become(awaitDb)
    }
  }

  def awaitDb: Receive = {
    case GetCurrentHeight(height, blockId) =>
      logger.info(s"last height is $height")
      blocksToWrite -= blockId
      logger.info(s"blocksToWrite size is ${blocksToWrite.size}")
      if (blocksToWrite.isEmpty) {
        isRecovering.set(false)
        dbActor ! RecoveryMode(false)
        context.become(workingCycle)
        if (height != currentNodeInfo.fullHeight) {
          self ! Recover
        }
      }
    case x => println(x)
  }
}


object NodeParser {

  case class PeersFromApi(peers: Set[InetAddress])

  case object PingNode

  case class GetCurrentHeight(height: Int, blockId: String)

  case object CheckForRollback

  case class SetNodeParams(bestFullBlock: String, bestHeaderHeight: Int)

  case class BlockFromNode(block: Block, nodeAddr: InetSocketAddress, nodeInfo: InfoRoute)

  case class ResolveFork(fromBlock: String, toDel: List[Block])

  case object Recover
}