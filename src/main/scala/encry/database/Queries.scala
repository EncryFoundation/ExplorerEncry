package encry.database

import java.net.InetSocketAddress

import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import doobie.free.connection.ConnectionIO
import doobie.util.update.Update
import doobie.postgres.implicits._
import doobie.implicits._
import encry.blockchain.modifiers.Header
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

  //id: String,
  //                  version: Byte,
  //                  parentId: String,
  //                  adProofsRoot: String,
  //                  stateRoot: String,
  //                  transactionsRoot: String,
  //                  timestamp: Long,
  //                  height: Int,
  //                  nonce: Long,
  //                  difficulty: BigInt,
  //                  equihashSolution: Array[Int]

  def insertHeaderQuery(header: Header, addr: InetSocketAddress): ConnectionIO[Int] = {
    val query: String =
      s"""
        |INSERT INTO public.headers (id, version, parent_id, adProofsRoot, stateRoot, transactionsRoot, timestamp, height, nonce,
        |       difficulty, equihashSolution, nodes)
        |VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """.stripMargin
    Update[Header](query).run(header.copy(nodes = List(addr.getAddress.getHostAddress)))
  }
}
