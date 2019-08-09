package encry.parser

import java.net.{ConnectException, InetAddress, InetSocketAddress}

import akka.actor.{Actor, ActorRef, Kill, OneForOneStrategy, PoisonPill, Props, SupervisorStrategy}
import com.typesafe.scalalogging.StrictLogging
import encry.ParsersController.BadPeer
import encry.blockchain.nodeRoutes.InfoRoute
import encry.database.DBActor.UpdatedInfoAboutNode
import encry.parser.NodeParser.{PeersFromApi, PingNode}
import encry.settings.ParseSettings

import scala.concurrent.duration._

class SimpleNodeParser(node: InetSocketAddress,
                       parserController: ActorRef,
                       dbActor: ActorRef,
                       settings: ParseSettings) extends Actor with StrictLogging {

  import context.dispatcher

  val parserRequests: ParserRequests = ParserRequests(node)
  var currentNodeInfo: InfoRoute = InfoRoute.empty
  var numberOfRejectedRequests: Int = 0

  override def preStart(): Unit = {
    println(s"Starting SNP for $node")
    context.system.scheduler.schedule(10.seconds, 10.seconds) (self ! PingNode)
  }

  override def postStop(): Unit = {
    logger.info(s"Actor $node stopped!!!")
    parserController ! BadPeer(node.getAddress)
    dbActor ! UpdatedInfoAboutNode(node, currentNodeInfo, status = false)
  }

  override def receive: Receive = initialPingBehaviour

  def initialPingBehaviour: Receive = {
    case PingNode if !settings.askNode && (numberOfRejectedRequests > 3) =>
      logger.info(s"No response from: $node. Stop self")
      context.stop(self)
    case PingNode =>
      var isConnected: Boolean = false
        parserRequests.getInfo match {
          case Left(th) =>
            logger.warn(s"Can't get node info ${node.getAddress} during initial ping behaviour. " +
              s"Current number of rejected requests if ${numberOfRejectedRequests + 1}. Cause: ${th.getMessage}")
            if (!settings.askNode) numberOfRejectedRequests += 1
          case Right(_) => isConnected = true
        }
      if (isConnected) {
        logger.info(s"Got response from $node. Starting working cycle")
        numberOfRejectedRequests = 0
        context.become(workingCycle)
      }
  }

  def workingCycle: Receive = {
    case PingNode if !settings.askNode && (numberOfRejectedRequests > 3) =>
      logger.info(s"No response from: $node. Stop self")
      context.stop(self)
    case PingNode =>
      parserRequests.getInfo match {
        case Left(th) =>
          if (!settings.askNode) numberOfRejectedRequests += 1
          logger.warn(s"Error during getting Info request to $node from SimpleParserController." +
            s" Add +1 attempt to numberOfRejectedRequests. current is: $numberOfRejectedRequests.", th)
        case Right(infoRoute)  =>
          if (infoRoute != currentNodeInfo){
//          logger.info(s"Got new information form Api on SNP for: $node. Sending update to DB...")
          dbActor ! UpdatedInfoAboutNode(node, infoRoute, status = true)
          currentNodeInfo = infoRoute
          }
//        case Right(_) => logger.info(s"Got outdated information from Api on SNP for: $node.")
      }
      parserRequests.getPeers match {
        case Left(th) =>
          if (!settings.askNode) numberOfRejectedRequests += 1
          logger.warn(s"Error during getting Peers request to $node from SimpleParserController." +
            s" Add +1 attempt to numberOfRejectedRequests. current is: $numberOfRejectedRequests.", th)
        case Right(peersList) =>
          //todo: add correct filter
          val peersCollection: Set[InetAddress] = peersList.collect {
            case peer => peer.address.getAddress
          }.toSet
//          logger.info(s"Got new peers: ${peersCollection.mkString(",")} from Api on SNP for: $node. " +
//            s"Sending new peers to parser controller.")
          parserController ! PeersFromApi(peersCollection)
      }

    case msg => logger.info(s"Got strange message on SimpleNodeParser connected to $node: $msg.")
  }

}

object SimpleNodeParser {

  def props(node: InetSocketAddress,
            parserController: ActorRef,
            dbActor: ActorRef,
            settings: ParseSettings): Props = Props(new SimpleNodeParser(node, parserController, dbActor, settings))

  case class PeerForRemove(peer: InetAddress)
}