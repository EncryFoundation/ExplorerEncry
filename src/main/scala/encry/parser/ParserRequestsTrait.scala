package encry.parser

import com.typesafe.scalalogging.StrictLogging
import encry.blockchain.modifiers.{Block, Header}
import encry.blockchain.nodeRoutes.InfoRoute
import encry.blockchain.nodeRoutes.apiEntities.Peer
import io.circe.{Decoder, Error}

trait ParserRequestsTrait extends StrictLogging{

   def makeGetRequest[T](uri: String)(implicit decoder: Decoder[T]): Either[Error, T]

   def getInfo: Either[Error, InfoRoute]

   def getBlock(blockId: String): Either[Error, Block]

   def getBlocksAtHeight(height: Int): Either[Error, List[String]]

   def getLastIds(qty: Int, maxHeight: Int): Either[Error, List[String]]

   def getPeers: Either[Error, List[Peer]]

   def getHeaders(height: Int): Either[Error, List[Header]]
}
