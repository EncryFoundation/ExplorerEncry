package encry.database.data

import org.encryfoundation.common.modifiers.mempool.transaction.Input
import org.encryfoundation.common.utils.Algos

case class DBInput(bxId: String, txId: String, contract: String, proofs: String)

object DBInput {

  //todo: remove input.contract.toString
  def apply(input: Input, txId: String): DBInput =
    new DBInput(
      Algos.encode(input.boxId),
      txId,
     input.contract match {
       case i if i.isRight => s"RegularContract - ${i.map(_.contract.script).toString}"
       case i  => s"CompiledContract - ${i.left.map(_.script).toString}"
     },
      input.proofs.map(_.toString).mkString(",")
    )

  def apply(bxId: String, txId: String, contract: String, proofs: String): DBInput = new DBInput(bxId, txId, contract, proofs)
}

