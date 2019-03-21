package encry.blockchain.nodeRoutes.apiEntities

import java.net.InetSocketAddress

import io.circe.{Decoder, HCursor}

case class Peer(address: InetSocketAddress,
                name: String,
                connectionType: String)

object Peer {

  implicit val decoder: Decoder[Peer] = (c: HCursor) => for {
    addressStr <- c.downField("address").as[String]
    name <- c.downField("name").as[String]
    connectionType <- c.downField("name").as[String]
  } yield Peer(
    new InetSocketAddress(addressStr.split("/").head, 9051),
    name,
    connectionType
  )
}