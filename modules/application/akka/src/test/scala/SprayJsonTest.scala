import java.time.ZonedDateTime

import SpraySyntaxSupport.JsonKey
import com.fasterxml.jackson.annotation.JacksonInject.Value
import jp.lanscopean.common.util.DateTimeUtil
import org.scalatest.FunSuite
import spray.json.{
  DefaultJsonProtocol,
  JsBoolean,
  JsNumber,
  JsObject,
  JsString,
  JsValue,
  RootJsonReader
}

object SpraySyntaxSupport {

  type JsonKey = String

  object Write {
    type JsonItem = (JsonKey, JsValue)

    private val jsValueOf = (value: Any) =>
      value match {
        case v: String     => JsString(v)
        case v: Int        => JsNumber(v)
        case v: Long       => JsNumber(v)
        case v: BigDecimal => JsNumber(v)
        case v: Boolean    => JsBoolean(v)
        case _ =>
          throw new IllegalArgumentException(
            s"Could not convert to JsValue. $value")
    }

    def jsObject(jsItems: Option[JsonItem]*): JsObject =
      JsObject(jsItems.flatten.toMap)

    def flatten(
        jsItems: Option[Seq[Option[JsonItem]]]*): Seq[Option[JsonItem]] =
      jsItems.flatten.flatten

    def string(key: JsonKey, value: String): Some[(JsonKey, JsString)] =
      Some(key -> JsString(value))

    def int(key: JsonKey, value: Int): Some[(JsonKey, JsNumber)] =
      Some(key -> JsNumber(value))

    def long(key: JsonKey, value: Long): Some[(JsonKey, JsNumber)] =
      Some(key -> JsNumber(value))

    def decimal(key: JsonKey, value: BigDecimal): Some[(JsonKey, JsNumber)] =
      Some(key -> JsNumber(value))

    def boolean(key: JsonKey, value: Boolean): Some[(JsonKey, JsBoolean)] =
      Some(key -> JsBoolean(value))

    def datetime(key: JsonKey,
                 value: ZonedDateTime): Some[(JsonKey, JsString)] =
      string(key, DateTimeUtil.formatISO(value))

    def as[V](key: JsonKey, value: V)(f: V => JsValue): Some[JsonItem] =
      Some(key -> f(value))

    def optString(key: JsonKey,
                  value: Option[String]): Option[(JsonKey, JsString)] =
      value.map(key -> JsString(_))

    def optInt(key: JsonKey, value: Option[Int]): Option[(JsonKey, JsNumber)] =
      value.map(key -> JsNumber(_))

    def optLong(key: JsonKey,
                value: Option[Long]): Option[(JsonKey, JsNumber)] =
      value.map(key -> JsNumber(_))

    def optDecimal(key: JsonKey,
                   value: Option[BigDecimal]): Option[(JsonKey, JsNumber)] =
      value.map(key -> JsNumber(_))

    def optBoolean(key: JsonKey,
                   value: Option[Boolean]): Option[(JsonKey, JsBoolean)] =
      value.map(key -> JsBoolean(_))
  }

  object Read extends DefaultJsonProtocol {
    def string(key: JsonKey)(implicit json: JsValue): String =
      fromField[String](json, key)

    def int(key: JsonKey)(implicit json: JsValue): Int =
      fromField[Int](json, key)

    def long(key: JsonKey)(implicit json: JsValue): Long =
      fromField[Long](json, key)

    def boolean(key: JsonKey)(implicit json: JsValue): Boolean =
      fromField[Boolean](json, key)

    def datetime(key: JsonKey)(implicit json: JsValue): ZonedDateTime =
      DateTimeUtil.parseISO(fromField[String](json, key))

    def optString(key: JsonKey)(implicit json: JsValue): Option[String] =
      fromField[Option[String]](json, key)
  }

}

case class LastDeliveryHistoryQueryModel(
    companyId: String,
    deviceId: String,
    managedContentId: String,
    actionType: String,
    deliveryState: String,
    deliveredAt: ZonedDateTime,
    requestedRecipeId: Option[String],
    deliveryMethod: String
)

class SprayJsonTest extends FunSuite {

  import SpraySyntaxSupport.Read._

  trait Fixture {
    private implicit val queryModelJsonReader =
      new RootJsonReader[LastDeliveryHistoryQueryModel] {
        override def read(json: JsValue): LastDeliveryHistoryQueryModel = {
          implicit val _json = json

          LastDeliveryHistoryQueryModel(
            companyId = string("company_id"),
            deviceId = string("device_id"),
            managedContentId = string("managedcontent_id"),
            actionType = string("action_type"),
            deliveryState = string("delivery_state"),
            deliveredAt = datetime("delivered_at"),
            requestedRecipeId = optString("requested_recipe_id"),
            deliveryMethod = string("delivery_method")
          )
        }
      }

    def map2OptString(doc: Map[String, Any],
                      attribute: String): Option[String] =
      doc.get(attribute).map(_.toString)

    def map2OptString2(doc: Map[String, Any], attribute: String) =
      doc.get(attribute) match {
        case Some(x) if x != null => Some(x.toString)
        case Some(x)              => None
        case None                 => None
      }

    def isNull(v: Any) = Option(v) match {
      case Some(x) =>
        if (x != Some(null)) println(s"$x nullじゃない何か")
        else println("Some(null)だよ")
      case None => println("nullだ！")
    }
  }

  test("sprayjsonのテスト") {
    new Fixture {
      println("********************************************")

      val AttrRequestedRecipeId = "requested_recipe_id"
      //      isNull(Some(null))
      val map1 = Map(AttrRequestedRecipeId -> "hoge")
      val map2 = Map(AttrRequestedRecipeId -> null)
      println(map2OptString2(map1, AttrRequestedRecipeId))
      println(map2OptString2(map2, AttrRequestedRecipeId))

      println("********************************************")
    }
  }

  test("Arrayのテスト") {
    println("********************************************")

    val array1 = Array(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    var array2: Array[Int] = Array.empty
    println(array1.last)
    println(array2.nonEmpty)

    println("********************************************")
  }
}
