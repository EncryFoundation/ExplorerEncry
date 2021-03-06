package encry.settings

import org.encryfoundation.common.utils.Algos

object Constants {
  val IntrinsicTokenId: Array[Byte] = Algos.hash("intrinsic_token")

  val EttTokenId: String = Algos.encode(IntrinsicTokenId)
}
