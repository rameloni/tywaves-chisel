package tywaves.hglddparser

import chisel3.RawModule
import io.circe.generic.auto._
import tywaves.circuitmapper.{Direction, ElId, HardwareType, Name, Type}
import tywaves.hglddparser
import tywaves.utils.UniqueHashMap

import scala.io.Source
case class VerilogSignals(names: Seq[String])
class DebugIRParser {

  lazy val modules        = new UniqueHashMap[ElId, Name]()
  lazy val ports          = new UniqueHashMap[ElId, (Name, Direction, Type /*, chiselIR.Port*/ )]()
  lazy val flattenedPorts = new UniqueHashMap[ElId, (Name, Direction, HardwareType, Type)]()
  lazy val allElements    = new UniqueHashMap[ElId, (Name, Direction, Type)]()
  lazy val signals        = new UniqueHashMap[ElId, (Name, Direction, HardwareType, Type, VerilogSignals)]()

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
   *   the `hdl_file` specified in [[hglddparser.HglddHeader.file_info]]) and to
   *   output working files of the parser
   * @param ddFilePath
   *   the input file to parse
   */
  def parse(workingDir: String, ddFilePath: String): Unit = {
    val hgldd = parseFile(ddFilePath)
    println("DebugIRParser: parse. hgldd: " + hgldd)
    val (fileInfo, hdlFileActualIndex) = (hgldd.HGLDD.file_info, hgldd.HGLDD.hdl_file_index - 1)
    val (hglFilePath, hdlFilePath)     = (fileInfo.head, workingDir + "/" + fileInfo(hdlFileActualIndex))

    fileInfo.map(f => f.replaceAll("\\.\\./", ""))

    hgldd.objects.foreach(
      parseObject(
        fileInfo.map(f => f.replaceAll("\\.\\./", "")), // Use the same of the other parsers TODO: needs better version
        _,
      )
    )
    println("======================")
    println("Modules")
    modules.foreach(println)
    println("======================")
    println("Flattened ports")
    flattenedPorts.foreach(println)
    println("======================")
    println("All elements")
    allElements.foreach(println)
    println("======================")
    println("Module Signals")
    signals.foreach(println)

  }

  /** Parse an object from the HGLDD representation */
  private def parseObject(fileInfo: Seq[String], hglddObject: HglddObject): Unit = {
    // Step 1: Create the ElId
//    val elId = createId(fileInfo, hglddObject.hgl_loc, hglddObject.obj_name)

    val scope =
      hglddObject.obj_name.lastIndexOf("_") match { case -1 => "root"; case i => hglddObject.obj_name.substring(0, i) }
    // Drop the scope from the object name
    val obj_name =
      scope match { case "root" => hglddObject.obj_name; case _ => hglddObject.obj_name.substring(scope.length + 1) }
    val elId = createId(fileInfo, hglddObject.hgl_loc, obj_name)

    // Step 2: Parse the kind of the object
    hglddObject.kind match {
      case s @ "struct" =>
        allElements.put(elId, (Name(obj_name, scope), Direction("Unknown"), Type(s)))
      case "module" =>
        modules.put(elId, Name(obj_name, scope))
        hglddObject.port_vars.foreach(parsePortVarFromModule(fileInfo, _, hglddObject.obj_name))
      case a =>
        println(s"Kind: $a not implemented")
        ???
    }

  }

  /**
   * Parse a portVar from a module. It must be used when [[HglddObject.kind]] is
   * "module".
   *
   * It collects all the [[Value.sig_name]]s from the value and add them to
   * [[signals]].
   */
  private def parsePortVarFromModule(fileInfo: Seq[String], portVar: PortVar, scope: String): Unit = {
    val elId  = createId(fileInfo, portVar.hgl_loc, portVar.var_name)
    val name  = Name(portVar.var_name, scope)
    val dir   = Direction("Unknown")
    val typ   = Type(portVar.type_name)
    val hwTyp = HardwareType(portVar.type_name)

    val sigNames = portVar.value match {
      case None        => VerilogSignals(Seq(portVar.var_name))
      case Some(value) => VerilogSignals(collectSigNames(value))

    }
    signals.put(elId, (name, dir, hwTyp, typ, sigNames))
  }

  /**
   * Collect all the [[Value.sig_name]]s from a [[Value]] and return them as a
   * sequence of strings.
   */
  private def collectSigNames(value: Value): Seq[String] = {
    var names = Seq[String]()
    value.sig_name match {
      case Some(sigName) => names = names :+ sigName
      case None          => ()
    }
    value.operands match {
      case Some(operands) =>
        operands.foreach { op =>
          names = names ++ collectSigNames(op)
        }
      case None => ()
    }
    names
  }

  /** Create an id for the debugIR parser */
  def createId(source: Seq[String], loc: HglLocation, name: String): ElId =
    ElId(source(loc.file - 1), loc.begin_line, loc.begin_column, name)

}
