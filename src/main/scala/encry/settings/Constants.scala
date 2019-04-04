package encry.settings

import org.encryfoundation.common.Algos

object Constants {

  val IntrinsicTokenId: Array[Byte] = Algos.hash("intrinsic_token")
}
