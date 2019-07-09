package encry.database

import java.net.InetSocketAddress

import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import doobie.free.connection.ConnectionIO
import doobie.util.update.Update
import doobie.implicits._
import encry.blockchain.modifiers.{Block, DirectiveDBVersion, Header, HeaderDBVersion, Transaction}
import encry.blockchain.nodeRoutes.InfoRoute
import encry.database.data._
import doobie.postgres.implicits._
import org.encryfoundation.common.utils.Algos

object Queries extends StrictLogging {

  def processBlock(block: Block, node: InetSocketAddress, nodeInfo: InfoRoute): ConnectionIO[Int] = (for {
    header            <- insertHeaderQuery(HeaderDBVersion(block))
    nodeToHeader      <- insertNodeToHeader(block.header, node)
    txs               <- insertTransactionsQuery(block)
    inputs            <- insertInputsQuery(block.getDBInputs)
    nonActiveOutputs  <- markOutputsAsNonActive(block.getDBInputs)
    tokens            <- insertTokens(block.getDbOutputs)
    accounts          <- insertAccounts(block.getDbOutputs)
    outputs           <- insertOutputsQuery(block.getDbOutputs)
    dir               <- insertDirectivesQuery(block.payload.txs)
    // _                 <- updateNode(nodeInfo, node)
  } yield for {
    _ <- header
    _ <- nodeToHeader
    _ <- txs
    _ <- inputs
    _ <- nonActiveOutputs
    _ <- tokens
    _ <- accounts
    _ <- outputs
    _ <- dir
  } yield 1).map {
    case Left(th) => th.printStackTrace(); throw th
    case Right(_) => 1
  }

  def removeBlock(addr: InetSocketAddress, block: Block): ConnectionIO[Int] = for {
    directives      <- removeDirectivesQuery(block.payload.txs)
    outputs         <- {
      removeOutputsQuery(block.getDbOutputs)
    }
    activeOutputs   <- {
      markOutputsAsActive(block.getDBInputs)
    }
    inputs          <- {
      removeInputsQuery(block.getDBInputs)
    }
    txs             <- {
      removeTransactionsQuery(block)
    }
    nodesToHeader    <- {
      deleteNodeToHeader(block.header, addr)
    }
    headers         <- {
      deleteHeaderQuery(HeaderDBVersion(block))
    }
  } yield directives  + outputs + activeOutputs + inputs + txs + nodesToHeader + headers

  def nodeInfoQuery(addr: InetSocketAddress): ConnectionIO[Option[Header]] = {
    sql"""SELECT * FROM public.headers ORDER BY height DESC LIMIT 1;""".query[Header].option
  }

  def insertNode(addr: InetSocketAddress, nodeInfo: InfoRoute, status: Boolean): ConnectionIO[Int] = {
    val nodeIns = Node(addr, nodeInfo)
    val query =
      s"""
         |INSERT INTO public.nodes (ip, status, lastFullBlock, lastFullHeight)
         |VALUES(?, ?, ?, ?) ON CONFLICT (ip) DO UPDATE SET status = $status,
         |lastFullBlock = '${nodeIns.lastFullBlock}', lastFullHeight = ${nodeIns.lastFullHeight};
         """.stripMargin
    Update[Node](query).run(nodeIns)
  }

  //  def updateNode(nodeInfo: InfoRoute, address: InetSocketAddress): ConnectionIO[Int] = {
  //    val query = "UPDATE public.nodes SET lastFullBlock = ?, lastFullHeight = ? WHERE ip = ?"
  //    Update[(String, Int, String)](query).run(nodeInfo.bestFullHeaderId, nodeInfo.fullHeight, address.getAddress.getHostName)
  //  }

  def insertHeaderQuery(block: HeaderDBVersion): ConnectionIO[Either[Throwable, Int]] = {
    val query: String =
      s"""
         |INSERT INTO public.headers (id, version, parent_id, transactionsRoot, timestamp, height, nonce,
         |       difficulty, equihashSolution, txCount, minerAddress, minerReward)
         |VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;
      """.stripMargin
    Update[HeaderDBVersion](query).run(block)
  }.attempt

  private def deleteHeaderQuery(header: HeaderDBVersion): ConnectionIO[Int] = {
    val query = """DELETE FROM headers where id = ?;"""
    Update[String](query).run(header.id)
  }

  private def insertTransactionsQuery(block: Block): ConnectionIO[Either[Throwable, Int]] = {
    val txs: List[DBTransaction] = block.payload.txs.map(tx => DBTransaction(tx, block.header.id))
    val query: String =
      """
        |INSERT INTO public.transactions (id, fee, timestamp, proof, coinbase, blockId)
        |VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;
        |""".stripMargin
    Update[DBTransaction](query).updateMany(txs)
  }.attempt

  private def removeTransactionsQuery(block: Block): ConnectionIO[Int] = {
    val query = """DELETE FROM transactions WHERE id = ?;"""
    Update[String](query).updateMany(block.payload.txs.map(_.id))
  }

  private def insertInputsQuery(inputs: List[DBInput]): ConnectionIO[Either[Throwable, Int]] = {
    val query: String =
      """
        |INSERT INTO public.inputs (bxId, txId, contract, proofs)
        |VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING;
        |""".stripMargin
    Update[DBInput](query).updateMany(inputs)
  }.attempt

  private def removeInputsQuery(inputs: List[DBInput]): ConnectionIO[Int] = {
    val query = "DELETE FROM public.inputs WHERE bxId = ?;"
    Update[String](query).updateMany(inputs.map(_.bxId))
  }

  private def markOutputsAsNonActive(inputs: List[DBInput]): ConnectionIO[Either[Throwable, Int]] = {
    val query: String =
      """
        |UPDATE public.outputs SET isActive = false WHERE id = ?
        |""".stripMargin
    Update[String](query).updateMany(inputs.map(_.bxId))
  }.attempt

  private def markOutputsAsActive(inputs: List[DBInput]): ConnectionIO[Int] = {
    val query: String =
      """
        |UPDATE public.outputs SET isActive = true WHERE id = ?
        |""".stripMargin
    Update[String](query).updateMany(inputs.map(_.bxId))
  }

  private def insertOutputsQuery(outputs: List[DBOutput]): ConnectionIO[Either[Throwable, Int]] = {
    val query: String =
      """
        |INSERT INTO public.outputs (id, boxType, txId, monetaryValue, nonce, coinId, contractHash, data, isActive, minerAddress)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;
        |""".stripMargin
    Update[DBOutput](query).updateMany(outputs)
  }.attempt

  private def removeOutputsQuery(outputs: List[DBOutput]): ConnectionIO[Int] = {
    val query = """DELETE FROM outputs where id = ?;"""
    Update[String](query).updateMany(outputs.map(_.id))
  }

  private def insertTokens(outputs: List[DBOutput]): ConnectionIO[Either[Throwable, Int]] = {
    val tokens = outputs.map(output => Token(output.coinId))
    val query: String =
      """
        |INSERT INTO public.tokens (id)
        |VALUES (?) ON CONFLICT DO NOTHING;
        |""".stripMargin
    Update[Token](query).updateMany(tokens)
  }.attempt

  private def insertAccounts(outputs: List[DBOutput]): ConnectionIO[Either[Throwable, Int]] = {
    val accounts = outputs.map(output => Account(output.contractHash))
    val query: String =
      """
        |INSERT INTO public.accounts (contractHash)
        |VALUES (?) ON CONFLICT DO NOTHING;
        |""".stripMargin
    Update[Account](query).updateMany(accounts)
  }.attempt

  def insertNodeToHeader(header: Header, addr: InetSocketAddress): ConnectionIO[Either[Throwable, Int]] = {
    val headerToNode = HeaderToNode(header.id, addr.getAddress.getHostAddress)
    val query: String =
      s"""
         |INSERT INTO public.headerToNode (id, nodeIp)
         |VALUES(?, ?) ON CONFLICT DO NOTHING;
      """.stripMargin
    Update[HeaderToNode](query).run(headerToNode)
  }.attempt

  private def deleteNodeToHeader(header: Header, addr: InetSocketAddress): ConnectionIO[Int] = {
    val query = """DELETE FROM headerToNode where nodeIp = ? AND id = ?;"""
    Update[(String, String)](query).run((addr.getAddress.getHostName, header.id))
  }

  def dropHeaderFromNode(headerId: String, addr: InetSocketAddress): ConnectionIO[Int] = {
    sql"""DELETE FROM public.headers WHERE id = '$headerId';""".query[Int].unique
  }

  private def insertDirectivesQuery(txs: Seq[Transaction]): ConnectionIO[Either[Throwable, Int]] = {
    val directives: Seq[DirectiveDBVersion] = txs.map(tx => tx.id -> tx.directive).flatMap {
      case (id, directives) => directives.zipWithIndex.map {
        case (directive, number) => directive.toDbVersion(Algos.decode(id).getOrElse(Array.emptyByteArray), number)
      }
    }
    val query: String =
      """
        |INSERT INTO public.directives (tx_id, number_in_tx, type_id, is_valid, contract_hash, amount, address, token_id_opt, data_field)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;
        |""".stripMargin
    Update[DirectiveDBVersion](query).updateMany(directives.toList)
  }.attempt

  private def removeDirectivesQuery(txs: Seq[Transaction]): ConnectionIO[Int] = {
    val query = s"""DELETE FROM directives WHERE tx_id = ?;"""
    Update[String](query).updateMany(txs.map(_.id).toList)
  }
}
