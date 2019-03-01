package encry.blockchain.modifiers.directive

import encry.blockchain.modifiers.directive.Directive.DTypeId
import io.circe.{Decoder, Encoder, HCursor}
import io.circe.syntax._

case class ScriptedAssetDirective(contractHash: String,
                                  amount: Long,
                                  tokenIdOpt: Option[String] = None) extends Directive {
  override val typeId: DTypeId = ScriptedAssetDirective.TypeId
}

object ScriptedAssetDirective {

  val TypeId: DTypeId = 3.toByte

  implicit val jsonEncoder: Encoder[ScriptedAssetDirective] = (d: ScriptedAssetDirective) => Map(
    "typeId"       -> d.typeId.asJson,
    "contractHash" -> d.contractHash.asJson,
    "amount"       -> d.amount.asJson,
    "tokenId"      -> d.tokenIdOpt.asJson
  ).asJson

  implicit val jsonDecoder: Decoder[ScriptedAssetDirective] = (c: HCursor) => for {
    contractHash <- c.downField("contractHash").as[String]
    amount       <- c.downField("amount").as[Long]
    tokenIdOpt   <- c.downField("tokenId").as[Option[String]]
  } yield ScriptedAssetDirective(
    contractHash,
    amount,
    tokenIdOpt
  )
}
