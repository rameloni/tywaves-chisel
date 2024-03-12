package tywaves.hglddparser

import chisel3.RawModule
import io.circe.generic.auto._
import tywaves.hglddparser

import scala.io.Source

class DebugIRParser {

  def parseString(hglddString: String): hglddparser.HglddTopInterface =
    io.circe.parser.decode[hglddparser.HglddTopInterface](hglddString) match {
      case Left(parsingError) => throw new IllegalArgumentException(s"Invalid JSON object: $parsingError")
      case Right(ddObj)       => ddObj
    }

  def parse[T <: RawModule](generateModule: () => T, workingDir: String, ddFilePath: String): Unit =
    parseFile(workingDir, ddFilePath)

  def parseFile(workingDir: String, ddFilePath: String): hglddparser.HglddTopInterface = {
    // Imp: for the future the "file_info" property is a relative path from the working directory
    println("DebugIRParser: parse. ddFilePath: " + ddFilePath)

    // Open the file HglDD file and convert it to a string
    val sourceHgldd = Source.fromFile(ddFilePath)
    val hglddString = sourceHgldd.mkString
    sourceHgldd.close()
    parseString(hglddString)
  }
}
