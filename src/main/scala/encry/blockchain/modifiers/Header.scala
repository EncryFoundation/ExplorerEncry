package encry.blockchain.modifiers

import io.circe.{Decoder, HCursor}

case class Header(id: String,
                  version: Byte,
                  parentId: String,
                  adProofsRoot: String,
                  stateRoot: String,
                  transactionsRoot: String,
                  timestamp: Long,
                  height: Int,
                  nonce: Long,
                  difficulty: Long,
                  equihashSolution: List[Int])

object Header {

  val empty: Header = Header("", -1: Byte, "", "", "", "", 0L, 0, 0L, 0L, List.empty)

  implicit val jsonDecoder: Decoder[Header] = (c: HCursor) =>
    for {
      id               <- c.downField("id").as[String]
      version          <- c.downField("version").as[Byte]
      parentId         <- c.downField("parentId").as[String]
      adProofsRoot     <- c.downField("adProofsRoot").as[String]
      stateRoot        <- c.downField("stateRoot").as[String]
      txRoot           <- c.downField("txRoot").as[String]
      timestamp        <- c.downField("timestamp").as[Long]
      height           <- c.downField("height").as[Int]
      nonce            <- c.downField("nonce").as[Long]
      difficulty       <- c.downField("difficulty").as[Long]
      equihashSolution <- c.downField("equihashSolution").as[List[Int]]
    } yield Header(
      id,
      version,
      parentId,
      adProofsRoot,
      stateRoot,
      txRoot,
      timestamp,
      height,
      nonce,
      difficulty,
      equihashSolution,
    )
}

