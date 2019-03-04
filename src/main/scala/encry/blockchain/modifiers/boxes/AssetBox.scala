package encry.blockchain.modifiers.boxes

import io.circe.{Decoder, HCursor}

case class AssetBox(override val id: String,
                    override val proposition: String,
                    override val nonce: Long,
                    amount: Long,
                    tokenIdOpt: Option[String] = None) extends EncryBaseBox {

  override val typeId: Byte = AssetBox.TypeId
}

object AssetBox {

  val TypeId: Byte = 1.toByte

  implicit val jsonDecoder: Decoder[AssetBox] = (c: HCursor) => {
    for {
      id          <- c.downField("id").as[String]
      proposition <- c.downField("proposition").as[Proposition]
      nonce       <- c.downField("nonce").as[Long]
      amount      <- c.downField("value").as[Long]
      tokenIdOpt  <- c.downField("tokenId").as[Option[String]]
    } yield AssetBox(
      id,
      proposition.contractHash,
      nonce,
      amount,
      tokenIdOpt
    )
  }
}
