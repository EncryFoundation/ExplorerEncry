package encry.blockchain.modifiers.boxes

import encry.blockchain.modifiers.boxes.Box.Amount
import encry.settings.Constants
import io.circe.{Decoder, Encoder, HCursor}
import io.circe.syntax._
import org.encryfoundation.common.Algos
import org.encryfoundation.prismlang.core.Types
import org.encryfoundation.prismlang.core.wrapped.{PObject, PValue}

case class AssetBox(override val proposition: EncryProposition,
                    override val nonce: Long,
                    override val amount: Amount,
                    tokenIdOpt: Option[Array[Byte]] = None)
  extends EncryBox[EncryProposition] with MonetaryBox {

  override val typeId: Byte = AssetBox.TypeId


  override val tpe: Types.Product = Types.AssetBox

  override def asVal: PValue = PValue(asPrism, Types.AssetBox)

  override def asPrism: PObject =
    PObject(baseFields ++ Map(
      "amount" -> PValue(amount, Types.PInt),
      "tokenId" -> PValue(tokenIdOpt.getOrElse(Constants.IntrinsicTokenId), Types.PCollection.ofByte)
    ), tpe)

  override def toDBBoxes: DBBoxGeneralizedClass =
    DBBoxGeneralizedClass(Algos.encode(id), Algos.encode(tokenIdOpt.getOrElse(Constants.IntrinsicTokenId)), Algos.encode(proposition.contractHash),nonce = nonce)
}

object AssetBox {

  val TypeId: Byte = 1.toByte

  implicit val jsonDecoder: Decoder[AssetBox] = (c: HCursor) => {
    for {
      proposition   <- c.downField("proposition").as[EncryProposition]
      nonce         <- c.downField("nonce").as[Long]
      amount        <- c.downField("value").as[Long]
      tokenIdOpt    <- c.downField("tokenId").as[Option[String]]
    } yield AssetBox(
      proposition,
      nonce,
      amount,
      tokenIdOpt.map(str => Algos.decode(str).getOrElse(Array.emptyByteArray))
    )
  }

  implicit val jsonEncoder: Encoder[AssetBox] = (bx: AssetBox) => Map(
    "type" -> TypeId.asJson,
    "id" -> Algos.encode(bx.id).asJson,
    "proposition" -> bx.proposition.asJson,
    "nonce" -> bx.nonce.asJson,
    "value" -> bx.amount.asJson,
    "tokenId" -> bx.tokenIdOpt.map(id => Algos.encode(id)).asJson
  ).asJson
}
