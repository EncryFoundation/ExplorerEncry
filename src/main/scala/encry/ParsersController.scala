package encry

import java.net.{InetAddress, InetSocketAddress}
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import encry.parser.{NodeParser, SimpleNodeParser}
import encry.settings.{BlackListSettings, ParseSettings}
import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import com.typesafe.scalalogging.StrictLogging
import encry.ParsersController.{BadPeer, RemoveBadPeer}
import encry.parser.NodeParser.PeersFromApi

import scala.concurrent.duration._

class ParsersController(settings: ParseSettings,
                        blackListSettings: BlackListSettings,
                        dbActor: ActorRef) extends Actor with StrictLogging {

  var peerReconnects: Map[InetAddress, Int] = Map.empty[InetAddress, Int]

  var blackList: Seq[(InetAddress, Long)] = Seq.empty

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(5, 10.seconds) {
    //todo can stop actor and remove peer from listening collection (can use ref as an indicator about peer address)
    case msg => logger.info(s"Stopping child actor $sender cause of: ${msg.getMessage}.")
      Stop
  }

  override def preStart(): Unit = {
    context.system.scheduler.scheduleOnce(blackListSettings.cleanupTime, self, RemoveBadPeer)
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
      val newPeers: Set[InetAddress] = peers.diff(knownPeers) -- blackList.map(_._1)
      newPeers.foreach { peer =>
        val newAddress: InetSocketAddress = new InetSocketAddress(peer, 9051)
        logger.info(s"Creating SimpleNode parser for: $newAddress...")
        context.actorOf(SimpleNodeParser.props(newAddress, self, dbActor, settings), name = s"SNP${peer.getHostName}")
      }
      val resultedPeers: Set[InetAddress] = knownPeers ++ newPeers
      context.become(mainBehaviour(resultedPeers))

    case BadPeer(peer) =>
      val currentNumberOfReconnects: Int = peerReconnects.getOrElse(peer, 0)
      if (currentNumberOfReconnects > 3) {
        blackList = blackList :+ (peer -> System.currentTimeMillis())
      }
      else {
        peerReconnects = peerReconnects.updated(peer, currentNumberOfReconnects +1)
        context.become(mainBehaviour(knownPeers - peer))
      }

    case RemoveBadPeer =>
      val peersForRemove: Seq[(InetAddress, Long)] = blackList
        .filter { case (_, banTime) =>
          System.currentTimeMillis() - banTime >= blackListSettings.banTime.toMillis
        }
      blackList = blackList.diff(peersForRemove)
      peerReconnects --= peersForRemove.map(_._1)
      context.system.scheduler.scheduleOnce(blackListSettings.cleanupTime, self, RemoveBadPeer)
      context.become(mainBehaviour(knownPeers -- peersForRemove.map(_._1)))

    case msg => logger.info(s"Got strange message on ParserController: $msg.")
  }

}

object ParsersController {

  final case class BadPeer(peer: InetAddress) extends AnyVal

  case object RemoveBadPeer

}