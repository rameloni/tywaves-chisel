package chisel3.simulator
import chisel3.{Aggregate, Bundle, Clock, Data, Vec, AsyncReset, UInt, SInt, Bool}
import chisel3.experimental.{NoSourceInfo, SourceInfo, SourceLine}
import chisel3.internal.firrtl.{ir => chiselIR}
import firrtl.ir.AsyncResetType
class ChiselIRParser extends CircuitParser[chiselIR.Circuit] {
  // Collection of all modules in the circuit
  lazy val modules        = new UniqueHashMap[ElId, (Name, chiselIR.Component)]()
  lazy val ports          = new UniqueHashMap[ElId, (Name, Direction, Type, chiselIR.Port)]()
  lazy val flattenedPorts = new UniqueHashMap[ElId, (Name, Direction, HardwareType, Type)]()
  lazy val allElements    = new UniqueHashMap[ElId, (Name, Direction, Type)]()

  /** Parse a whole [[chiselIR.Circuit]] */
  override def parse(circuitChiselIR: chiselIR.Circuit): Unit =
    circuitChiselIR.components.foreach(parse)

  /** Parse a whole [[chiselIR.DefModule]] */
  def parse(chiselComponent: chiselIR.Component): Unit = {
    // Parse generic info and create an ID for the module
    val name = chiselComponent.name
    val elId = this.createId(SourceLine(name, 0, 0), Some(name))

    modules.put(elId, (Name(name, "root"), chiselComponent)) // Add the module and its name

    // Parse the internals of the module
    chiselComponent match {
      case chiselIR.DefModule(_, _, ports, body) =>
        ports.foreach(parse(name, _))
        // TODO: Parse the body:
        body.foreach(parse(name, _))
      case chiselIR.DefBlackBox(_, name, ports, topDir, params) =>
        println(s"DefBlackBox: name: $name, ports: $ports, topDir: $topDir, params: $params")

      case _ => throw new Exception(s"Failed to parse module $name. Unknown type.")
    }
  }

  /**
   * Parse a [[chiselIR.Port]].
   *
   * Imp: at ChiselIR abstraction level, direction is not specified in some
   * cases. So the firrtlIR will be needed to retrieve that in such a case. It
   * happens for example here:
   * {{{
   *    val io = IO(new Bundle {
   *      val a   = Input(Bool())
   *     })
   * }}}
   */
  def parse(scope: String, port: chiselIR.Port): Unit = {
    val portData: Data = port.id
    // Parse generic info and create an ID for the port
    val (name, info, dir) = (portData.toNamed.name, port.sourceInfo, port.dir)
    val elId              = this.createId(info, Some(name))

    ports.put(
      elId,
      (Name(name, scope), Direction(dir.toString), Type(portData.typeName), port),
    ) // Add the port and its name

    // Types from here: https://github.com/chipsalliance/chisel?tab=readme-ov-file#data-types-overview
    portData match {
      case agg: Aggregate =>
        // TODO: check this
        println(s"AggregateType: $agg")
        parse(elId, Name(name, scope), Direction(dir.toString), HardwareType("Port"), agg)
      case _ => parse(elId, Name(name, scope), Direction(dir.toString), HardwareType("Port"), portData)
    }
  }

  /**
   * Specialized function for AggregateTypes like Bundles and Vecs.
   *
   * More information on Chisel basic types: [Chisel's Data Types
   * Overview](https://github.com/chipsalliance/chisel?tab=readme-ov-file#data-types-overview)
   *
   * It unwraps Bundles and Vecs and executes specific parse functions of other
   * types.
   */
  def parse(elId: ElId, name: Name, dir: Direction, hwType: HardwareType, aggrType: Aggregate): Unit = {
    flattenedPorts.put(elId.addName(name.name), (name, dir, hwType, Type(aggrType.getClass.getName)))
    allElements.put(elId.addName(name.name), (name, dir, Type(aggrType.getClass.getName)))
    aggrType match {
      case b: Bundle =>
        b.elements.foreach { case (fieldName, dataType) =>
          parse(elId, Name(fieldName, name.name), dir, hwType, dataType)
          println(s"AggregateType: $aggrType, dir: $dir, hwType: $hwType, name: $name")
        }
      case v: Vec[Data] =>
        for (i <- 0 until v.length) {
          parse(elId, Name(name.name + "[" + i + "]", name.scope), dir, hwType, v(i))
        }

    }
    // TODO: Implement for Aggregate types
  }

  /**
   * Parse a [[Data]].
   *
   * This function handles special cases of aggregate types.
   */
  def parse(elId: ElId, name: Name, dir: Direction, hwType: HardwareType, dataType: Data): Unit =
    dataType match {
      case aggr: Bundle    => parse(elId, name, dir, hwType, aggr)
      case aggr: Vec[Data] => parse(elId, name, dir, hwType, aggr) // TODO: Implement
      case other =>
        // TODO: other cases need to be implemented. For now, simply add the element to the map
        flattenedPorts.put(elId.addName(name.name), (name, dir, hwType, Type(dataType.typeName)))
        allElements.put(elId.addName(name.name), (name, dir, Type(dataType.typeName)))
      case _ => throw new Exception(s"Failed to parse type $dataType. Unknown type.")
    }
  //  ??? // TODO: Implement for Data types

  /** Parse a [[chiselIR.Command]]. In FIRRTL, commands are Statements */
  def parse(scope: String, body: chiselIR.Command): Unit =
    body match {
      case chiselIR.DefWire(sourceInfo, dataType) =>
        val elId = createId(sourceInfo)
        allElements.put(
          elId,
          (Name(dataType.toNamed.name, scope), Direction(dataType.direction.toString), Type(dataType.typeName)),
        )
        parse(
          elId,
          Name(dataType.toNamed.name, scope),
          Direction(dataType.direction.toString),
          HardwareType("Wire"),
          dataType,
        )
      case _: chiselIR.Connect    => Console.err.println("Parsing Connect. Skip.")
      case _: chiselIR.DefRegInit => Console.err.println("Parsing DefRegInit. Skip.")
      case _: chiselIR.DefPrim[?] => Console.err.println("Parsing DefPrim. Skip.")
      case _: chiselIR.WhenBegin  => Console.err.println("Parsing WhenBegin. Skip.")
      case _: chiselIR.WhenEnd    => Console.err.println("Parsing WhenEnd. Skip.")
      case _: chiselIR.Printf     => Console.err.println("Parsing Printf. Skip.")
      case a =>
        println(s"aaaa: $a")
        ???
    }
  // TODO: Implement for commands -> Statements in FIRRTL

  /**
   * Create an Id from [[SourceInfo]] to systematically identify the element
   */
  def createId(info: SourceInfo, specialPort: Option[String] = None): ElId =
    info match {
      case _: NoSourceInfo =>
        println(Console.RED + "Warning: Bad ID NoSourceInfo" + Console.RESET)
        ElId(specialPort.getOrElse("NoInfo"), 0, 0)
      case SourceLine(source, row, col) =>
        ElId(source, row, col, specialPort.getOrElse(""))

      case _ => throw new Exception(s"Failed to create ID from $info. Unknown type.")
    }

  override def dumpMaps(fileDump: String): Unit = {
    modules.dumpFile(fileDump, "Modules:", append = false)
    ports.dumpFile(fileDump, "Ports:")
    flattenedPorts.dumpFile(fileDump, "Flattened Ports:")
    allElements.dumpFile(fileDump, "Internal Elements:")
  }

  override def dumpMaps(): Unit = {
    println()
    // Change color
    println(Console.MAGENTA)

    modules.log("Modules:")
    ports.log("Ports:")
    flattenedPorts.log("Ports")
    allElements.log("Internal Elements")

    println(Console.RESET)
  }
}
