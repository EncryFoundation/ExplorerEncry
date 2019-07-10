package encry.database

import java.net.{InetAddress, InetSocketAddress}

import akka.pattern._
import akka.actor.Actor
import com.typesafe.scalalogging.StrictLogging
import encry.blockchain.modifiers.Block
import encry.blockchain.nodeRoutes.InfoRoute
import encry.database.DBActor.{ActivateNodeAndGetNodeInfo, DropBlocksFromNode, UpdatedInfoAboutNode}
import encry.parser.NodeParser.{BlockFromNode, GetCurrentHeight, SetNodeParams}
import encry.settings.DatabaseSettings

import scala.concurrent.{Await, ExecutionContextExecutor}

class DBActor(settings: DatabaseSettings) extends Actor with StrictLogging {

  val dbService = DBService(settings)
  implicit val ec: ExecutionContextExecutor = context.dispatcher

  override def receive: Receive = {
    case ActivateNodeAndGetNodeInfo(addr: InetSocketAddress, infoRoute: InfoRoute) =>
      dbService
        .activateOrGetNodeInfo(addr, infoRoute)
        .map(nodeInfo => SetNodeParams(nodeInfo.id, nodeInfo.height))
        .pipeTo(sender())
      //res.foreach(nodeInfo => sender() ! SetNodeParams(nodeInfo.id, nodeInfo.height))

    case UpdatedInfoAboutNode(addr: InetSocketAddress, infoRoute: InfoRoute, status: Boolean) =>
      dbService.activateNode(addr, infoRoute, status)

    case BlockFromNode(block, nodeAddr, nodeInfo) =>
      logger.info(s"Insert block with id: ${block.header.id} on height ${block.header.height} " +
        s"from node ${nodeAddr.getAddress.getHostAddress}")
      dbService.insertBlockFromNode(block, nodeAddr, nodeInfo)
      val height = block.header.height
      sender() ! GetCurrentHeight(height)

    case DropBlocksFromNode(addr: InetSocketAddress, blocks: List[Block]) =>
      blocks.foreach(block => dbService.deleteBlock(addr, block))
  }
}

object DBActor {

  case class PrepareInfoForNode(addr: InetAddress)

  case class ActivateNodeAndGetNodeInfo(addr: InetSocketAddress, infoRoute: InfoRoute)

  case class UpdatedInfoAboutNode(addr: InetSocketAddress, infoRoute: InfoRoute, status: Boolean)

  case class DropBlocksFromNode(addr: InetSocketAddress, blocks: List[Block])

}