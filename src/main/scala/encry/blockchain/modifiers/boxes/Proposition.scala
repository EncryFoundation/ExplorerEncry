package encry.blockchain.modifiers.boxes

import io.circe.{Decoder, HCursor}

case class Proposition(contractHash: String)

object Proposition {
  implicit val jsonDecoder: Decoder[Proposition] = (c: HCursor) =>
    for { contractHash <- c.downField("contractHash").as[String] }
      yield Proposition(contractHash)
}
