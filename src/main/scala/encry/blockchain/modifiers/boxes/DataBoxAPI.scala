package encry.blockchain.modifiers.boxes

import io.circe.{Decoder, HCursor}

case class DataBoxAPI(override val id: String,
                      override val proposition: String,
                      override val nonce: Long,
                      data: String) extends EncryBaseBoxAPI {

  override val typeId: Byte = DataBoxAPI.TypeId
}

object DataBoxAPI {

  val TypeId: Byte = 4.toByte

  implicit val jsonDecoder: Decoder[DataBoxAPI] = (c: HCursor) => {
    for {
      id          <- c.downField("id").as[String]
      proposition <- c.downField("proposition").as[Proposition]
      nonce       <- c.downField("nonce").as[Long]
      data        <- c.downField("data").as[String]
    } yield DataBoxAPI(
      id,
      proposition.contractHash,
      nonce,
      data
    )
  }
}