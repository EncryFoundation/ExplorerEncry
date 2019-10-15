package encry.net.network

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.Tcp.SO.KeepAlive
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import com.typesafe.scalalogging.StrictLogging
import encry.net.network.BasicMessagesRepo.{InvNetworkMessage, Outgoing}
import encry.net.network.NetworkMessagesHandler.{BroadcastInvForTx, TransactionForCommit}
import encry.net.network.NetworkServer.{CheckConnection, ConnectionSetupSuccessfully}
import encry.net.network.PeerHandler._
import encry.net.modifiers.Transaction
import encry.net.utils.CoreTaggedTypes.{ModifierId, ModifierTypeId}
import encry.net.utils.Mnemonic.createPrivKey
import encry.net.utils.NetworkTimeProvider
import encry.settings.NetworkSettings

import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor

class NetworkServer(settings: NetworkSettings,
                    timeProvider: NetworkTimeProvider) extends Actor with StrictLogging {

  implicit val system: ActorSystem = context.system
  implicit val ec: ExecutionContextExecutor = context.dispatcher

  var isConnected = false

  val messagesHandler: ActorRef = context.actorOf(NetworkMessagesHandler.props())

  var tmpConnectionHandler: Option[ActorRef] = None

  val selfPeer: InetSocketAddress =
    new InetSocketAddress(settings.bindAddressHost, settings.bindAddressPort)

  val connectingPeer: InetSocketAddress =
    new InetSocketAddress(settings.peerForConnectionHost, settings.peerForConnectionPort)

  IO(Tcp) ! Bind(self, selfPeer)

  override def receive: Receive = {
    case Bound(localAddress) =>
      logger.info(s"Local app was successfully bound to $localAddress!")
      context.system.scheduler.schedule(5.seconds, 30.seconds, self, CheckConnection)

    case CommandFailed(add: Bind) =>
      logger.info(s"Failed to bind to ${add.localAddress}.")
      context.stop(self)

    case Connected(remote, _) if !isConnected && remote.getAddress == connectingPeer.getAddress =>
      val handler: ActorRef = context.actorOf(
        PeerHandler.props(remote, sender(), settings, timeProvider, Outgoing, messagesHandler)
      )
      logger.info(s"Successfully connected to $remote. Creating handler: $handler.")
      isConnected = true
      tmpConnectionHandler = Some(handler)
      sender ! Register(handler)
      sender ! ResumeReading

    case Connected(remote, _) => logger.info(s"Remote: $remote try to connect but isConnected: $isConnected.")

    case CommandFailed(c: Connect) =>
      isConnected = false
      tmpConnectionHandler = None
      logger.info(s"Failed to connect to: ${c.remoteAddress}")

    case CheckConnection if !isConnected =>
      IO(Tcp) ! Connect(connectingPeer, options = KeepAlive(true) :: Nil, timeout = Some(5.seconds))
      logger.info(s"Trying to connect to $connectingPeer.")

    case CheckConnection =>
      logger.info(s"Triggered CheckConnection. Current connection is: $isConnected")

    case RemovePeerFromConnectionList(peer) =>
      isConnected = false
      tmpConnectionHandler = None
      logger.info(s"Disconnected from $peer.")

    case BroadcastInvForTx(tx) =>
      val inv: BasicMessagesRepo.NetworkMessage =
        InvNetworkMessage(ModifierTypeId @@ Transaction.modifierTypeId -> Seq(ModifierId @@ tx.id))
      tmpConnectionHandler.foreach(_ ! inv)
      logger.debug(s"Send inv message to remote.")

    case ConnectionSetupSuccessfully =>
      //logger.info(s"Created generator actor for ${peer.explorerHost}:${peer.explorerPort}.")

    case msg@TransactionForCommit(_) => messagesHandler ! msg

    case msg => logger.info(s"Got strange message on NetworkServer: $msg.")
  }
}

object NetworkServer {
  case object CheckConnection
  case object ConnectionSetupSuccessfully

  def props(settings: NetworkSettings, timeProvider: NetworkTimeProvider): Props =
    Props(new NetworkServer(settings, timeProvider))
}