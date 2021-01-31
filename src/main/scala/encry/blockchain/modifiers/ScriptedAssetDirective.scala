package encry.blockchain.modifiers

import com.google.common.primitives.Ints
import io.circe.syntax._
import encry.blockchain.modifiers.Directive.DTypeId
import encry.blockchain.modifiers.boxes.{AssetBox, EncryBaseBox, EncryProposition}
import encry.utils.Utils
import io.circe.{Decoder, Encoder, HCursor}
import org.encryfoundation.common.utils.Algos
import org.encryfoundation.common.utils.TaggedTypes.ADKey
import org.encryfoundation.prismlang.compiler.CompiledContract.ContractHash
import scorex.crypto.encode.Base16
import scorex.crypto.hash.Digest32

case class ScriptedAssetDirective(contractHash: ContractHash,
                                  amount: Long,
                                  tokenIdOpt: Option[ADKey] = None) extends Directive{

  override def toDBDirective: DBDirectiveGeneralizedClass = DBDirectiveGeneralizedClass()

  override val isValid: Boolean = amount > 0

  override val typeId: DTypeId = ScriptedAssetDirective.TypeId

  override def boxes(digest: Digest32, idx: Int): EncryBaseBox =
    AssetBox(EncryProposition(contractHash), Utils.nonceFromDigest(digest ++ Ints.toByteArray(idx)), amount)

  override def toDbVersion(txId: Array[Byte], numberInTx: Int): DirectiveDBVersion =
    DirectiveDBVersion(Base16.encode(txId), numberInTx, typeId, isValid, Base16.encode(contractHash), amount, "", tokenIdOpt.map(Base16.encode), "")
}

object ScriptedAssetDirective{
  val TypeId: DTypeId = 3.toByte

  implicit val jsonDecoder: Decoder[ScriptedAssetDirective] = (c: HCursor) => for {
    contractHash  <- c.downField("contractHash").as[String]
    amount        <- c.downField("amount").as[Long]
    tokenIdOpt    <- c.downField("tokenId").as[Option[String]]
  } yield Algos.decode(contractHash)
    .map(ch => ScriptedAssetDirective(ch, amount, tokenIdOpt.flatMap(id => Algos.decode(id).map(ADKey @@ _).toOption)))
    .getOrElse(throw new Exception("Decoding failed"))

  implicit val jsonEncoder: Encoder[ScriptedAssetDirective] = (d: ScriptedAssetDirective) => Map(
    "typeId" -> d.typeId.asJson,
    "contractHash" -> Algos.encode(d.contractHash).asJson,
    "amount" -> d.amount.asJson,
    "tokenId" -> d.tokenIdOpt.map(id => Algos.encode(id)).asJson
  ).asJson
}
