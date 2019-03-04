package encry.database.data

import encry.blockchain.modifiers.boxes.{AssetBox, DataBox, EncryBaseBox, TokenIssuingBox}
import encry.settings.Constants

case class DBOutput(id: String,
                    txId: String,
                    monetaryValue: Long,
                    coinId: String,
                    contractHash: String,
                    data: String,
                    isActive: Boolean)

object DBOutput {

  def apply(id: String,
            txId: String,
            monetaryValue: Long,
            coinId: String,
            contractHash: String,
            data: String,
            isActive: Boolean): DBOutput = new DBOutput(id, txId, monetaryValue, coinId, contractHash, data, isActive)

  def apply(output: EncryBaseBox, txId: String): DBOutput = output match {
    case assetBox: AssetBox =>
      new DBOutput(
        assetBox.id,
        txId,
        assetBox.amount,
        assetBox.tokenIdOpt.getOrElse(Constants.IntrinsicTokenId),
        assetBox.proposition,
        "",
        true
      )
    case dataBox: DataBox =>
      new DBOutput(
        dataBox.id,
        txId,
        -1,
        "",
        dataBox.proposition,
        dataBox.data,
        true
      )
    case tokenIssuingBox: TokenIssuingBox =>
      new DBOutput(
        tokenIssuingBox.id,
        txId,
        tokenIssuingBox.amount,
        tokenIssuingBox.tokenId,
        tokenIssuingBox.proposition,
        "",
        true
      )
  }
}
