package encry

import java.net.{InetAddress, InetSocketAddress}
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import encry.parser.{NodeParser, SimpleNodeParser}
import encry.settings.ParseSettings
import akka.actor.SupervisorStrategy.Resume
import com.typesafe.scalalogging.StrictLogging
import encry.parser.NodeParser.PeersFromApi
import encry.parser.SimpleNodeParser.PeerForRemove
import scala.concurrent.duration._

class ParsersController(settings: ParseSettings, dbActor: ActorRef) extends Actor with StrictLogging {

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(5, 10.seconds) {
    //todo can stop actor and remove peer from listening collection (can use ref as an indicator about peer address)
    case _ => Resume
  }

  override def preStart(): Unit = {
    logger.info(s"Starting Parsing controller. Try to create listeners for: ${settings.nodes.mkString(",")}")
    settings.nodes.foreach(node =>
      context.actorOf(Props(new NodeParser(node, self, dbActor, settings)).withDispatcher("parser-dispatcher"))
    )
    val initialPeers: Set[InetAddress] = settings.nodes.map(_.getAddress).toSet
    logger.info(s"Initial peers are: ${initialPeers.mkString(",")}. Starting main behaviour...")
    context.become(mainBehaviour(initialPeers))
  }

  override def receive: Receive = mainBehaviour(Set.empty[InetAddress])

  def mainBehaviour(knownPeers: Set[InetAddress]): Receive = {
    case PeersFromApi(peers) =>
      val newPeers: Set[InetAddress] = peers.diff(knownPeers)
      logger.info(s"Got new peers on ParserController. Current knownPeers are: ${knownPeers.mkString(",")}, " +
        s"received peers are: ${peers.mkString(",")}, new unique peers are: ${newPeers.mkString(",")}")
      newPeers.foreach { peer =>
        val newAddress: InetSocketAddress = new InetSocketAddress(peer, 9051)
        logger.info(s"Creating SimpleNode parser for: $newAddress...")
        context.actorOf(SimpleNodeParser.props(newAddress, self, dbActor, settings), name = s"SNP${peer.getHostName}")
      }
      val resultedPeers: Set[InetAddress] = knownPeers ++ newPeers
      logger.info(s"Resulted peers collection is: ${resultedPeers.mkString(",")}.")
      context.become(mainBehaviour(resultedPeers))

    case PeerForRemove(peer) =>
      val updatedPeers: Set[InetAddress] = knownPeers - peer
      logger.info(s"Got peer for removing: $peer. Updated collection is: ${updatedPeers.mkString(",")}.")
      context.become(mainBehaviour(updatedPeers))

    case msg => logger.info(s"Got strange message on ParserController: $msg.")
  }
}