package encry.settings

import org.encryfoundation.common.Algos

object Constants {

  val IntrinsicTokenId: String = Algos.encode(Algos.hash("intrinsic_token"))
}
