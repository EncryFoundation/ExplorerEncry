package encry.blockchain.modifiers

import io.circe.{Decoder, HCursor}

case class Payload(headerId: String,
                   txs: Seq[Transaction])

object Payload {

  implicit val jsonDecoder: Decoder[Payload] = (c: HCursor) => {
    for {
      headerId     <- c.downField("headerId").as[String]
      transactions <- c.downField("transactions").as[Seq[Transaction]]
    } yield Payload(
      headerId,
      transactions
    )
  }
}
