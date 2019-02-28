package encry.parser

import java.net.InetSocketAddress

import scala.concurrent.duration._
import akka.actor.Actor
import com.typesafe.scalalogging.StrictLogging
import encry.blockchain.nodeRoutes.InfoRoute
import encry.parser.NodeParser.PingNode
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class NodeParser(node: InetSocketAddress) extends Actor with StrictLogging {

  val parserRequests: ParserRequests = ParserRequests(node)
  var currentNodeInfo: InfoRoute = InfoRoute.empty

  override def preStart(): Unit = {
    logger.info(s"Start monitoring: ${node.getAddress}")
    context.system.scheduler.schedule(
      10 seconds,
      10 seconds
    )(self ! PingNode)
  }

  override def receive: Receive = {
    case PingNode =>
      parserRequests.getInfo match {
        case Left(err) => logger.info(s"Error during request to $node: ${err.getMessage}")
        case Right(newInfoRoute) => if (newInfoRoute == currentNodeInfo)
          logger.info(s"info route on node $node don't change")
        else {
          logger.info(s"Update node info on $node to $newInfoRoute|${newInfoRoute == currentNodeInfo}")
          currentNodeInfo = newInfoRoute
        }
      }
    case _ =>
  }
}

object NodeParser {

  case object PingNode
}

