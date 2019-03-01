package encry.blockchain.modifiers

import io.circe.{Decoder, HCursor}

case class ADProofs(headerId: String, proofBytes: String)

object ADProofs {
  implicit val jsonDecoder: Decoder[ADProofs] = (c: HCursor) => {
    for {
      headerIdAd <- c.downField("headerId").as[String]
      proofBytes <- c.downField("proofBytes").as[String]
    } yield ADProofs(
      headerIdAd,
      proofBytes,
    )
  }
}