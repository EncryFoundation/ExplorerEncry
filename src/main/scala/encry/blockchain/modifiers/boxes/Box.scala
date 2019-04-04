package encry.blockchain.modifiers.boxes

import org.encryfoundation.common.utils.TaggedTypes.ADKey

trait Box[P <: EncryProposition]  {
  val proposition: P

  val id: ADKey
}

object Box {
  type Amount = Long
}
