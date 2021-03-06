package encry.blockchain.modifiers

import com.google.common.primitives.Ints
import io.circe.syntax._
import encry.blockchain.modifiers.Directive.DTypeId
import encry.blockchain.modifiers.boxes.{AssetBox, EncryBaseBox, EncryProposition}
import encry.utils.Utils
import io.circe.{Decoder, Encoder, HCursor}
import org.encryfoundation.common.modifiers.mempool.transaction.EncryAddress
import org.encryfoundation.common.modifiers.mempool.transaction.EncryAddress.Address
import org.encryfoundation.common.utils.Algos
import org.encryfoundation.common.utils.TaggedTypes.ADKey
import scorex.crypto.encode.Base16
import scorex.crypto.hash.Digest32

case class TransferDirective(address: Address,
                             amount: Long,
                             tokenIdOpt: Option[ADKey] = None) extends Directive {

  override val isValid: Boolean = amount > 0 && EncryAddress.resolveAddress(address).isSuccess

  override def toDBDirective: DBDirectiveGeneralizedClass = DBDirectiveGeneralizedClass(address, amount)

  override val typeId: DTypeId = TransferDirective.TypeId

  override def boxes(digest: Digest32, idx: Int): EncryBaseBox =
    AssetBox(EncryProposition.addressLocked(address),
      Utils.nonceFromDigest(digest ++ Ints.toByteArray(idx)), amount, tokenIdOpt)

  override def toDbVersion(txId: Array[Byte], numberInTx: Int): DirectiveDBVersion =
    DirectiveDBVersion(Base16.encode(txId), numberInTx, typeId, isValid, "", amount, address, tokenIdOpt.map(Base16.encode), "")

}

object TransferDirective {

  val TypeId: DTypeId = 1.toByte

  implicit val jsonDecoder: Decoder[TransferDirective] = (c: HCursor) => {
    for {
      address     <- c.downField("address").as[String]
      amount      <- c.downField("amount").as[Long]
      tokenIdOpt  <- c.downField("tokenId").as[Option[String]]
    } yield TransferDirective(
      address,
      amount,
      tokenIdOpt.flatMap(id => Algos.decode(id).map(ADKey @@ _).toOption)
    )
  }

  implicit val jsonEncoder: Encoder[TransferDirective] = (d: TransferDirective) => Map(
    "typeId" -> d.typeId.asJson,
    "address" -> d.address.toString.asJson,
    "amount" -> d.amount.asJson,
    "tokenId" -> d.tokenIdOpt.map(id => Algos.encode(id)).asJson
  ).asJson
}