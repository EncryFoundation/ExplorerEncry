package encry

import java.net.{InetAddress, InetSocketAddress}
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import encry.parser.{NodeParser, SimpleNodeParser}
import encry.settings.ParseSettings
import ExplorerApp._
import akka.actor.SupervisorStrategy.Resume
import com.typesafe.scalalogging.StrictLogging
import encry.parser.NodeParser.PeersList

import scala.concurrent.duration._

class ParsersController(settings: ParseSettings, dbActor: ActorRef) extends Actor with StrictLogging {

  override def supervisorStrategy: SupervisorStrategy = {
    OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 10 seconds) {
      case _ => Resume
    }
  }

  var currentListeningPeers: List[InetAddress] = List.empty[InetAddress]

  override def preStart(): Unit = {
    settings.nodes.foreach { node =>
//      println(s"$node / node")
//      self ! PeersList(List(node.getAddress.getHostName))
      currentListeningPeers == currentListeningPeers ++: List(node.getAddress)
      context.system.actorOf(Props(new NodeParser(node, self, dbActor, settings)).withDispatcher("parser-dispatcher"),
        s"ParserFor${node.getHostName}")
    }
  }

  override def receive: Receive = {
    case PeersList(peers) =>
      println(s"$currentListeningPeers / currentlisteningPeer")
//      println(s"$peers / peers")
      val fp: List[InetAddress] = peers.distinct.diff(currentListeningPeers)
//      println(s"$fp / fp")
      logger.info(s"ParserContreoller got peers list. Peers before diff ${peers.mkString(",")}, resulted collection is: " +
        s"${fp.mkString(",")}")
      fp.foreach { peer =>
        context.system.actorOf(Props(new SimpleNodeParser(new InetSocketAddress(peer, 9051), self, dbActor, settings)),
          s"SimpleParserFor${peer.getHostName}")
              }
        currentListeningPeers = currentListeningPeers ++: fp
    case _ =>
  }
}