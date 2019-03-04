package encry

import akka.actor.{Actor, ActorRef, Props}
import encry.parser.NodeParser
import encry.settings.ParseSettings
import ExplorerApp._
import com.typesafe.scalalogging.StrictLogging

class ParsersController(settings: ParseSettings, dbActor: ActorRef) extends Actor with StrictLogging {

  override def preStart(): Unit = {
    settings.nodes.foreach(node =>
      system.actorOf(Props(new NodeParser(node, self, dbActor)).withDispatcher("parser-dispatcher"),
        s"ParserFor${node.getHostName}")
    )
  }

  override def receive: Receive = {
    case _ =>
  }
}
