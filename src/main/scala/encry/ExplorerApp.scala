package encry

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import encry.settings.ExplorerSettings
import encry.parser._
import scala.concurrent.ExecutionContextExecutor

object ExplorerApp extends App {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val settings = ExplorerSettings.read

  settings.parseSettings.nodes.foreach(node =>
    system.actorOf(Props(new NodeParser(node)), s"ParserFor${node.getHostName}")
  )
}
