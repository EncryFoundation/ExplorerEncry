package encry.parser

import java.net.InetSocketAddress
import akka.actor.Actor
import com.typesafe.scalalogging.StrictLogging

class NodeParser(node: InetSocketAddress) extends Actor with StrictLogging {

  override def preStart(): Unit = {
    logger.info(s"Start monitoring: ${node.getAddress}")
  }

  override def receive: Receive = {
    case _ =>
  }
}

