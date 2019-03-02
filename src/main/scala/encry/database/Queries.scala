package encry.database

import java.net.InetSocketAddress
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import doobie.free.connection.ConnectionIO
import doobie.util.update.Update
import doobie.postgres.implicits._
import doobie.implicits._
import encry.blockchain.nodeRoutes.InfoRoute
import encry.database.data.Node

object Queries extends StrictLogging {

  def nodeInfoQuery(addr: InetSocketAddress): ConnectionIO[Option[Node]] = {
    val test = addr.getAddress.getHostAddress
    logger.info(test)
    sql"""SELECT * FROM public.nodes WHERE ip = $test;""".query[Node].option
  }

  def insertNode(addr: InetSocketAddress, nodeInfo: InfoRoute): ConnectionIO[Int] = {
    val nodeIns = Node(addr, nodeInfo)
    val query = "INSERT INTO public.nodes (ip, status, lastFullBlock, lastFullHeight) VALUES(?, ?, ?, ?) ON CONFLICT (ip) DO " +
      "UPDATE SET status = true"
    Update[Node](query).run(nodeIns)
  }
}
