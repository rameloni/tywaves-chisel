package tywaves.hglddparser

import chisel3.RawModule
import io.circe.generic.auto._
import tywaves.hglddparser

import scala.io.Source

class DebugIRParser {

  /**
   * Parse a given string in `hgldd` format.
   *
   * @param hglddString
   *   the input hgldd format string
   * @return
   *   [[hglddparser.HglddTopInterface]] case class
   */
  def parseString(hglddString: String): hglddparser.HglddTopInterface =
    io.circe.parser.decode[hglddparser.HglddTopInterface](hglddString) match {
      case Left(parsingError) => throw new IllegalArgumentException(s"Invalid JSON object: $parsingError")
      case Right(ddObj)       => ddObj
    }

  /**
   * Parse a given file in `hgldd` format.
   * @param ddFilePath
   *   the input file path
   * @return
   */
  def parseFile(ddFilePath: String): hglddparser.HglddTopInterface = {
    // Imp: for the future the "file_info" property is a relative path from the working directory
    println("DebugIRParser: parse. ddFilePath: " + ddFilePath)

    // Open the file HglDD file and convert it to a string
    val sourceHgldd = Source.fromFile(ddFilePath)
    val hglddString = sourceHgldd.mkString
    sourceHgldd.close()
    parseString(hglddString)
  }

  /**
   * Parse a given file in `hgldd` format.
   * @param workingDir
   *   the working directory used to resolve properties in the hgldd file (i.e.
   *   [[hglddparser.HglddHeader.file_info]]) and to output working files of the
   *   parser
   * @param ddFilePath
   *   the input file to parse \@tparam
   */
  def parse(workingDir: String, ddFilePath: String): Unit =
    parseFile(ddFilePath)

}
