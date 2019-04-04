package encry.database.data

import encry.blockchain.modifiers.DBDirectiveGeneralizedClass
import encry.blockchain.modifiers.boxes.DBBoxGeneralizedClass

case class DBOutput(id: String,
                    txId: String,
                    monetaryValue: Long,
                    coinId: String,
                    contractHash: String,
                    data: String,
                    isActive: Boolean,
                    minerAddress: String){
  override def toString: String = s"$id, $txId, $monetaryValue, $coinId, $contractHash, $data, $isActive, $minerAddress"
}

object DBOutput {

  def apply(directive: DBDirectiveGeneralizedClass,
            box: DBBoxGeneralizedClass,
            txId: String,
            isActive: Boolean): DBOutput =
    new DBOutput(box.id, txId, directive.amount, box.coinId, box.contractHash, box.data, isActive, directive.address)
}