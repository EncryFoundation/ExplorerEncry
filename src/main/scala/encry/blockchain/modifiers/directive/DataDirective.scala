package encry.blockchain.modifiers.directive

import encry.blockchain.modifiers.directive.Directive.DTypeId
import io.circe.{Decoder, Encoder, HCursor}
import org.encryfoundation.common.Algos
import io.circe.syntax._

case class DataDirective(contractHash: String, data: String) extends Directive {

  override val typeId: DTypeId = DataDirective.TypeId
}

object DataDirective {

  val TypeId: DTypeId = 5.toByte

  implicit val jsonEncoder: Encoder[DataDirective] = (d: DataDirective) => Map(
    "typeId"       -> d.typeId.asJson,
    "contractHash" -> d.contractHash.asJson,
    "data"         -> d.data.asJson
  ).asJson

  implicit val jsonDecoder: Decoder[DataDirective] = (c: HCursor) => {
    for {
      contractHash <- c.downField("contractHash").as[String]
      dataEnc      <- c.downField("data").as[String]
    } yield DataDirective(contractHash, dataEnc)
  }
}