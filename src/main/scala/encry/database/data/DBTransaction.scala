package encry.database.data

import encry.blockchain.modifiers.Transaction

case class DBTransaction(id: String,
                         fee: Long,
                         timestamp: Long,
                         defaultProofOpt: String,
                         isCoinbase: Boolean,
                         blockId: String)

object DBTransaction {

  def apply(tx: Transaction,
            blockId: String): DBTransaction =
    new DBTransaction(
      tx.id,
      tx.fee,
      tx.timestamp,
      tx.defaultProofOpt.map(_.toString).getOrElse(""),
      tx.inputs.isEmpty,
      blockId)

  def apply(id: String,
            fee: Long,
            timestamp: Long,
            defaultProofOpt: String,
            isCoinbase: Boolean,
            blockId: String): DBTransaction = new DBTransaction(id, fee, timestamp, defaultProofOpt, isCoinbase, blockId)
}
