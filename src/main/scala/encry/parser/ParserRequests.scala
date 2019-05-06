package encry.parser

import java.net.InetSocketAddress

import encry.blockchain.nodeRoutes.InfoRoute
import io.circe.{Decoder, Error}
import scalaj.http._
import InfoRoute._
import com.typesafe.scalalogging.StrictLogging
import encry.blockchain.modifiers.Block._
import encry.blockchain.modifiers.{Block, Header}
import encry.blockchain.nodeRoutes.apiEntities.Peer
import scala.io.Source

case class ParserRequests(node: InetSocketAddress) extends ParserRequestsTrait with StrictLogging {

  override def makeGetRequest[T](uri: String)(implicit decoder: Decoder[T]): Either[Error, T] = for {
    json <- Http(uri).execute(parser = { inputStream =>
      val str = Source.fromInputStream(inputStream, "UTF8").mkString
      io.circe.parser.parse(str)
    }).body
    elem <- decoder.decodeJson(json)
  } yield elem

 override def getInfo: Either[Error, InfoRoute] =
    makeGetRequest[InfoRoute](s"http://${node.getAddress.getHostAddress}:${node.getPort}/info")

 override def getBlock(blockId: String): Either[Error, Block] =
    makeGetRequest[Block](s"http://${node.getAddress.getHostAddress}:${node.getPort}/history/$blockId")

  def getBlocksAtHeight(height: Int): Either[Error, List[String]] =
    makeGetRequest[List[String]](s"http://${node.getAddress.getHostAddress}:${node.getPort}/history/at/$height")

  def getLastIds(qty: Int, maxHeight: Int): Either[Error, List[String]] =
    makeGetRequest[List[String]](s"http://${node.getAddress.getHostAddress}:${node.getPort}/history?limit=$qty&offset=${maxHeight - qty + 1}")

  def getPeers: Either[Error, List[Peer]] =
    makeGetRequest[List[Peer]](s"http://${node.getAddress.getHostAddress}:${node.getPort}/peers/connected")

  def getHeaders(height: Int): Either[Error, List[Header]]=
    makeGetRequest[List[Header]](s"http://${node.getAddress.getHostAddress}:${node.getPort}/history/lastHeaders/$height")
}