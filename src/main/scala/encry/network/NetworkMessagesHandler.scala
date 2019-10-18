package encry.network

import HeaderProto.HeaderProtoMessage
import PayloadProto.PayloadProtoMessage
import TransactionProto.TransactionProtoMessage
import akka.actor.{Actor, ActorSelection, Props}
import com.typesafe.scalalogging.StrictLogging
import org.encryfoundation.common.utils.Algos
import BasicMessagesRepo._
import org.encryfoundation.common.modifiers.history.{Block, Header, HeaderProtoSerializer, Payload, PayloadProtoSerializer}
import org.encryfoundation.common.modifiers.mempool.transaction.{Transaction, TransactionProtoSerializer}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class NetworkMessagesHandler(frontRemoteActor: ActorSelection) extends Actor with StrictLogging {

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
                payload.txs.foreach(tx => println(s"payload.tx: ${tx.encodedId}"))
                println(s"payload.end")
                frontRemoteActor ! payload
              }
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

  def props(frontRemoteActor: ActorSelection) = Props(new NetworkMessagesHandler(frontRemoteActor))
}