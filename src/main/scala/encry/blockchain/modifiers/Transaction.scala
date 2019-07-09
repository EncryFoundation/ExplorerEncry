package encry.blockchain.modifiers

import encry.blockchain.modifiers.boxes.EncryBaseBoxAPI
import io.circe.{Decoder, HCursor}
import org.encryfoundation.common.modifiers.mempool.transaction.{Input, Proof}

case class Transaction(id: String = "",
                       fee: Long = 0L,
                       timestamp: Long = 0L,
                       inputs: IndexedSeq[Input] = IndexedSeq.empty[Input],
                       outputs: IndexedSeq[EncryBaseBoxAPI] = IndexedSeq.empty[EncryBaseBoxAPI],
                       defaultProofOpt: Option[Proof] = None,
                       directive: IndexedSeq[Directive] = IndexedSeq.empty[Directive])

object Transaction {

  implicit val jsonDecoder: Decoder[Transaction] = (c: HCursor) => {
    for {
      id              <- c.downField("id").as[String]
      fee             <- c.downField("fee").as[Long]
      timestamp       <- c.downField("timestamp").as[Long]
      inputs          <- c.downField("inputs").as[IndexedSeq[Input]]
      outputs         <- c.downField("outputs").as[IndexedSeq[EncryBaseBoxAPI]]
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