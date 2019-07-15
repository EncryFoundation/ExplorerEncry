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
import scala.util.Try

case class ParserRequests(node: InetSocketAddress) extends StrictLogging {

  private def makeGetRequest[T](uri: String)(implicit decoder: Decoder[T]): Either[Throwable, T] = for {
    resp <- Try(Http(uri)
      .option(HttpOptions.readTimeout(60000))
      .option(HttpOptions.connTimeout(60000))
      .execute(parser = { inputStream =>
        val str: String = Source.fromInputStream(inputStream, "UTF8").mkString
        io.circe.parser.parse(str)
      })).toEither
    json <- resp.body
    elem <- decoder.decodeJson(json)
  } yield elem

  def getInfo: Either[Throwable, InfoRoute] =
    makeGetRequest[InfoRoute](s"http://${node.getAddress.getHostAddress}:${node.getPort}/info")

  def getBlock(blockId: String): Either[Throwable, Block] =
    makeGetRequest[Block](s"http://${node.getAddress.getHostAddress}:${node.getPort}/history/$blockId")

  def getBlocksAtHeight(height: Int): Either[Throwable, List[String]] =
    makeGetRequest[List[String]](s"http://${node.getAddress.getHostAddress}:${node.getPort}/history/at/$height")

  def getLastIds(qty: Int, maxHeight: Int): Either[Throwable, List[String]] =
    makeGetRequest[List[String]](s"http://${node.getAddress.getHostAddress}:${node.getPort}/history?limit=$qty&offset=${maxHeight - qty + 1}")

  def getPeers: Either[Throwable, List[Peer]] =
    makeGetRequest[List[Peer]](s"http://${node.getAddress.getHostAddress}:${node.getPort}/peers/connected")

  def getHeaders(height: Int): Either[Throwable, List[Header]]=
    makeGetRequest[List[Header]](s"http://${node.getAddress.getHostAddress}:${node.getPort}/history/lastHeaders/$height")
}