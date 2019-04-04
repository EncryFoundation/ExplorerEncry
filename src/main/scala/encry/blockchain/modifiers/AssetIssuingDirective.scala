package encry.blockchain.modifiers

import com.google.common.primitives.Ints
import io.circe.syntax._
import encry.blockchain.modifiers.Directive.DTypeId
import encry.blockchain.modifiers.boxes.{EncryBaseBox, EncryProposition, TokenIssuingBox}
import encry.utils.Utils
import io.circe.{Decoder, Encoder, HCursor}
import org.encryfoundation.common.Algos
import org.encryfoundation.prismlang.compiler.CompiledContract.ContractHash
import scorex.crypto.hash.Digest32

case class AssetIssuingDirective (contractHash: ContractHash, amount: Long) extends Directive{

  override def toDBDirective: DBDirectiveGeneralizedClass = DBDirectiveGeneralizedClass()

  override val isValid: Boolean = amount > 0

  override val typeId: DTypeId = AssetIssuingDirective.TypeId

  override def boxes(digest: Digest32, idx: Int): EncryBaseBox =
    TokenIssuingBox(
      EncryProposition(contractHash),
      Utils.nonceFromDigest(digest ++ Ints.toByteArray(idx)),
      amount,
      Algos.hash(Ints.toByteArray(idx) ++ digest)
    )
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