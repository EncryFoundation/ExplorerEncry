package encry.blockchain.modifiers

import encry.blockchain.modifiers.directive.Directive
import io.circe.{Decoder, Encoder, HCursor}
import org.encryfoundation.common.transaction.{Input, Proof}

case class Transaction(id: String,
                       fee: Long,
                       timestamp: Long,
                       inputs: IndexedSeq[Input],
                       directives: IndexedSeq[Directive],
                       defaultProofOpt: Option[Proof])

object Transaction {

  implicit val jsonDecoder: Decoder[Transaction] = (c: HCursor) => {
    for {
      id              <- c.downField("id").as[String]
      fee             <- c.downField("fee").as[Long]
      timestamp       <- c.downField("timestamp").as[Long]
      inputs          <- c.downField("inputs").as[IndexedSeq[Input]]
      directives      <- c.downField("directives").as[IndexedSeq[Directive]]
      defaultProofOpt <- c.downField("defaultProofOpt").as[Option[Proof]]
    } yield Transaction(
      id,
      fee,
      timestamp,
      inputs,
      directives,
      defaultProofOpt
    )
  }
}
