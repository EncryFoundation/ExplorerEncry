package encry

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import encry.settings.ExplorerSettings

import scala.concurrent.ExecutionContextExecutor

object ExplorerApp extends App {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val settings = ExplorerSettings.read

}
