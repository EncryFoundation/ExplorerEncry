package encry.blockchain.modifiers.boxes

import io.circe.{Decoder, HCursor}

case class DataBox(override val id: String,
                   override val proposition: String,
                   override val nonce: Long,
                   data: String) extends EncryBaseBox {

  override val typeId: Byte = DataBox.TypeId
}

object DataBox {

  val TypeId = 4.toByte

  implicit val jsonDecoder: Decoder[DataBox] = (c: HCursor) => {
    for {
      id          <- c.downField("id").as[String]
      proposition <- c.downField("proposition").as[String]
      nonce       <- c.downField("nonce").as[Long]
      data        <- c.downField("data").as[Array[Byte]]
    } yield DataBox(
      id,
      proposition,
      nonce,
      data.toString
    )
  }
}