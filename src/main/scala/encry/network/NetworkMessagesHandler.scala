package encry.network

import TransactionProto.TransactionProtoMessage
import akka.actor.{Actor, ActorRef, Props}
import com.typesafe.scalalogging.StrictLogging
import encry.network.BasicMessagesRepo._
import org.encryfoundation.common.modifiers.history.{Header, Payload}
import org.encryfoundation.common.modifiers.mempool.transaction.{Transaction, TransactionProtoSerializer}
import org.encryfoundation.common.utils.Algos

class NetworkMessagesHandler(networkServer: ActorRef) extends Actor with StrictLogging {

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
              tx.foreach(networkServer ! _)

//            case Header.modifierTypeId =>
//              val header = HeaderProtoSerializer.fromProto(HeaderProtoMessage.parseFrom(bytes))
//              header.foreach { header =>
//                println(s"header: ${header.encodedId}")
//                networkServer ! header
//              }
//
//            case Payload.modifierTypeId =>
//              val payload = PayloadProtoSerializer.fromProto(PayloadProtoMessage.parseFrom(bytes))
//
//              payload.foreach { payload =>
//                println(s"payload: ${payload.encodedId} txs ${payload.txs.size}")
//                payload.txs.foreach(tx => println(s"payload.tx: ${tx.encodedId}"))
//                println(s"payload.end")
//                networkServer ! payload
//              }
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

  def props(networkServer: ActorRef) = Props(new NetworkMessagesHandler(networkServer))
}