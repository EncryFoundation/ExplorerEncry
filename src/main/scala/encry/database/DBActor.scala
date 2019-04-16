package encry.database

import java.net.{InetAddress, InetSocketAddress}

import akka.actor
import akka.actor.Actor
import akka.stream.StreamRefMessages.ActorRef
import com.typesafe.scalalogging.StrictLogging
import encry.blockchain.nodeRoutes.InfoRoute
import encry.database.DBActor.{ActivateNodeAndGetNodeInfo, DropBlocksFromNode}
import encry.parser.NodeParser
import encry.parser.NodeParser.{BlockFromNode, GetCurrentHeight, SetNodeParams}
import encry.settings.DatabaseSettings

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class DBActor(settings: DatabaseSettings) extends Actor with StrictLogging {

  val dbService = DBService(settings)
  implicit val ec = context.dispatcher

  override def receive: Receive = {
    case ActivateNodeAndGetNodeInfo(addr: InetSocketAddress, infoRoute: InfoRoute) =>
      val res = Await.result(dbService.activateOrGetNodeInfo(addr, infoRoute), 3.minutes)
      res.foreach(nodeInfo => sender() ! SetNodeParams(nodeInfo.lastFullBlock, nodeInfo.lastFullHeight))
    case BlockFromNode(block, nodeAddr) =>
      logger.info(s"Insert block with id: ${block.header.id} on height ${block.header.height} from node ${nodeAddr.getAddress.getHostAddress}")
      dbService.insertBlockFromNode(block, nodeAddr)
      val height = block.header.height
      sender() ! GetCurrentHeight(height)
     case DropBlocksFromNode(addr: InetSocketAddress, blocks: List[String]) =>
      blocks.foreach(blockId => dbService.deleteBlocksFromNode(addr, blockId))
  }
}

object DBActor {

  case class PrepareInfoForNode(addr: InetAddress)

  case class ActivateNodeAndGetNodeInfo(addr: InetSocketAddress, infoRoute: InfoRoute)

  case class DropBlocksFromNode(addr: InetSocketAddress, blocks: List[String])
}
