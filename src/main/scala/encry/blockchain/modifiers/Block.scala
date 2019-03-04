package encry.blockchain.modifiers

import encry.database.data.{DBInput, DBOutput}
import io.circe.{Decoder, HCursor}

case class Block(header: Header, payload: Payload, adProofsOpt: Option[ADProofs]) {

  def getDBInputs: List[DBInput] = payload.txs.flatMap(tx => tx.inputs.map(input => DBInput(input, tx.id)))

  def getDbOutputs: List[DBOutput] = payload.txs.flatMap(tx => tx.outputs.map(output => DBOutput(output, tx.id)))
}

object Block {

  val empty: Block = Block(Header.empty, Payload.empty, None)

  implicit val decoder: Decoder[Block] = (c: HCursor) => {
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
