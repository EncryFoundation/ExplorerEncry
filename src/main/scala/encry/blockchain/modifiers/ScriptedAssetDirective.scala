package encry.blockchain.modifiers

import io.circe.syntax._
import encry.blockchain.modifiers.Directive.DTypeId
import io.circe.{Decoder, Encoder, HCursor}
import org.encryfoundation.common.Algos
import org.encryfoundation.common.utils.TaggedTypes.ADKey
import org.encryfoundation.prismlang.compiler.CompiledContract.ContractHash
import scorex.crypto.encode.Base16

case class ScriptedAssetDirective(contractHash: ContractHash,
                                  amount: Long,
                                  tokenIdOpt: Option[ADKey] = None) extends Directive{

  override def toDbDirective(txId: String, numberInTx: Int): DirectiveDBVersion =
    DirectiveDBVersion(txId, numberInTx, typeId, isValid, Base16.encode(contractHash), amount, "", tokenIdOpt.map(Base16.encode), "")

  override val isValid: Boolean = amount > 0
  override val typeId: DTypeId = ScriptedAssetDirective.TypeId
}

object ScriptedAssetDirective{
  val TypeId: DTypeId = 3.toByte

  implicit val jsonEncoder: Encoder[ScriptedAssetDirective] = (d: ScriptedAssetDirective) => Map(
    "typeId" -> d.typeId.asJson,
    "contractHash" -> Algos.encode(d.contractHash).asJson,
    "amount" -> d.amount.asJson,
    "tokenId" -> d.tokenIdOpt.map(id => Algos.encode(id)).asJson
  ).asJson

  implicit val jsonDecoder: Decoder[ScriptedAssetDirective] = (c: HCursor) => for {
    contractHash <- c.downField("contractHash").as[String]
    amount <- c.downField("amount").as[Long]
    tokenIdOpt <- c.downField("tokenId").as[Option[String]]
  } yield Algos.decode(contractHash)
    .map(ch => ScriptedAssetDirective(ch, amount, tokenIdOpt.flatMap(id => Algos.decode(id).map(ADKey @@ _).toOption)))
    .getOrElse(throw new Exception("Decoding failed"))
}
