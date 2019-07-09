package encry.blockchain.modifiers

import encry.blockchain.modifiers.boxes.EncryBaseBox
import encry.database.data.{DBInput, DBOutput}
import io.circe.{Decoder, HCursor}
import org.encryfoundation.common.utils.Algos
import scorex.crypto.hash.Digest32

case class Block(header: Header, payload: Payload) {

  def getDBInputs: List[DBInput] = payload.txs.flatMap(tx => tx.inputs.map(input => DBInput(input, tx.id)))

  def getDbOutputs: List[DBOutput] = payload.txs.flatMap(tx =>
    tx.directive.zipWithIndex.map {
      case (directive, idx) =>
        val directive1: DBDirectiveGeneralizedClass = directive.toDBDirective
        val box: EncryBaseBox = directive.boxes(Digest32 @@ Algos.decode(tx.id).getOrElse(Array.emptyByteArray), idx)
       DBOutput(directive1, box.toDBBoxes, tx.id, true)
    }
  )
}

object Block {

  val empty: Block = Block(Header.empty, Payload.empty)

  implicit val decoder: Decoder[Block] = (c: HCursor) => {
    for {
      header <- c.downField("header").as[Header]
      payload <- c.downField("payload").as[Payload]
    } yield Block(
      header,
      payload,
    )
  }
}
