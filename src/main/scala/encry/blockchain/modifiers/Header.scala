package encry.blockchain.modifiers

import io.circe.{Decoder, HCursor}

case class Header(version: String,
                  parentId: String,
                  adProofsRoot: String,
                  stateRoot: String,
                  transactionsRoot: String,
                  timestamp: Long,
                  height: Int,
                  nonce: Long,
                  difficulty: Long,
                  equihashSolution: List[Int])

