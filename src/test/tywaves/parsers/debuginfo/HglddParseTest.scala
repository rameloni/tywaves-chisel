package tywaves.parsers.debuginfo

import foo.Foo
import org.scalatest.flatspec.AnyFlatSpec
import tywaves.circuitmapper.GenerateHgldd
import tywaves.hglddparser.DebugIRParser

import java.nio.file.Files

class HglddParseTest extends AnyFlatSpec {
  behavior of "HglddParseTest"
  private val tmpDir  = Files.createTempDirectory("hgldd-parser-test")
  private val tmpFile = tmpDir.resolve("hgldd.json")
  private val hlddString =
    """ {
      |   "HGLDD": {
      |     "version": "1.0",
      |     "file_info": [
      |       "../../../../../Documents/master-delft/thesis/_tywaves/TyWaves-samples/src/test/foo/Foo.scala",
      |       "Foo.sv"
      |    ],
      |    "hdl_file_index": 2
      |   },
      |   "objects": [
      |     {
      |       "hgl_loc": {
      |         "begin_column": 16,
      |         "begin_line": 40,
      |         "end_column": 16,
      |         "end_line": 40,
      |         "file": 1
      |       },
      |       "kind": "struct",
      |       "obj_name": "Foo_s_bundle",
      |       "port_vars": [
      |        {
      |          "hgl_loc": {
      |            "begin_column": 16,
      |            "begin_line": 40,
      |            "end_column": 16,
      |            "end_line": 40,
      |            "file": 1
      |          },
      |          "type_name": "logic",
      |          "var_name": "z"
      |        }
      |      ]
      |     },
      |     {
      |      "hgl_loc": {
      |        "begin_column": 16,
      |        "begin_line": 40,
      |        "end_column": 16,
      |        "end_line": 40,
      |        "file": 1
      |      },
      |      "kind": "struct",
      |      "obj_name": "Foo_s",
      |      "port_vars": [
      |        {
      |          "hgl_loc": {
      |            "begin_column": 16,
      |            "begin_line": 40,
      |            "end_column": 16,
      |            "end_line": 40,
      |            "file": 1
      |          },
      |          "packed_range": [
      |            7,
      |            0
      |          ],
      |          "type_name": "logic",
      |          "var_name": "a"
      |        },
      |        {
      |          "hgl_loc": {
      |            "begin_column": 16,
      |            "begin_line": 40,
      |            "end_column": 16,
      |            "end_line": 40,
      |            "file": 1
      |          },
      |          "packed_range": [
      |            7,
      |            0
      |          ],
      |          "type_name": "logic",
      |          "var_name": "b"
      |        },
      |        {
      |          "hgl_loc": {
      |            "begin_column": 16,
      |            "begin_line": 40,
      |            "end_column": 16,
      |            "end_line": 40,
      |            "file": 1
      |          },
      |          "packed_range": [
      |            7,
      |            0
      |          ],
      |          "type_name": "logic",
      |          "var_name": "c"
      |        },
      |        {
      |          "hgl_loc": {
      |            "begin_column": 16,
      |            "begin_line": 40,
      |            "end_column": 16,
      |            "end_line": 40,
      |            "file": 1
      |          },
      |          "type_name": "Foo_s_bundle",
      |          "var_name": "bundle"
      |        }
      |      ]
      |    }
      |
      |   ]
      | }
      |""".stripMargin

  // write the string to a file
  println(tmpFile.toString)
  Files.writeString(tmpFile, hlddString)

  it should "parse a Hgldd string" in {
    val debugParser = new DebugIRParser
    val parsedObj   = debugParser.parseString(hlddString)
    println(parsedObj)
  }

  it should "parse a Hgldd file" in {
    val debugParser = new DebugIRParser
    println(debugParser.parseFile("workingDir", tmpFile.toString))
  }

  it should "parse an actual Hgldd file" in {
    // Generate the Hgldd file
    val outFile     = GenerateHgldd(() => new Foo, tmpDir.toString) + "/Foo.dd"
    val debugParser = new DebugIRParser
    println(debugParser.parseFile("workingDir", outFile))
  }

  it should "parse all infos" in {
    import scala.io.Source
    import io.circe.syntax.EncoderOps
    import io.circe.generic.auto._
    import io.circe.parser

    // Generate the Hgldd file
    val ddFile      = GenerateHgldd(() => new Foo, tmpDir.toString) + "/Foo.dd"
    val debugParser = new DebugIRParser
    val parsedObj   = debugParser.parseFile("workingDir", ddFile)

    // Read the outFile
    val sourceHgldd = {
      val inString = Source.fromFile(ddFile)
      val out = parser.parse(inString.mkString)
        .getOrElse(throw new Exception("Could not parse the file"))
        .deepDropNullValues.spaces2SortKeys
      inString.close()
      out
    }
    val outputHgldd = parsedObj.asJson
      .deepDropNullValues.spaces2SortKeys

    assert(parser.parse(sourceHgldd) ==
      parser.parse(outputHgldd))

  }

}
