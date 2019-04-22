package encry.blockchain.modifiers

import encry.blockchain.modifiers.Directive.DTypeId
import encry.blockchain.modifiers.boxes.EncryBaseBox
import io.circe.{Decoder, DecodingFailure, Encoder}
import org.encryfoundation.common.transaction.EncryAddress.Address
import cats.implicits._
import encry.utils.CoreTaggedTypes.ModifierId
import org.encryfoundation.common.utils.TaggedTypes.ADKey
import scorex.crypto.encode.Base16
import scorex.crypto.hash.Digest32

trait Directive {

  val isValid: Boolean

  val typeId: DTypeId

  def toDBDirective: DBDirectiveGeneralizedClass

  def boxes(digest: Digest32, idx: Int): EncryBaseBox

  def toDbVersion(txId: Array[Byte], numberInTx: Int): DirectiveDBVersion

}

object Directive {

  type DTypeId = Byte

  implicit val jsonDecoder: Decoder[Directive] = {
    Decoder.instance { c =>
      c.downField("typeId").as[DTypeId] match {
        case Right(s) => s match {
          case TransferDirective.TypeId => TransferDirective.jsonDecoder(c)
          case AssetIssuingDirective.TypeId => AssetIssuingDirective.jsonDecoder(c)
          case ScriptedAssetDirective.TypeId => ScriptedAssetDirective.jsonDecoder(c)
          case DataDirective.TypeId => DataDirective.jsonDecoder(c)
          case _ => Left(DecodingFailure("Incorrect directive typeID", c.history))
        }
        case Left(_) => Left(DecodingFailure("None typeId", c.history))
      }
    }
  }
  implicit val jsonEncoder: Encoder[Directive] = {
    case td: TransferDirective => TransferDirective.jsonEncoder(td)
    case aid: AssetIssuingDirective => AssetIssuingDirective.jsonEncoder(aid)
    case sad: ScriptedAssetDirective => ScriptedAssetDirective.jsonEncoder(sad)
    case dad: DataDirective => DataDirective.jsonEncoder(dad)
    case _ => throw new Exception("Incorrect directive type")
  }
}

case class DirectiveDBVersion(txId: String,
                              numberInTx: Int,
                              dTypeId: DTypeId,
                              isValid: Boolean,
                              contractHash: String,
                              amount: Long,
                              address: String,
                              tokenIdOpt: Option[String],
                              data: String)  {

  def toDirective: Option[Directive] =
    dTypeId match {
      case AssetIssuingDirective.TypeId => Base16.decode(contractHash).map(AssetIssuingDirective(_, amount)).toOption
      case TransferDirective.TypeId => tokenIdOpt match {
        case Some(tokenIdStr) => Base16.decode(tokenIdStr)
          .map(tokenId => TransferDirective(address, amount, Some(ADKey @@ tokenId))).toOption
        case None => Some(TransferDirective(address, amount, None))
      }
      case ScriptedAssetDirective.TypeId => tokenIdOpt match {
        case Some(tokenIdStr) => (Base16.decode(tokenIdStr), Base16.decode(contractHash)).mapN {
          case (tokenId, contractHashDec) => ScriptedAssetDirective(contractHashDec, amount, Some(ADKey @@ tokenId))
        }.toOption
        case None => Base16.decode(contractHash).map(ScriptedAssetDirective(_, amount, None)).toOption
      }
      case DataDirective.TypeId => (Base16.decode(contractHash), Base16.decode(data)).mapN {
        case (contractHashDec, dataDec) => DataDirective(contractHashDec, dataDec)
      }.toOption
      case _ => None
    }
}

case class DBDirectiveGeneralizedClass(address: Address = "", amount: Long = 0L)