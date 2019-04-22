package encry.blockchain.modifiers

import io.circe.syntax._
import com.google.common.primitives.Ints
import encry.blockchain.modifiers.Directive.DTypeId
import encry.blockchain.modifiers.boxes.{DataBox, EncryBaseBox, EncryProposition}
import encry.utils.CoreTaggedTypes.ModifierId
import encry.utils.Utils
import io.circe.{Decoder, Encoder, HCursor}
import org.encryfoundation.common.Algos
import org.encryfoundation.prismlang.compiler.CompiledContract.ContractHash
import scorex.crypto.encode.Base16
import scorex.crypto.hash.Digest32

case class DataDirective(contractHash: ContractHash, data: Array[Byte]) extends Directive {

  override val isValid: Boolean = data.length <= 1000

  override val typeId: DTypeId = DataDirective.TypeId

  override def toDBDirective: DBDirectiveGeneralizedClass = DBDirectiveGeneralizedClass()

  override def boxes(digest: Digest32, idx: Int): EncryBaseBox =
    DataBox(EncryProposition(contractHash), Utils.nonceFromDigest(digest ++ Ints.toByteArray(idx)), data)

  override def toDbVersion(txId: Array[Byte], numberInTx: Int): DirectiveDBVersion =
    DirectiveDBVersion(Base16.encode(txId), numberInTx, typeId, isValid, Base16.encode(contractHash), 0L, "", None, Base16
      .encode(data))
}

object DataDirective {
  val TypeId: DTypeId = 5.toByte

  implicit val jsonDecoder: Decoder[DataDirective] = (c: HCursor) => {
    for {
      contractHash  <- c.downField("contractHash").as[String]
      dataEnc       <- c.downField("data").as[String]
    } yield Algos.decode(contractHash)
      .flatMap(ch => Algos.decode(dataEnc).map(data => DataDirective(ch, data)))
      .getOrElse(throw new Exception("Decoding failed"))
  }

  implicit val jsonEncoder: Encoder[DataDirective] = (d: DataDirective) => Map(
    "typeId" -> d.typeId.asJson,
    "contractHash" -> Algos.encode(d.contractHash).asJson,
    "data" -> Algos.encode(d.data).asJson
  ).asJson
}