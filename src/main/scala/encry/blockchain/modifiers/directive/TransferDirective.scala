package encry.blockchain.modifiers.directive

import encry.blockchain.modifiers.directive.Directive.DTypeId
import io.circe.{Decoder, Encoder, HCursor}
import org.encryfoundation.common.Algos
import org.encryfoundation.common.utils.TaggedTypes.ADKey
import io.circe.syntax._

case class TransferDirective(address: String,
                             amount: Long,
                             tokenIdOpt: Option[String] = None) extends Directive {

  override val typeId: DTypeId = TransferDirective.TypeId
}

object TransferDirective {

  val TypeId: DTypeId = 1.toByte

  implicit val jsonEncoder: Encoder[TransferDirective] = (d: TransferDirective) => Map(
    "typeId"  -> d.typeId.asJson,
    "address" -> d.address.toString.asJson,
    "amount"  -> d.amount.asJson,
    "tokenId" -> d.tokenIdOpt.asJson
  ).asJson

  implicit val jsonDecoder: Decoder[TransferDirective] = (c: HCursor) => {
    for {
      address    <- c.downField("address").as[String]
      amount     <- c.downField("amount").as[Long]
      tokenIdOpt <- c.downField("tokenId").as[Option[String]]
    } yield TransferDirective(
      address,
      amount,
      tokenIdOpt
    )
  }
}
