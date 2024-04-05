package tywaves.parsers.debuginfo

import io.circe.generic.semiauto.deriveDecoder
import org.scalatest.flatspec.AnyFlatSpec
import upickle.default._

class JsonParseTest extends AnyFlatSpec {
  behavior of "JsonParseTest"

  case class MyJson(name: String, age: Int, car: String)

  it should "parse a json string" in {
    val jsonString = """{"name": "John", "age": 30, "car": null}"""

    implicit val ownerRw: ReadWriter[MyJson] = macroRW
    val jsonFromObject = read[MyJson](jsonString)
    assert(jsonFromObject == MyJson("John", 30, null))
  }

  case class ComplexJson(name: String, age: Int, car: String, children: List[MyJson])
  it should "parse a more complex json string" in {
    val jsonString =
      """{"name": "John", "age": 30, "car": null,
        "children": [
          {"name": "Alice", "age": 5, "car": null},
          {"name": "Bob", "age": 7, "car": "Ferrari"}
          ]}"""
    implicit val ownerRw:   ReadWriter[MyJson]      = macroRW
    implicit val complexRw: ReadWriter[ComplexJson] = macroRW
    val jsonFromObject = read[ComplexJson](jsonString)
    assert(jsonFromObject == ComplexJson(
      "John",
      30,
      null,
      List(MyJson("Alice", 5, null), MyJson("Bob", 7, "Ferrari")),
    ))
  }

}
