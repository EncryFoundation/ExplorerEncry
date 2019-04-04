package encry.blockchain.modifiers.boxes

trait EncryBox[P <: EncryProposition] extends EncryBaseBox {

  override val proposition: P

}

object EncryBox {

  val BoxIdSize = 32
}

