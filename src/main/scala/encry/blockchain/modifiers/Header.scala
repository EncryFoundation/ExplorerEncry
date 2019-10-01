package encry.blockchain.modifiers

import io.circe.{Decoder, HCursor}

case class Header(id: String,
                  version: Byte,
                  parentId: String,
                  transactionsRoot: String,
                  timestamp: Long,
                  height: Int,
                  nonce: Long,
                  difficulty: Long,
                  stateRoot: String,
                  equihashSolution: List[Int])

object Header {

  val empty: Header = Header("", -1: Byte, "", "", 0L, 0, 0L, 0L, "",List.empty)

  implicit val jsonDecoder: Decoder[Header] = (c: HCursor) =>
    for {
      id               <- c.downField("id").as[String]
      version          <- c.downField("version").as[Byte]
      parentId         <- c.downField("parentId").as[String]
      txRoot           <- c.downField("txRoot").as[String]
      timestamp        <- c.downField("timestamp").as[Long]
      height           <- c.downField("height").as[Int]
      nonce            <- c.downField("nonce").as[Long]
      difficulty       <- c.downField("difficulty").as[Long]
      stateRoot        <- c.downField("stateRoot").as[String]
      equihashSolution <- c.downField("equihashSolution").as[List[Int]]
    } yield Header(
      id,
      version,
      parentId,
      txRoot,
      timestamp,
      height,
      nonce,
      difficulty,
      stateRoot,
      equihashSolution
    )
}

case class HeaderDBVersion(id: String,
                           version: Int,
                           parentId: String,
                           transactionsRoot: String,
                           timestamp: Long,
                           height: Int,
                           nonce: Long,
                           difficulty: Long,
                           stateRoot: String,
                           equihashSolution: List[Int],
                           txCount: Int,
                           minerAddress: String,
                           minerReward: Long)

object HeaderDBVersion {

  def apply(block: Block): HeaderDBVersion = {
    val (minerAddress: String, minerReward: Long) = minerInfo(block.payload.txs.find(_.inputs.isEmpty).getOrElse(Transaction()))
    HeaderDBVersion(
      block.header.id,
      block.header.version,
      block.header.parentId,
      block.header.transactionsRoot,
      block.header.timestamp,
      block.header.height,
      block.header.nonce,
      block.header.difficulty,
      block.header.stateRoot,
      block.header.equihashSolution,
      block.payload.txs.size,
      minerAddress,
      minerReward,
    )
  }

     def minerInfo(coinbase: Transaction): (String, Long) = coinbase.directive.headOption match {
    case Some(TransferDirective(address, amount, tokenIdOpt)) if tokenIdOpt.isEmpty => address -> amount
    case _ => "unknown" -> 0L
  }
}