package encry.settings

import java.net.InetSocketAddress

case class ParseSettings(nodes: List[InetSocketAddress],
                         recoverBatchSize: Int,
                         infinitePing: Boolean,
                         numberOfAttempts: Option[Int] = None)
