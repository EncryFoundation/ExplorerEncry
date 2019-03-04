package encry.database

import java.net.InetSocketAddress

import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import doobie.free.connection.ConnectionIO
import doobie.util.update.Update
import doobie.postgres.implicits._
import doobie.implicits._
import encry.blockchain.modifiers.{Block, Header, Transaction}
import encry.blockchain.nodeRoutes.InfoRoute
import encry.database.data.{DBTransaction, HeaderToNode, Node}

object Queries extends StrictLogging {

  def proccessBlock(block: Block, node: InetSocketAddress): ConnectionIO[Int] = for {
    header <- insertHeaderQuery(block.header)
    nodeToHeader <- insertNodeToHeader(block.header, node)
    txs <- insertTransactionsQuery(block)
  } yield header + nodeToHeader + txs

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

  def insertHeaderQuery(header: Header): ConnectionIO[Int] = {
    val query: String =
      s"""
        |INSERT INTO public.headers (id, version, parent_id, adProofsRoot, stateRoot, transactionsRoot, timestamp, height, nonce,
        |       difficulty, equihashSolution)
        |VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """.stripMargin
    Update[Header](query).run(header)
  }

  private def insertTransactionsQuery(block: Block): ConnectionIO[Int] = {
    val txs: List[DBTransaction] = block.payload.txs.map(tx => DBTransaction(tx, block.header.id))
    val query: String =
      """
        |INSERT INTO public.transactions (id, fee, timestamp, proof, coinbase, blockId)
        |VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;
        |""".stripMargin
    Update[DBTransaction](query).updateMany(txs)
  }

  def insertNodeToHeader(header: Header, addr: InetSocketAddress): ConnectionIO[Int] = {
    val headerToNode = HeaderToNode(header.id, addr.getAddress.getHostAddress)
    val query: String =
      s"""
         |INSERT INTO public.headerToNode (id, nodeIp)
         |VALUES(?, ?)
      """.stripMargin
    Update[HeaderToNode](query).run(headerToNode)
  }
}
