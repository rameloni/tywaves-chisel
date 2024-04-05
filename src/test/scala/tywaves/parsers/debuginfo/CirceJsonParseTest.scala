package tywaves.parsers.debuginfo

import org.scalatest.flatspec.AnyFlatSpec
import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser._
import io.circe.syntax.EncoderOps

// https://www.baeldung.com/scala/circe-json
class CirceJsonParseTest extends AnyFlatSpec {

  behavior of "CirceJsonParseTest"

  case class NestedObject(arrayField: List[Int], name: String = "John")
  case class HGLDD(name: String = "John", age: String = "30")
  case class CirceExample(
      textField:    String,
      numericField: Int,
      booleanField: Boolean,
      nestedObject: NestedObject,
      myField:      HGLDD,
  )

  val jsonString =
    """
      |{
      | "textField": "textContent",
      | "numericField": 123,
      | "booleanField": true,
      | "nestedObject": {
      |   "arrayField": [1, 2, 3],
      |   "name": "John"
      | },
      | "myField": {
      |   "name": "John",
      |   "age": "30"
      |  }
      |}
      |""".stripMargin

  it should "use circe with manual conversion" in {

    val parseResult: Either[ParsingFailure, Json] = parse(jsonString)

    parseResult match {
      case Left(parsingError) =>
        throw new IllegalArgumentException(s"Invalid JSON object: ${parsingError.message}")
      case Right(json) =>
        // Success json is accessible
        val numbers = json \\ "numericField"
        val firstNumber: Option[Option[JsonNumber]] =
          numbers.collectFirst { case field => field.asNumber }
        val singleOption: Option[Int] = firstNumber.flatten.flatMap(_.toInt)

    }

  }

  it should "use circe with case classes" in {
    // Decoded: json -> case class
    implicit val n:             Decoder[HGLDD]        = deriveDecoder[HGLDD]
    implicit val nestedDecoder: Decoder[NestedObject] = deriveDecoder[NestedObject]
    implicit val jsonDecoder:   Decoder[CirceExample] = deriveDecoder[CirceExample]

    val decoded = decode[CirceExample](jsonString)
//    assert(decoded == Right(CirceExample("textContent", 123, booleanField = true, NestedObject(List(1, 2, 3)))))

    // Encoded: case class -> json
    implicit val nestedEncoder:  Encoder[NestedObject] = deriveEncoder[NestedObject]
    implicit val myFieldEncoder: Encoder[HGLDD]        = deriveEncoder[HGLDD]
    implicit val jsonEncoder:    Encoder[CirceExample] = deriveEncoder[CirceExample]
    decoded match {
      case Right(decodedJson) =>
        val jsonObject: Json = decodedJson.asJson
        val newJsonString =
          parser.parse(jsonObject.spaces2)
        assert(newJsonString == parser.parse(jsonString))

    }
  }

  it should "use circe auto decoders" in {
    import io.circe.generic.auto._
    // Decoded: json -> case class

    val decoded = decode[CirceExample](jsonString)
//    assert(decoded == Right(CirceExample("textContent", 123, booleanField = true, NestedObject(List(1, 2, 3)))))

    // Encoded: case class -> json
    decoded match {
      case Right(decodedJson) =>
        val jsonObject: Json = decodedJson.asJson
        val newJsonString =
          parser.parse(jsonObject.spaces2)
        assert(newJsonString == parser.parse(jsonString))

    }
  }

}
