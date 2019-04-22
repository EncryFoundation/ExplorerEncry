package encry

import java.net.{InetAddress, InetSocketAddress}
import akka.actor.{Actor, ActorRef, Props}
import encry.parser.NodeParser
import encry.settings.ParseSettings
import ExplorerApp._
import com.typesafe.scalalogging.StrictLogging

class ParsersController(settings: ParseSettings, dbActor: ActorRef) extends Actor with StrictLogging {

  override def preStart(): Unit = {
    settings.nodes.foreach(node =>
      system.actorOf(Props(new NodeParser(node, self, dbActor, settings)).withDispatcher("parser-dispatcher"),
        s"ParserFor${node.getHostName}")
    )
  }
  var currentListeningPeers: List[InetAddress] = List.empty[InetAddress]

  override def receive: Receive = {
//    case PeersList(peers) =>
//      val fp: List[InetAddress] = peers.diff(currentListeningPeers)
//      fp.foreach { peer =>
//        system.actorOf(Props(new NodeParser(new InetSocketAddress(peer, 9051), self, dbActor)),
//          s"ParserFor${peer.getHostName}")
//      }
//      currentListeningPeers = currentListeningPeers ++: fp
    case _ =>
  }
}
