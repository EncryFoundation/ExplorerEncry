package encry.parser.NodeParserTests

import java.net.InetSocketAddress

import com.typesafe.scalalogging.StrictLogging
import io.circe.Error

case class DummyParserRequests(node: InetSocketAddress) extends StrictLogging {

  var condition: Boolean = true

  val coll1 = List("1", "2", "3", "100")
  val coll2 = List("50", "51", "100")

  def getLastIds(qty: Int, maxHeight: Int): Either[Error, List[String]] = {
    if (condition) {
      condition = false
      Right(coll1)
    }
    else Right(coll2)
  }
}
