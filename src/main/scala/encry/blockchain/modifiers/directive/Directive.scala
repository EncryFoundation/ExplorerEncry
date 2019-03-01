package encry.blockchain.modifiers.directive

import encry.blockchain.modifiers.directive.Directive.DTypeId
import io.circe.{Decoder, DecodingFailure, Encoder}

trait Directive {

  val typeId: DTypeId
}

object Directive {
  type DTypeId = Byte

  implicit val jsonEncoder: Encoder[Directive] = {
    case td: TransferDirective       => TransferDirective.jsonEncoder(td)
    case aid: AssetIssuingDirective  => AssetIssuingDirective.jsonEncoder(aid)
    case sad: ScriptedAssetDirective => ScriptedAssetDirective.jsonEncoder(sad)
    case dad: DataDirective          => DataDirective.jsonEncoder(dad)
    case _ => throw new Exception("Incorrect directive type")
  }

  implicit val jsonDecoder: Decoder[Directive] = {
    Decoder.instance { c =>
      c.downField("typeId").as[DTypeId] match {
        case Right(s) => s match {
          case TransferDirective.TypeId      => TransferDirective.jsonDecoder(c)
          case AssetIssuingDirective.TypeId  => AssetIssuingDirective.jsonDecoder(c)
          case ScriptedAssetDirective.TypeId => ScriptedAssetDirective.jsonDecoder(c)
          case DataDirective.TypeId          => DataDirective.jsonDecoder(c)
          case _ => Left(DecodingFailure("Incorrect directive typeID", c.history))
        }
        case Left(_) => Left(DecodingFailure("None typeId", c.history))
      }
    }
  }
}