package encry.network

import HeaderProto.HeaderProtoMessage
import PayloadProto.PayloadProtoMessage
import TransactionProto.TransactionProtoMessage
import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.StrictLogging
import org.encryfoundation.common.utils.Algos
import BasicMessagesRepo._
import ModifierMessages._
import org.encryfoundation.common.modifiers.history.{Header, HeaderProtoSerializer, Payload, PayloadProtoSerializer}
import org.encryfoundation.common.modifiers.mempool.transaction.{Transaction, TransactionProtoSerializer}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

//TODO: replace everywhere encry.net to org.encryfoundation.common

class NetworkMessagesHandler(frontHost: String, frontPort: Int) extends Actor with StrictLogging {

  val frontRemoteActor = context.actorSelection(s"akka.tcp://application@$frontHost:$frontPort/user/receiver")

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
              tx.foreach { tx =>
                println(s"tx: ${tx.encodedId}")
                frontRemoteActor ! tx
              }

            case Header.modifierTypeId =>
              val header = HeaderProtoSerializer.fromProto(HeaderProtoMessage.parseFrom(bytes))
              header.foreach { header =>
                println(s"header: ${header.encodedId}")
                frontRemoteActor ! header
              }

            case Payload.modifierTypeId =>
              val payload = PayloadProtoSerializer.fromProto(PayloadProtoMessage.parseFrom(bytes))
              payload.foreach { payload =>
                println(s"payload: ${payload.encodedId} txs ${payload.txs.size}")
                frontRemoteActor ! payload
              }
          }
        }

      case _ =>
    }
    case _ =>
  }
}

object ModifierMessages {
  case class ModifierTx(tx: Transaction)
  case class ModifierHeader(header: Header)
  case class ModifierPayload(payload: Payload)
}

object NetworkMessagesHandler {
  def props(frontHost: String, frontPort: Int) = Props(new NetworkMessagesHandler(frontHost, frontPort))
}