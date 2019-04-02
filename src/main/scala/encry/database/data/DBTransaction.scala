package encry.database.data

import encry.blockchain.modifiers.Transaction
import org.encryfoundation.common.Algos
import org.encryfoundation.common.transaction.Proof
import org.encryfoundation.prismlang.core.wrapped.BoxedValue.{BoolValue, ByteCollectionValue, ByteValue, IntValue, Signature25519Value, StringValue}

case class DBTransaction(id: String,
                         fee: Long,
                         timestamp: Long,
                         defaultProofOpt: Option[String],
                         isCoinbase: Boolean,
                         blockId: String)

object DBTransaction {

  def apply(tx: Transaction,
            blockId: String): DBTransaction =
    new DBTransaction(
      tx.id,
      tx.fee,
      tx.timestamp,
      tx.defaultProofOpt.map{
        case Proof(IntValue(value), _) => s"IntValue - ${value.toString}"
        case Proof(ByteValue(value), _) => s"ByteValue - ${value.toString}"
        case Proof(BoolValue(value), _) => s"BoolValue - ${value.toString}"
        case Proof(StringValue(value), _) => s"StringValue - ${value.toString}"
        case Proof(ByteCollectionValue(value), _) => s"ByteCollectionValue - ${Algos.encode(value.toArray)}"
        case Proof(Signature25519Value(value), _) => s"Signature25519Value - ${Algos.encode(value.toArray)}"
      },
      tx.inputs.isEmpty,
      blockId)

  def apply(id: String,
            fee: Long,
            timestamp: Long,
            defaultProofOpt: Option[String],
            isCoinbase: Boolean,
            blockId: String): DBTransaction = new DBTransaction(id, fee, timestamp, defaultProofOpt, isCoinbase, blockId)
}
