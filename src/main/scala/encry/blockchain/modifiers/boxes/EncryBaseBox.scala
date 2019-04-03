package encry.blockchain.modifiers.boxes

import com.google.common.primitives.Longs
import encry.blockchain.modifiers.boxes.EncryBox.BxTypeId
import io.circe.{Decoder, DecodingFailure, Encoder}
import org.encryfoundation.common.Algos
import org.encryfoundation.common.utils.TaggedTypes.ADKey
import org.encryfoundation.prismlang.core.{PConvertible, Types}
import org.encryfoundation.prismlang.core.wrapped.{PObject, PValue}

case class DBBoxGeneralizedClass(id: String = "",
                                 coinId: String = "",
                                 contractHash: String = "",
                                 data: String = "-",
                                 nonce: Long = 0L)

trait EncryBaseBox extends Box[EncryProposition] with PConvertible {

  val typeId: BxTypeId

  val nonce: Long

  override lazy val id: ADKey = ADKey @@ Algos.hash(Longs.toByteArray(nonce)).updated(0, typeId)

  def isAmountCarrying: Boolean = this.isInstanceOf[MonetaryBox]

  def toDBBoxes: DBBoxGeneralizedClass

  override val tpe: Types.Product = Types.EncryBox

  lazy val baseFields: Map[String, PValue] = Map(
    "contractHash" -> PValue(proposition.contractHash, Types.PCollection.ofByte),
    "typeId"       -> PValue(typeId.toLong, Types.PInt),
    "id"           -> PValue(id, Types.PCollection.ofByte)
  )

  def asPrism: PObject = PObject(baseFields, tpe)

}

object EncryBaseBox {

  implicit val jsonEncoder: Encoder[EncryBaseBox] = {
    case ab: AssetBox         => AssetBox.jsonEncoder(ab)
    case db: DataBox          => DataBox.jsonEncoder(db)
    case aib: TokenIssuingBox => TokenIssuingBox.jsonEncoder(aib)
  }

  implicit val jsonDecoder: Decoder[EncryBaseBox] = {
    Decoder.instance { c =>
      c.downField("type").as[BxTypeId] match {
        case Right(s) => s match {
          case AssetBox.TypeId        => AssetBox.jsonDecoder(c)
          case DataBox.TypeId         => DataBox.jsonDecoder(c)
          case TokenIssuingBox.TypeId => TokenIssuingBox.jsonDecoder(c)
          case _ => Left(DecodingFailure("Incorrect directive typeID", c.history))
        }
        case Left(_) => Left(DecodingFailure("None typeId", c.history))
      }
    }
  }
}
