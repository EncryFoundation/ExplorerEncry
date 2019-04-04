package encry.blockchain.nodeRoutes

import io.circe.{Decoder, HCursor}

case class InfoRoute(nodeName: String,
                     stateType: String,
                     headersHeight: Int,
                     fullHeight: Int,
                     bestHeaderId: String,
                     bestFullHeaderId: String,
                     previousFullHeaderId: String,
                     difficulty: Long,
                     unconfirmedCount: Long,
                     stateVersion: String,
                     isMining: Boolean,
                     peersCount: Int,
                     knownPeers: List[String],
                     storage: String,
                     isConnectedWithKnownPeers: Boolean){

  override def toString: String =
    s"Node: $nodeName\n " +
      s"headersHeight: $headersHeight\n " +
      s"fullHeight: $fullHeight\n " +
      s"bestHeaderId: $bestHeaderId\n " +
      s"bestFullHeaderId: $bestFullHeaderId\n"
}

object InfoRoute {

  val empty =
    InfoRoute("", "", 0, 0, "", "", "", 0L, 0L, "", isMining = false, 0, List.empty, "", isConnectedWithKnownPeers = false)

  implicit val decoder: Decoder[InfoRoute] = (c: HCursor) => for {
    nodeName                  <- c.downField("name").as[String]
    stateType                 <- c.downField("stateType").as[String]
    headersHeight             <- c.downField("headersHeight").as[Int]
    fullHeight                <- c.downField("fullHeight").as[Int]
    bestHeaderId              <- c.downField("bestHeaderId").as[String]
    bestFullHeaderId          <- c.downField("bestFullHeaderId").as[String]
    previousFullHeaderId      <- c.downField("previousFullHeaderId").as[String]
    difficulty                <- c.downField("difficulty").as[Long]
    unconfirmedCount          <- c.downField("unconfirmedCount").as[Long]
    stateVersion              <- c.downField("stateVersion").as[String]
    isMining                  <- c.downField("isMining").as[Boolean]
    peersCount                <- c.downField("peersCount").as[Int]
    knownPeers                <- c.downField("knownPeers").as[List[String]]
    storage                   <- c.downField("storage").as[String]
    isConnectedWithKnownPeers <- c.downField("isConnectedWithKnownPeers").as[Boolean]
  } yield InfoRoute(
    nodeName,
    stateType,
    headersHeight,
    fullHeight,
    bestHeaderId,
    bestFullHeaderId,
    previousFullHeaderId,
    difficulty,
    unconfirmedCount,
    stateVersion,
    isMining,
    peersCount,
    knownPeers,
    storage,
    isConnectedWithKnownPeers
  )
}
