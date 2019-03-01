package encry.blockchain.modifiers

import io.circe.{Decoder, HCursor}
import org.encryfoundation.common.Algos

case class Header(version: Byte,
                  parentId: String,
                  adProofsRoot: String,
                  stateRoot: String,
                  transactionsRoot: String,
                  timestamp: Long,
                  height: Int,
                  nonce: Long,
                  difficulty: BigInt,
                  equihashSolution: List[Int])

object Header {
  
  val empty: Header = Header(-1: Byte, "", "", "", "", 0L, 0, 0L, 0L, List.empty)


  implicit val jsonDecoder: Decoder[Header] = (c: HCursor) =>
    for {
      version          <- c.downField("version").as[Byte]
      parentId         <- c.downField("parentId").as[String]
      adProofsRoot     <- c.downField("adProofsRoot").as[String]
      stateRoot        <- c.downField("stateRoot").as[String]
      txRoot           <- c.downField("txRoot").as[String]
      timestamp        <- c.downField("timestamp").as[Long]
      height           <- c.downField("height").as[Int]
      nonce            <- c.downField("nonce").as[Long]
      difficulty       <- c.downField("difficulty").as[BigInt]
      equihashSolution <- c.downField("equihashSolution").as[List[Int]]
    } yield Header(
      version,
      parentId,
      adProofsRoot,
      stateRoot,
      txRoot,
      timestamp,
      height,
      nonce,
      difficulty,
      equihashSolution
    )
}

