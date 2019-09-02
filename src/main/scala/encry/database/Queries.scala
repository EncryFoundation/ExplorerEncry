package encry.database

import java.net.InetSocketAddress

import cats.free.Free
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import doobie.free.connection.ConnectionIO
import doobie.util.update.Update
import doobie.implicits._
import encry.blockchain.modifiers.{Block, DirectiveDBVersion, Header, HeaderDBVersion, Transaction}
import encry.blockchain.nodeRoutes.InfoRoute
import encry.database.data._
import doobie.postgres.implicits._
import doobie.util.fragment.Fragment
import org.encryfoundation.common.utils.Algos

object Queries extends StrictLogging {

  def blocksIdsQuery(from: Int, to: Int): ConnectionIO[List[String]] =
    sql"""SELECT id FROM headers WHERE height >= $from AND height <= $to""".query[String].to[List]

  def processBlock(block: Block, node: InetSocketAddress, nodeInfo: InfoRoute): ConnectionIO[Unit] = for {
    _ <- insertHeaderQuery(HeaderDBVersion(block))
    _ <- insertNodeToHeader(block.header, node)
    _ <- insertTransactionsQuery(block)
    _ <- insertInputsQuery(block.getDBInputs)
    _ <- markOutputsAsNonActive(block.getDBInputs)
    _ <- insertTokens(block.getDbOutputs)
    _ <- insertAccounts(block.getDbOutputs)
    _ <- insertOutputsQuery(block.getDbOutputs)
    _ <- insertDirectivesQuery(block.payload.txs)
  } yield ()

  def removeBlock(addr: InetSocketAddress, block: Block): ConnectionIO[Unit] = for {
    _ <- removeDirectivesQuery(block.payload.txs)
    _ <- removeOutputsQuery(block.getDbOutputs)
    _ <- markOutputsAsActive(block.getDBInputs)
    _ <- removeInputsQuery(block.getDBInputs)
    _ <- removeTransactionsQuery(block)
    _ <- deleteNodeToHeader(block.header, addr)
    _ <- deleteHeaderQuery(HeaderDBVersion(block))
  } yield ()

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

  def insertHeaderQuery(block: HeaderDBVersion): ConnectionIO[Int] = {
    val query: String =
      s"""
         |INSERT INTO public.headers (id, version, parent_id, transactionsRoot, timestamp, height, nonce,
         |       difficulty, equihashSolution, txCount, minerAddress, minerReward)
         |VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;
      """.stripMargin
    Update[HeaderDBVersion](query).run(block)
  }

  private def deleteHeaderQuery(header: HeaderDBVersion): ConnectionIO[Int] = {
    val query = """DELETE FROM headers where id = ?;"""
    Update[String](query).run(header.id)
  }

  private def insertTransactionsQuery(block: Block): ConnectionIO[Int] = {
    val txs: List[DBTransaction] = block.payload.txs.map(tx => DBTransaction(tx, block.header.id))

    txs.grouped(5000).map { txs =>
      (fr"INSERT INTO public.transactions (id, fee, timestamp, proof, coinbase, blockId) VALUES " ++
        txs.init.map(tx => fr"(${tx.id}, ${tx.fee}, ${tx.timestamp}, ${tx.defaultProofOpt}, ${tx.isCoinbase}, ${tx.blockId}), ").fold(Fragment.empty)(_ ++ _) ++
        txs.lastOption.map(tx => fr"(${tx.id}, ${tx.fee}, ${tx.timestamp}, ${tx.defaultProofOpt}, ${tx.isCoinbase}, ${tx.blockId})").get ++
        fr" ON CONFLICT DO NOTHING;").update.run
    }.reduceLeft(_ *> _)
  }

  private def removeTransactionsQuery(block: Block): ConnectionIO[Int] = {
    val query = """DELETE FROM transactions WHERE id = ?;"""
    Update[String](query).updateMany(block.payload.txs.map(_.id))
  }

  private def insertInputsQuery(inputs: List[DBInput]): ConnectionIO[Int] = {
    val query: String =
      """
        |INSERT INTO public.inputs (bxId, txId, contract, proofs)
        |VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING;
        |""".stripMargin
    if (inputs.nonEmpty) {
      inputs.grouped(7500).map { inputs =>
        (fr"INSERT INTO public.inputs (bxId, txId, contract, proofs) VALUES " ++
          inputs.init.map(i => fr"(${i.bxId}, ${i.txId}, ${i.contract}, ${i.proofs}), ").fold(Fragment.empty)(_ ++ _) ++
          inputs.lastOption.map(i => fr"(${i.bxId}, ${i.txId}, ${i.contract}, ${i.proofs})").get ++
          fr" ON CONFLICT DO NOTHING;").update.run
      }.reduceLeft(_ *> _)
    } else Free.pure(0)
  }

  private def removeInputsQuery(inputs: List[DBInput]): ConnectionIO[Int] = {
    val query = "DELETE FROM public.inputs WHERE bxId = ?;"
    Update[String](query).updateMany(inputs.map(_.bxId))
  }

  private def markOutputsAsNonActive(inputs: List[DBInput]): ConnectionIO[Int] = {
    val query: String =
      """
        |UPDATE public.outputs SET isActive = false WHERE id = ?
        |""".stripMargin
    Update[String](query).updateMany(inputs.map(_.bxId))
  }

  private def markOutputsAsActive(inputs: List[DBInput]): ConnectionIO[Int] = {
    val query: String =
      """
        |UPDATE public.outputs SET isActive = true WHERE id = ?
        |""".stripMargin
    Update[String](query).updateMany(inputs.map(_.bxId))
  }

  private def insertOutputsQuery(outputs: List[DBOutput]): ConnectionIO[Int] = {
    val query: String =
      """
        |INSERT INTO public.outputs (id, boxType, txId, monetaryValue, nonce, coinId, contractHash, data, isActive, minerAddress)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;
        |""".stripMargin

    if (outputs.nonEmpty) {
      outputs.grouped(3000).map { outputs =>
        (fr"INSERT INTO public.outputs (id, boxType, txId, monetaryValue, nonce, coinId, contractHash, data, isActive, minerAddress) VALUES " ++
          outputs.init.map(o => fr"(${o.id}, ${o.boxType}, ${o.txId}, ${o.monetaryValue}, ${o.nonce}, ${o.coinId}, ${o.contractHash}, ${o.data}, ${o.isActive}, ${o.minerAddress}), ").fold(Fragment.empty)(_ ++ _) ++
          outputs.lastOption.map(o => fr"(${o.id}, ${o.boxType}, ${o.txId}, ${o.monetaryValue}, ${o.nonce}, ${o.coinId}, ${o.contractHash}, ${o.data}, ${o.isActive}, ${o.minerAddress})").get ++
          fr" ON CONFLICT DO NOTHING;").update.run
      }.reduceLeft(_ *> _)
    } else Free.pure(0)
  }

  private def removeOutputsQuery(outputs: List[DBOutput]): ConnectionIO[Int] = {
    val query = """DELETE FROM outputs where id = ?;"""
    Update[String](query).updateMany(outputs.map(_.id))
  }

  private def insertTokens(outputs: List[DBOutput]): ConnectionIO[Int] = {
    val tokens = outputs.map(output => Token(output.coinId))
    val query: String =
      """
        |INSERT INTO public.tokens (id)
        |VALUES (?) ON CONFLICT DO NOTHING;
        |""".stripMargin
    Update[Token](query).updateMany(tokens)
  }

  private def insertAccounts(outputs: List[DBOutput]): ConnectionIO[Int] = {
    val accounts = outputs.map(output => Account(output.contractHash))
    val query: String =
      """
        |INSERT INTO public.accounts (contractHash)
        |VALUES (?) ON CONFLICT DO NOTHING;
        |""".stripMargin
    Update[Account](query).updateMany(accounts)
  }

  def insertNodeToHeader(header: Header, addr: InetSocketAddress): ConnectionIO[Int] = {
    val headerToNode = HeaderToNode(header.id, addr.getAddress.getHostAddress)
    val query: String =
      s"""
         |INSERT INTO public.headerToNode (id, nodeIp)
         |VALUES(?, ?) ON CONFLICT DO NOTHING;
      """.stripMargin
    Update[HeaderToNode](query).run(headerToNode)
  }

  private def deleteNodeToHeader(header: Header, addr: InetSocketAddress): ConnectionIO[Int] = {
    val query = """DELETE FROM headerToNode where nodeIp = ? AND id = ?;"""
    Update[(String, String)](query).run((addr.getAddress.getHostName, header.id))
  }

  def dropHeaderFromNode(headerId: String, addr: InetSocketAddress): ConnectionIO[Int] = {
    sql"""DELETE FROM public.headers WHERE id = '$headerId';""".query[Int].unique
  }

  private def insertDirectivesQuery(txs: Seq[Transaction]): ConnectionIO[Int] = {
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
    if (directives.nonEmpty) {
      directives.grouped(3300).map { directives =>
        (fr"INSERT INTO public.directives (tx_id, number_in_tx, type_id, is_valid, contract_hash, amount, address, token_id_opt, data_field) VALUES " ++
          directives.init.map(d => fr"(${d.txId}, ${d.numberInTx}, ${d.dTypeId}, ${d.isValid}, ${d.contractHash}, ${d.amount}, ${d.address}, ${d.tokenIdOpt}, ${d.data}), ").fold(Fragment.empty)(_ ++ _) ++
          directives.lastOption.map(d => fr"(${d.txId}, ${d.numberInTx}, ${d.dTypeId}, ${d.isValid}, ${d.contractHash}, ${d.amount}, ${d.address}, ${d.tokenIdOpt}, ${d.data})").get ++
          fr" ON CONFLICT DO NOTHING;").update.run
      }.reduceLeft(_ *> _)
    } else Free.pure(0)
  }

  private def removeDirectivesQuery(txs: Seq[Transaction]): ConnectionIO[Int] = {
    val query = s"""DELETE FROM directives WHERE tx_id = ?;"""
    Update[String](query).updateMany(txs.map(_.id).toList)
  }

}
