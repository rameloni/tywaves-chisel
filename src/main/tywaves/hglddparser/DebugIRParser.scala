package tywaves.hglddparser

import chisel3.RawModule
import io.circe.generic.auto._
import tywaves.circuitmapper.{Direction, ElId, HardwareType, Name, Type, VerilogSignals}
import tywaves.hglddparser
import tywaves.utils.UniqueHashMap

import scala.io.Source
class DebugIRParser(val workingDir: String, ddFilePath: String) {

  def this() = this("workingDir", "ddFilePath")

  val hierarchySeparator = "_"

  lazy val modules        = new UniqueHashMap[ElId, Name]()
  lazy val ports          = new UniqueHashMap[ElId, (Name, Direction, Type /*, chiselIR.Port*/ )]()
  lazy val flattenedPorts = new UniqueHashMap[ElId, (Name, Direction, HardwareType, Type)]()
  lazy val allElements    = new UniqueHashMap[ElId, (Name, Direction, Type)]()
  lazy val signals        = new UniqueHashMap[ElId, (Name, Direction, HardwareType, Type, VerilogSignals)]()

  private var nameUpdate = false
  def dump(fileDump: String): Unit = {
    if (!nameUpdate) throw new Exception("The name of the file has been updated. Please use the new name.")
    modules.dumpFile(fileDump, "Modules:", append = false)
    ports.dumpFile(fileDump, "Ports:")
    flattenedPorts.dumpFile(fileDump, "Flattened Ports:")
    allElements.dumpFile(fileDump, "Internal Elements:")
    signals.dumpFile(fileDump, "Signals:")

  }

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

    // Open the file HglDD file and convert it to a string
    val sourceHgldd = Source.fromFile(ddFilePath)
    val hglddString = sourceHgldd.mkString
    sourceHgldd.close()
    parseString(hglddString)
  }

  /**
   * Parse the file in `hgldd` format in [[ddFilePath]] and fill [[modules]],
   * [[ports]], [[flattenedPorts]], [[allElements]], and [[signals]].
   *
   * It also updates the [[signals]] with the actual system verilog name.
   */
  def parse(): Unit = {
    val hgldd = parseFile(ddFilePath)
    println("DebugIRParser: parse. hgldd: " + hgldd)
    val (fileInfo, hdlFileActualIndex) = (hgldd.HGLDD.file_info, hgldd.HGLDD.hdl_file_index - 1)
    val (hglFilePath, hdlFilePath)     = (fileInfo.head, workingDir + "/" + fileInfo(hdlFileActualIndex))

    fileInfo.map(f => f.replaceAll("\\.\\./", ""))

    hgldd.objects.foreach(
      parseObject(
        fileInfo.map(f =>
          f.replaceAll("\\.\\./", "")
        ), // Use the same of the other parsers (ChiselIR and FIRRTL IR) TODO: needs better version
        _,
      )
    )

    updateSignals()
  }

  /** Parse an object from the HGLDD representation */
  private def parseObject(fileInfo: Seq[String], hglddObject: HglddObject): Unit = {

    val scope =
      hglddObject.obj_name.lastIndexOf("_") match { case -1 => "root"; case i => hglddObject.obj_name.substring(0, i) }
    // Drop the scope from the object name
    val obj_name =
      scope match { case "root" => hglddObject.obj_name; case _ => hglddObject.obj_name.substring(scope.length + 1) }
    val elId =
      createId(fileInfo, hglddObject.hgl_loc, obj_name)
    val parentModule =
      hglddObject.obj_name.lastIndexOf("_") match { case -1 => "root"; case i => hglddObject.obj_name.substring(0, i) }
    // Parse the kind of the object
    hglddObject.kind match {
      case s @ "struct" =>
        allElements.put(elId, (Name(obj_name, scope, parentModule), Direction("Unknown"), Type(s)))
        hglddObject.port_vars.foreach(parsePortVarFromModule(fileInfo, _, hglddObject.obj_name, parentModule))
      case "module" =>
        modules.put(elId, Name(obj_name, scope, parentModule))
        hglddObject.port_vars.foreach(parsePortVarFromModule(fileInfo, _, hglddObject.obj_name, hglddObject.obj_name))
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
  private def parsePortVarFromModule(
      fileInfo:     Seq[String],
      portVar:      PortVar,
      scope:        String,
      parentModule: String,
  ): Unit = {
    val elId = createId(fileInfo, portVar.hgl_loc, portVar.var_name)
    val name = Name(portVar.var_name, scope, parentModule)
    val dir  = Direction("Unknown")
    val typ  = Type(portVar.type_name)
    val hwTyp = HardwareType(
      portVar.type_name,
      Some(portVar.packed_range.map(sizes => sizes.head - sizes.last + 1).getOrElse(1)),
    )

    val sigNames =
      portVar.value match {
        case None        => VerilogSignals(Seq(portVar.var_name))
        case Some(value) => VerilogSignals(collectSigNames(value))
      }
//    val sigNames = VerilogSignals(Seq(portVar.var_name))
    signals.put(elId, (name, dir, hwTyp, typ, sigNames))
  }

  /**
   * Update the [[signals]] in order to associate their actual system verilog
   * name. Some signals are part of a complex type, for example a bundle/struct.
   *
   * The [[signals]] list contains all the signals in the HGLDD file associated
   * to their system verilog name which should be contained in the
   * [[Value.sig_name]] variable. However, when they are children of a complex
   * type (no "logic", "wire", or "register") their [[Value.sig_name]] does not
   * match the actual system verilog name.
   *
   * A signal that is complex type has a [[Type.name]] that is not "logic", and
   * it contains a list of [[Value.sig_name]]s which should be the actual system
   * verilog name. This function associates these names to their respective
   * signals in [[signals]].
   *
   * It updates that name by doing the following steps:
   *
   *   1. Find all the complex types elements (i.e. not logic, wire, or
   *      register) and assign the `scope` as its type.
   *   1. Get the list of actual system verilog names (children signals of
   *      `scope`) from the [[Value.sig_name]].
   *   1. From [[signals]] search those signals within the `scope` and update
   *      with their new name.
   */
  private def updateSignals(): Unit = {
    val (logic, wire, register) = ("logic", "wire", "register")
    // Get all the types and their sig_names: type = scope, sig_names = new names for the children of the scope
    signals.map { case (elId, (name, _, _, typ, sigNames)) => (typ.name, sigNames.names, name.name, elId) }
      .filter(typ => typ._1 != logic && typ._1 != wire && typ._1 != register) // Filter the complex types
      .foreach {
        case (scope, newNames, parentName, parentElId) =>
          // Find all the signals that are within the scope to be updated
          signals.filter { case (_, (name, _, _, _, _)) => name.scope == scope }
            .zip(newNames).foreach { // Update the signals with the new names (the sig_names is overwritten)
              case ((_elId, (name, dir, hwType, typ, _)), _newSigName) =>
                val newSigName = parentName + hierarchySeparator + name.name
                val elId       = ElId(parentElId.source, parentElId.row, parentElId.col, _elId.name)
                // Now the sig_names contains verilog names
                signals.putOrReplace(elId, (name, dir, hwType, typ, VerilogSignals(Seq(newSigName)))) match {
                  case None => // It is a new value, that means the something changed
                    Console.err.println(s"Info: The signal $elId was not found in the signals list.")
                    Console.err.println("Removed: " + signals.remove(_elId)) // Remove the old value
                  case Some(_) => ()
                }
            }
      } // end foreach
    nameUpdate = true
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

    if (value.opcode.getOrElse("") == "'{")
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
