package encry.utils

import com.google.common.primitives.Longs
import org.encryfoundation.common.utils.Algos

object Utils {

  def nonceFromDigest(digest: Array[Byte]): Long = Longs.fromByteArray(Algos.hash(digest).take(8))

}
