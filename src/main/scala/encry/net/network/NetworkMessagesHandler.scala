package encry.net.network

import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.StrictLogging
import org.encryfoundation.common.utils.Algos
import encry.net.network.BasicMessagesRepo._
import encry.net.network.NetworkMessagesHandler.{BroadcastInvForTx, TransactionForCommit}
import encry.net.modifiers.{Transaction, TransactionProtoSerializer}
import encry.net.utils.CoreTaggedTypes.{ModifierId, ModifierTypeId}
import encry.net.utils.CoreTaggedTypes
import supertagged.@@

class NetworkMessagesHandler() extends Actor with StrictLogging {

  var localGeneratedTransactions: Seq[Transaction] = Seq.empty

  override def receive: Receive = {
    case TransactionForCommit(transaction) =>
      localGeneratedTransactions :+= transaction
      context.parent ! BroadcastInvForTx(transaction)

    case MessageFromNetwork(message, _) => message match {
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
  case class TransactionForCommit(tx: Transaction)
  case class BroadcastInvForTx(tx: Transaction)

  def props() = Props(new NetworkMessagesHandler())
}