package encry.blockchain.modifiers

import io.circe.{Decoder, HCursor}

case class Payload(headerId: String,
                   txs: List[Transaction])

object Payload {

  val empty: Payload = Payload("", List.empty)

  implicit val jsonDecoder: Decoder[Payload] = (c: HCursor) => {
    for {
      headerId     <- c.downField("headerId").as[String]
      transactions <- c.downField("transactions").as[List[Transaction]]
    } yield Payload(
      headerId,
      transactions
    )
  }
}
