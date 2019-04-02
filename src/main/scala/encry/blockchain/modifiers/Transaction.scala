package encry.blockchain.modifiers

import encry.blockchain.modifiers.boxes.EncryBaseBox
import io.circe.{Decoder, HCursor}
import org.encryfoundation.common.transaction.{Input, Proof}

case class Transaction(id: String,
                       fee: Long,
                       timestamp: Long,
                       inputs: IndexedSeq[Input],
                       outputs: IndexedSeq[EncryBaseBox],
                       defaultProofOpt: Option[Proof],
                       directive: IndexedSeq[Directive])

object Transaction {

  implicit val jsonDecoder: Decoder[Transaction] = (c: HCursor) => {
    for {
      id              <- c.downField("id").as[String]
      fee             <- c.downField("fee").as[Long]
      timestamp       <- c.downField("timestamp").as[Long]
      inputs          <- c.downField("inputs").as[IndexedSeq[Input]]
      outputs         <- c.downField("outputs").as[IndexedSeq[EncryBaseBox]]
      defaultProofOpt <- c.downField("defaultProofOpt").as[Option[Proof]]
      directives      <- c.downField("directives").as[IndexedSeq[Directive]]
    } yield Transaction(
      id,
      fee,
      timestamp,
      inputs,
      outputs,
      defaultProofOpt,
      directives
    )
  }
}
