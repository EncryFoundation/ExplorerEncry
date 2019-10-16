package encry.network

import HeaderProto.HeaderProtoMessage
import PayloadProto.PayloadProtoMessage
import TransactionProto.TransactionProtoMessage
import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.StrictLogging
import org.encryfoundation.common.utils.Algos
import BasicMessagesRepo._
import NetworkMessagesHandler._
import org.encryfoundation.common.modifiers.history.{Header, HeaderProtoSerializer, Payload, PayloadProtoSerializer}
import org.encryfoundation.common.modifiers.mempool.transaction.{Transaction, TransactionProtoSerializer}

//TODO: replace everywhere encry.net to org.encryfoundation.common

class NetworkMessagesHandler() extends Actor with StrictLogging {

  override def receive: Receive = {

    case MessageFromNetwork(message, peerOpt) => message match {

      case InvNetworkMessage((modifierTypeId, modifierIds)) =>
        logger.debug(s"Got modifiers: $modifierTypeId (${modifierIds.map(Algos.encode).mkString(",")})")
        println(s"Got modifier: ${peerOpt.get}")
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
              tx.foreach(self ! ReceiveTransaction(_))

            case Header.modifierTypeId =>
              val header = HeaderProtoSerializer.fromProto(HeaderProtoMessage.parseFrom(bytes))
              header.foreach(self ! ReceiveHeader(_))

            case Payload.modifierTypeId =>
              val payload = PayloadProtoSerializer.fromProto(PayloadProtoMessage.parseFrom(bytes))
              payload.foreach(self ! ReceivePayload(_))
          }
        }

      case _ =>
    }

    case ReceiveTransaction(tx: Transaction) => println(s"tx: $tx")

    case ReceiveHeader(header: Header) => println(s"header: $header")

    case ReceivePayload(payload: Payload) => println(s"payload: txs ${payload.txs.size}")


    case _ =>
  }
}

object NetworkMessagesHandler {

  case class ReceiveTransaction(tx: Transaction)

  case class ReceiveHeader(header: Header)

  case class ReceivePayload(payload: Payload)

  def props() = Props(new NetworkMessagesHandler())
}