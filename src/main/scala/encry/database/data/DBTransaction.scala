package encry.database.data

import encry.blockchain.modifiers.Transaction
import org.encryfoundation.common.modifiers.mempool.transaction.Proof
import org.encryfoundation.common.utils.Algos
import org.encryfoundation.prismlang.core.wrapped.BoxedValue._

case class DBTransaction(id: String,
                         fee: Long,
                         timestamp: Long,
                         defaultProofOpt: Option[String],
                         isCoinbase: Boolean,
                         blockId: String)

object DBTransaction {

  def apply(tx: Transaction,
            blockId: String): DBTransaction =
    DBTransaction(
      tx.id,
      tx.fee,
      tx.timestamp,
      tx.defaultProofOpt.map {
        case Proof(IntValue(value), _) => s"IntValue - ${value.toString}"
        case Proof(ByteValue(value), _) => s"ByteValue - ${value.toString}"
        case Proof(BoolValue(value), _) => s"BoolValue - ${value.toString}"
        case Proof(StringValue(value), _) => s"StringValue - ${value.toString}"
        case Proof(ByteCollectionValue(value), _) => s"ByteCollectionValue - ${Algos.encode(value.toArray)}"
        case Proof(Signature25519Value(value), _) => s"Signature25519Value - ${Algos.encode(value.toArray)}"
        case Proof(MultiSignatureValue(value), _) => s"MultiSignatureValue - ${Algos.encode(value.flatten.toArray)}"
      },
      tx.inputs.isEmpty,
      blockId)

 }
