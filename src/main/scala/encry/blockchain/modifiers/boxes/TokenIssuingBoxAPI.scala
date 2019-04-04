package encry.blockchain.modifiers.boxes

import io.circe.{Decoder, HCursor}

case class TokenIssuingBoxAPI(override val id: String,
                              override val proposition: String,
                              override val nonce: Long,
                              amount: Long,
                              tokenId: String) extends EncryBaseBoxAPI {

  override val typeId: Byte = 2.toByte
}

object TokenIssuingBoxAPI {

  val TypeId = 2.toByte

  implicit val jsonDecoder: Decoder[TokenIssuingBoxAPI] = (c: HCursor) => {
    for {
      id          <- c.downField("id").as[String]
      proposition <- c.downField("proposition").as[String]
      nonce       <- c.downField("nonce").as[Long]
      amount      <- c.downField("amount").as[Long]
      tokenId     <- c.downField("tokenId").as[String]
    } yield TokenIssuingBoxAPI(
      id,
      proposition,
      nonce,
      amount,
      tokenId
    )
  }
}
