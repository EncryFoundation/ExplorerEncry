package encry.blockchain.nodeRoutes.apiEntities

import java.net.InetSocketAddress

import io.circe.{Decoder, HCursor}

case class Peer(address: InetSocketAddress,
                name: String,
                connectionType: String)

object Peer {

  implicit val decoder: Decoder[Peer] = (c: HCursor) => for {
    addressStr      <- c.downField("address").as[String]
    name            <- c.downField("name").as[String]
    connectionType  <- c.downField("connectionType").as[String]
  } yield {
    val ip: String = addressStr.split("/").last.split(":").head
//    val ip: String = if (address.head.isEmpty) address.last.split(":").head else address.head
    Peer(
      new InetSocketAddress (ip, 9051),
      name,
      connectionType
    )
  }

}