package encry.parser

import java.net.{InetAddress, InetSocketAddress}

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorRef, Kill, OneForOneStrategy, PoisonPill, Props, SupervisorStrategy}
import com.typesafe.scalalogging.StrictLogging
import encry.blockchain.nodeRoutes.InfoRoute
import encry.database.DBActor.UpdatedInfoAboutNode
import encry.parser.NodeParser.{PeersFromApi, PingNode}
import encry.parser.SimpleNodeParser.PeerForRemove
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

  override def receive: Receive = initialPingBehaviour

  def initialPingBehaviour: Receive = {
    case PingNode =>
      var isConnected: Boolean = false
      parserRequests.getInfo match {
        case Left(_) =>
        case Right(_) => isConnected = true
      }
      if (isConnected) {
        println(s"Got response from $node. Starting working cycle")
        context.become(workingCycle)
      } else {
        println(s"No response from: $node. Stop self ")
        context.stop(self)
      }
  }

  def workingCycle: Receive = {
    case PingNode if numberOfRejectedRequests < 3 =>
      parserRequests.getInfo match {
        case Left(err) =>
          numberOfRejectedRequests += 1
          logger.info(s"Error during getting Info request to $node: ${err.getMessage} from SimpleParserController." +
            s" Add +1 attempt to numberOfRejectedRequests. current is: $numberOfRejectedRequests.")
        case Right(infoRoute)  =>
          if (infoRoute != currentNodeInfo){
//          logger.info(s"Got new information form Api on SNP for: $node. Sending update to DB...")
          dbActor ! UpdatedInfoAboutNode(node, infoRoute)
          currentNodeInfo = infoRoute
          }
//        case Right(_) => logger.info(s"Got outdated information from Api on SNP for: $node.")
      }
      parserRequests.getPeers match {
        case Left(err) =>
          numberOfRejectedRequests += 1
          logger.info(s"Error during getting Peers request to $node: ${err.getMessage} from SimpleParserController." +
            s" Add +1 attempt to numberOfRejectedRequests. current is: $numberOfRejectedRequests.")
        case Right(peersList) =>
          //todo: add correct filter
          val peersCollection: Set[InetAddress] = peersList.collect {
            case peer => peer.address.getAddress
          }.toSet
//          logger.info(s"Got new peers: ${peersCollection.mkString(",")} from Api on SNP for: $node. " +
//            s"Sending new peers to parser controller.")
          parserController ! PeersFromApi(peersCollection)
      }
    case PingNode =>
      logger.info(s"Number of attempts has expired! Stop self actor for: $node and remove this node from listening peers!")
//      parserController ! PeerForRemove(node.getAddress)
      context.stop(self)

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