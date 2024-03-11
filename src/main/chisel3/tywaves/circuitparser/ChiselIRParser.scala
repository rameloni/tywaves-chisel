package chisel3.tywaves.circuitparser

import chisel3.experimental.{NoSourceInfo, SourceInfo, SourceLine}
import chisel3.internal.firrtl.{ir => chiselIR}
import chisel3.{Aggregate, Bundle, Data, Vec}
import tywaves.utils.UniqueHashMap

class ChiselIRParser
    extends CircuitParser[chiselIR.Circuit, chiselIR.Component, chiselIR.Port, Aggregate, Data, chiselIR.Command] {
  // Collection of all modules in the circuit
  override lazy val modules        = new UniqueHashMap[ElId, (Name, chiselIR.Component)]()
  override lazy val ports          = new UniqueHashMap[ElId, (Name, Direction, Type, chiselIR.Port)]()
  override lazy val flattenedPorts = new UniqueHashMap[ElId, (Name, Direction, HardwareType, Type)]()
  override lazy val allElements    = new UniqueHashMap[ElId, (Name, Direction, Type)]()

  /** Parse a whole [[chiselIR.Circuit]] */
  override def parseCircuit(circuitChiselIR: chiselIR.Circuit): Unit =
    circuitChiselIR.components.foreach(parseModule) // each component is a module

  /** Parse a chiselIR module = [[chiselIR.Component]] */
  override def parseModule(chiselComponent: chiselIR.Component): Unit = {
    // Parse generic info and create an ID for the module
    val name = chiselComponent.name
    val elId = this.createId(SourceLine(name, 0, 0), Some(name))

    modules.put(elId, (Name(name, "root"), chiselComponent)) // Add the module and its name

    // Parse the internals of the module
    chiselComponent match {
      case chiselIR.DefModule(_, _, ports, body) =>
        ports.foreach(parsePort(name, _))
        // TODO: Parse the body:
        body.foreach(parseBodyStatement(name, _))
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
  override def parsePort(scope: String, port: chiselIR.Port): Unit = {
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
        parseAggregate(elId, Name(name, scope), Direction(dir.toString), HardwareType("Port"), agg)
      case _ => parseElement(elId, Name(name, scope), Direction(dir.toString), HardwareType("Port"), portData)
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
  override def parseAggregate(
      elId:     ElId,
      name:     Name,
      dir:      Direction,
      hwType:   HardwareType,
      aggrType: Aggregate,
  ): Unit = {

    super.parseAggregate(elId, name, dir, hwType, aggrType)

    aggrType match {
      case b: Bundle =>
        b.elements.foreach { case (fieldName, dataType) =>
          parseElement(elId, Name(fieldName, name.name), dir, hwType, dataType)
          println(s"AggregateType: $aggrType, dir: $dir, hwType: $hwType, name: $name")
        }
      case v: Vec[Data] =>
        for (i <- 0 until v.length) {
          parseElement(elId, Name(name.name + "[" + i + "]", name.scope), dir, hwType, v(i))
        }

    }
    // TODO: Implement for Aggregate types
  }

  /**
   * Parse a [[Data]].
   *
   * This function handles special cases of aggregate types.
   */
  def parseElement(elId: ElId, name: Name, dir: Direction, hwType: HardwareType, dataType: Data): Unit =
    dataType match {
      case aggr: Bundle    => parseAggregate(elId, name, dir, hwType, aggr)
      case aggr: Vec[Data] => parseAggregate(elId, name, dir, hwType, aggr) // TODO: Implement
      case _ =>
        // TODO: other cases need to be implemented. For now, simply add the element to the map
        if (hwType == HardwareType("Port"))
          flattenedPorts.put(elId.addName(name.name), (name, dir, hwType, Type(dataType.typeName)))
        allElements.put(elId.addName(name.name), (name, dir, Type(dataType.typeName)))
      case _ => throw new Exception(s"Failed to parse type $dataType. Unknown type.")
    }
  //  ??? // TODO: Implement for Data types

  /** Parse a [[chiselIR.Command]]. In FIRRTL, commands are Statements */
  override def parseBodyStatement(scope: String, body: chiselIR.Command): Unit =
    body match {
      case chiselIR.DefWire(sourceInfo, dataType) =>
        val elId = createId(sourceInfo)
        allElements.put(
          elId,
          (Name(dataType.toNamed.name, scope), Direction(dataType.direction.toString), Type(dataType.typeName)),
        )
        parseElement(
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
        println(s"a a a: $a")
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

}
