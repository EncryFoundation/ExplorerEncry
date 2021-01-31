package encry.network

import TransactionProto.TransactionProtoMessage
import akka.actor.{Actor, ActorRef, Props}
import com.typesafe.scalalogging.StrictLogging
import encry.network.NetworkMessagesHandler.MessageFromNetwork
import encry.network.PeerHandler.ConnectedPeer
import org.encryfoundation.common.modifiers.mempool.transaction.{Transaction, TransactionProtoSerializer}
import org.encryfoundation.common.network.BasicMessagesRepo._
import org.encryfoundation.common.utils.Algos

class NetworkMessagesHandler(networkServer: ActorRef) extends Actor with StrictLogging {

  override def receive: Receive = {

    case MessageFromNetwork(message, peerOpt) => message match {

      case InvNetworkMessage((modifierTypeId, modifierIds)) =>
        peerOpt.foreach { peer =>
          if (Transaction.modifierTypeId == modifierTypeId) {
            logger.debug(s"Request modifier: $modifierTypeId ${modifierIds.map(Algos.encode).mkString(",")}")
            peer.handlerRef ! RequestModifiersNetworkMessage((modifierTypeId, modifierIds))
          }
        }

      case ModifiersNetworkMessage((modifierTypeId, modifierMap)) =>
        logger.debug(s"Response modifiers: $modifierTypeId size ${modifierMap.size}")
        modifierMap.foreach { case (modifierId, bytes) =>
          modifierTypeId match {
            case Transaction.modifierTypeId =>
              val tx = TransactionProtoSerializer.fromProto(TransactionProtoMessage.parseFrom(bytes))
              tx.foreach(networkServer ! _)

            case _ =>
          }
        }

      case _ =>
    }
    case _ =>
  }
}

object NetworkMessagesHandler {

  case class Transaction(tx: Transaction)
  case class Block(block: Block)

  /**
   * @param message - message, received from network
   * @param source - sender of received message
   *
   *               This case class transfers network message from PeerConnectionHandler actor to the NetworkController.
   *               Main duty is to transfer message from network with sender of it message to the NetworkController as an end point.
   */
  case class MessageFromNetwork(message: NetworkMessage, source: Option[ConnectedPeer])

  def props(networkServer: ActorRef) = Props(new NetworkMessagesHandler(networkServer))
}