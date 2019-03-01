package encry.blockchain.modifiers.directive

import encry.blockchain.modifiers.directive.Directive.DTypeId
import io.circe.{Decoder, Encoder, HCursor}
import io.circe.syntax._

case class AssetIssuingDirective(contractHash: String, amount: Long) extends Directive {

  override val typeId: DTypeId = AssetIssuingDirective.TypeId
}

object AssetIssuingDirective {

  val TypeId: DTypeId = 2.toByte

  implicit val jsonEncoder: Encoder[AssetIssuingDirective] = (d: AssetIssuingDirective) => Map(
    "typeId"       -> d.typeId.asJson,
    "contractHash" -> d.contractHash.asJson,
    "amount"       -> d.amount.asJson
  ).asJson

  implicit val jsonDecoder: Decoder[AssetIssuingDirective] = (c: HCursor) => {
    for {
      contractHash <- c.downField("contractHash").as[String]
      amount       <- c.downField("amount").as[Long]
    } yield AssetIssuingDirective(contractHash, amount)
  }
}
