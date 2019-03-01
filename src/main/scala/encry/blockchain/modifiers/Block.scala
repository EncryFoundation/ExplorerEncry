package encry.blockchain.modifiers

import io.circe.{Decoder, HCursor}

case class Block(header: Header, payload: Payload, adProofsOpt: Option[ADProofs])

object Block {

  implicit val jsonDecoder: Decoder[Block] = (c: HCursor) => {
    for {
      header  <- c.downField("header").as[Header]
      payload <- c.downField("payload").as[Payload]
    } yield Block(
      header,
      payload,
      None
    )
  }
}
