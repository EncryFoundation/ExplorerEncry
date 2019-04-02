package encry.blockchain.modifiers

import io.circe.syntax._
import encry.blockchain.modifiers.Directive.DTypeId
import io.circe.{Decoder, Encoder, HCursor}
import org.encryfoundation.common.Algos
import org.encryfoundation.prismlang.compiler.CompiledContract.ContractHash
import scorex.crypto.encode.Base16

case class AssetIssuingDirective (contractHash: ContractHash, amount: Long) extends Directive{

  override def toDbDirective(txId: String, numberInTx: Int): DirectiveDBVersion =
  DirectiveDBVersion(txId, numberInTx, typeId, isValid, Base16.encode(contractHash), amount, "", None, "")

  override val isValid: Boolean = amount > 0
  override val typeId: DTypeId = AssetIssuingDirective.TypeId
}

object AssetIssuingDirective {

  val TypeId: DTypeId = 2.toByte

  implicit val jsonEncoder: Encoder[AssetIssuingDirective] = (d: AssetIssuingDirective) => Map(
    "typeId" -> d.typeId.asJson,
    "contractHash" -> Algos.encode(d.contractHash).asJson,
    "amount" -> d.amount.asJson
  ).asJson

  implicit val jsonDecoder: Decoder[AssetIssuingDirective] = (c: HCursor) => {
    for {
      contractHash <- c.downField("contractHash").as[String]
      amount <- c.downField("amount").as[Long]
    } yield Algos.decode(contractHash)
      .map(ch => AssetIssuingDirective(ch, amount))
      .getOrElse(throw new Exception("Decoding failed"))
  }
}