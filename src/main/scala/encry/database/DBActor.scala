package encry.database

import java.net.{InetAddress, InetSocketAddress}

import akka.actor.Actor
import com.typesafe.scalalogging.StrictLogging
import encry.blockchain.nodeRoutes.InfoRoute
import encry.database.DBActor.ActivateNodeAndGetNodeInfo
import encry.parser.NodeParser.SetNodeParams
import encry.settings.DatabaseSettings

import scala.concurrent.duration._
import scala.concurrent.Await

class DBActor(settings: DatabaseSettings) extends Actor with StrictLogging {

  val dbService = DBService(settings)

  override def receive: Receive = {
    case ActivateNodeAndGetNodeInfo(addr: InetSocketAddress, infoRoute: InfoRoute) =>
      val res = Await.result(dbService.activateOrGetNodeInfo(addr, infoRoute), 3.minutes)
      res.foreach(nodeInfo => sender() ! SetNodeParams(nodeInfo.lastFullBlock, nodeInfo.lastFullHeight))
  }
}

object DBActor {

  case class PrepareInfoForNode(addr: InetAddress)

  case class ActivateNodeAndGetNodeInfo(addr: InetSocketAddress, infoRoute: InfoRoute)
}
