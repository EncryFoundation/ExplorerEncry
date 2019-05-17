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

  override def preStart(): Unit = {
    settings.nodes.foreach(node =>
     context.system.actorOf(Props(new NodeParser(node, self, dbActor, settings)).withDispatcher("parser-dispatcher"),
        s"ParserFor${node.getHostName}")
    )
  }
  var currentListeningPeers: List[InetAddress] = List.empty[InetAddress]

  override def receive: Receive = {
    case PeersList(peers) =>
      val fp: List[InetAddress] = peers.diff(currentListeningPeers)
      fp.foreach { peer =>
        context.system.actorOf(Props(new SimpleNodeParser(new InetSocketAddress(peer, 9051), self, dbActor, settings)),
          s"ParserFor${peer.getHostName}")
              }
        currentListeningPeers = currentListeningPeers ++: fp
    case _ =>
  }
}