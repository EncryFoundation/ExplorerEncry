package encry.net.network

import HeaderProto.HeaderProtoMessage
import PayloadProto.PayloadProtoMessage
import TransactionProto.TransactionProtoMessage
import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.StrictLogging
import org.encryfoundation.common.utils.Algos
import encry.net.network.BasicMessagesRepo._
import encry.net.network.NetworkMessagesHandler.{BroadcastInvForTx, ReceiveHeader, ReceivePayload, ReceiveTransaction, TransactionForCommit}
import encry.net.modifiers.{Transaction, TransactionProtoSerializer}
import encry.net.utils.CoreTaggedTypes.{ModifierId, ModifierTypeId}
import encry.net.utils.CoreTaggedTypes
import org.encryfoundation.common.modifiers.history.{Header, HeaderProtoSerializer, Payload, PayloadProtoSerializer}
import supertagged.@@

//TODO: replace everywhere encry.net to org.encryfoundation.common

class NetworkMessagesHandler() extends Actor with StrictLogging {

  var localGeneratedTransactions: Seq[Transaction] = Seq.empty

  override def receive: Receive = {
    case TransactionForCommit(transaction) =>
      localGeneratedTransactions :+= transaction
      context.parent ! BroadcastInvForTx(transaction)

    case MessageFromNetwork(message, peerOpt) => message match {

      case InvNetworkMessage((modifierTypeId, modifierIds)) =>
        logger.debug(s"Got modifiers: $modifierTypeId (${modifierIds.map(Algos.encode).mkString(",")})")
        peerOpt.foreach { peer =>
          if (List(Transaction.modifierTypeId, Header.modifierTypeId, Payload.modifierTypeId).contains(modifierTypeId)) {
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
              tx.foreach(target ! ReceiveTransaction(_))

            case Header.modifierTypeId =>
              val header = HeaderProtoSerializer.fromProto(HeaderProtoMessage.parseFrom(bytes))
              header.foreach(target ! ReceiveHeader(_))

            case Payload.modifierTypeId =>
              val payload = PayloadProtoSerializer.fromProto(PayloadProtoMessage.parseFrom(bytes))
              payload.foreach(target ! ReceivePayload(_))
          }
        }

      case RequestModifiersNetworkMessage(invData) if invData._1 == Transaction.modifierTypeId =>
        logger.debug(s"Got request modifiers on NMH")
        val tmpInv: Seq[String] = invData._2.map(Algos.encode)
        val transactions: Seq[Transaction] = localGeneratedTransactions.filter(tx => tmpInv.contains(Algos.encode(tx.id)))
        val forSend: Map[Array[Byte] @@ CoreTaggedTypes.ModifierId.Tag, Array[Byte]] = transactions.map { tx =>
          ModifierId @@ tx.id -> TransactionProtoSerializer.toProto(tx).toByteArray
        }.toMap
        sender() ! ModifiersNetworkMessage(ModifierTypeId @@ Transaction.modifierTypeId -> forSend)
        val tmpTxs = transactions.map(tx => Algos.encode(tx.id))
        localGeneratedTransactions = localGeneratedTransactions.filter(tx =>
          !tmpTxs.contains(Algos.encode(tx.id))
        )
        logger.debug(s"Sent modifiers to node.")
      case _ =>
    }
    case _ =>
  }
}

object NetworkMessagesHandler {

  case class ReceiveTransaction(tx: Transaction)

  case class ReceiveHeader(header: Header)

  case class ReceivePayload(payload: Payload)

  case class TransactionForCommit(tx: Transaction)

  case class BroadcastInvForTx(tx: Transaction)

  def props() = Props(new NetworkMessagesHandler())
}