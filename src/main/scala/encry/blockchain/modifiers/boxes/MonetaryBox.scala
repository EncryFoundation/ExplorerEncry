package encry.blockchain.modifiers.boxes

import encry.blockchain.modifiers.boxes.Box.Amount

trait MonetaryBox extends EncryBaseBox {

  val amount: Amount
}
