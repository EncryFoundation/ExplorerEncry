package encry.blockchain.modifiers.boxes

import io.circe.{Decoder, HCursor}

case class AssetBoxAPIAPI(override val id: String,
                          override val proposition: String,
                          override val nonce: Long,
                          amount: Long,
                          tokenIdOpt: Option[String] = None) extends EncryBaseBoxAPI {

  override val typeId: Byte = AssetBoxAPIAPI.TypeId
}

object AssetBoxAPIAPI {

  val TypeId: Byte = 1.toByte

  implicit val jsonDecoder: Decoder[AssetBoxAPIAPI] = (c: HCursor) => {
    for {
      id          <- c.downField("id").as[String]
      proposition <- c.downField("proposition").as[Proposition]
      nonce       <- c.downField("nonce").as[Long]
      amount      <- c.downField("value").as[Long]
      tokenIdOpt  <- c.downField("tokenId").as[Option[String]]
    } yield AssetBoxAPIAPI(
      id,
      proposition.contractHash,
      nonce,
      amount,
      tokenIdOpt
    )
  }
}
