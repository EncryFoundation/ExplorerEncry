package encry.settings

import org.encryfoundation.common.Algos
import scorex.crypto.hash.Digest32

object Constants {

  val IntrinsicTokenId: Array[Byte] = Algos.hash("intrinsic_token")
}
