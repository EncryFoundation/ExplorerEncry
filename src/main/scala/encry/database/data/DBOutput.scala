package encry.database.data

import encry.blockchain.modifiers.DBDirectiveGeneralizedClass
import encry.blockchain.modifiers.boxes.{AssetBox, AssetBoxAPIAPI, DBBoxGeneralizedClass, DataBox, DataBoxAPI, EncryBaseBox, EncryBaseBoxAPI, TokenIssuingBox, TokenIssuingBoxAPI}
import encry.settings.Constants
import org.encryfoundation.common.Algos

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

  def apply(directive: DBDirectiveGeneralizedClass, box: DBBoxGeneralizedClass, txId: String, isActive: Boolean): DBOutput = new DBOutput(box.id, txId, directive.amount, box.coinId, box.contractHash, box.data, isActive, directive.address)

  def apply(id: String,
            txId: String,
            monetaryValue: Long,
            coinId: String,
            contractHash: String,
            data: String,
            isActive: Boolean,
            minerAddress: String): DBOutput = new DBOutput(id, txId, monetaryValue, coinId, contractHash, data, isActive, minerAddress)

  def apply(output: EncryBaseBox, txId: String): DBOutput = output match {
    case assetBox: AssetBox =>
      new DBOutput(
        Algos.encode(assetBox.id),
        txId,
        assetBox.amount,
        Algos.encode(assetBox.tokenIdOpt.getOrElse(Constants.IntrinsicTokenId)),
        Algos.encode(assetBox.proposition.contractHash),
        "",
        true,
        ""
      )
    case dataBox: DataBox =>
      new DBOutput(
        Algos.encode(dataBox.id),
        txId,
        -1,
        "",
        Algos.encode(dataBox.proposition.contractHash),
        Algos.encode(dataBox.data),
        true,
        ""
      )
    case tokenIssuingBox: TokenIssuingBox =>
      new DBOutput(
        Algos.encode(tokenIssuingBox.id),
        txId,
        tokenIssuingBox.amount,
        Algos.encode(tokenIssuingBox.tokenId),
        Algos.encode(tokenIssuingBox.proposition.contractHash),
        "",
        true,
        ""
      )
  }
}
