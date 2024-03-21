package chisel3.tywaves.circuitparser

import chisel3.experimental.{NoSourceInfo, SourceInfo, SourceLine}
import chisel3.internal.firrtl.{ir => chiselIR}
import chisel3.{Aggregate, Bundle, Data, Vec}
import tywaves.utils.UniqueHashMap
import tywaves.circuitmapper.{Direction, ElId, HardwareType, Name, Type, tywaves_symbol_table}

class ChiselIRParser
    extends CircuitParser[chiselIR.Circuit, chiselIR.Component, chiselIR.Port, Aggregate, Data, chiselIR.Command] {
  // Collection of all modules in the circuit
  override lazy val modules        = new UniqueHashMap[ElId, (Name, chiselIR.Component)]()
  override lazy val ports          = new UniqueHashMap[ElId, (Name, Direction, Type /*, chiselIR.Port*/ )]()
  override lazy val flattenedPorts = new UniqueHashMap[ElId, (Name, Direction, HardwareType, Type)]()
  override lazy val allElements    = new UniqueHashMap[ElId, (Name, Direction, Type)]()

  private var _tywavesState = tywaves_symbol_table.TywaveState(Seq.empty)
  def tywaveState: tywaves_symbol_table.TywaveState = _tywavesState

  /** Parse a whole [[chiselIR.Circuit]] */
  override def parseCircuit(circuitChiselIR: chiselIR.Circuit): Unit =
    circuitChiselIR.components.foreach(parseModule) // each component is a module

  /** Parse a chiselIR module = [[chiselIR.Component]] */
  override def parseModule(chiselComponent: chiselIR.Component): Unit = {
    // Parse generic info and create an ID for the module
    val name = chiselComponent.name
    val elId = this.createId(SourceLine(name, 0, 0), Some(name))

    modules.put(elId, (Name(name, "root", "root"), chiselComponent)) // Add the module and its name
    _tywavesState.scopes = _tywavesState.scopes :+
      tywaves_symbol_table.Scope(name, Seq.empty, Seq.empty)

    // Parse the internals of the module
    chiselComponent match {
      case chiselIR.DefModule(_, moduleName, ports, body) =>
        ports.foreach(parsePort(name, _, moduleName))
        // TODO: Parse the body:
        body.foreach(parseBodyStatement(name, _, moduleName))
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
  override def parsePort(scope: String, port: chiselIR.Port, parentModule: String): Unit = {
    val portData: Data = port.id

    // Parse generic info and create an ID for the port
    val (name, info, dir) = (portData.toNamed.name, port.sourceInfo, port.dir)
    val elId              = this.createId(info, Some(name))

    ports.put(
      elId,
      (
        Name(name, scope, parentModule),
        Direction(dir.toString),
        Type(portData.typeName), /*, port*/
      ),                         // Fixme: type name
    )                            // Add the port and its name

    // Types from here: https://github.com/chipsalliance/chisel?tab=readme-ov-file#data-types-overview
    portData match {
      case agg: Aggregate =>
        // TODO: check this
        println(s"AggregateType: $agg")
        parseAggregate(
          elId,
          Name(name, scope, parentModule),
          Direction(dir.toString),
          HardwareType("Port"),
          agg,
          parentModule,
        )
      case _ => parseElement(
          elId,
          Name(name, scope, parentModule),
          Direction(dir.toString),
          HardwareType("Port"),
          portData,
          parentModule,
        )
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
      elId:         ElId,
      name:         Name,
      dir:          Direction,
      hwType:       HardwareType,
      aggrType:     Aggregate,
      parentModule: String,
  ): Unit = {

    super.parseAggregate(elId, name, dir, hwType, aggrType, parentModule)

    aggrType match {
      case b: Bundle =>
        b.elements.foreach { case (fieldName, dataType) =>
          parseElement(elId, Name(fieldName, name.name, parentModule), dir, hwType, dataType, parentModule)
          println(s"AggregateType: $aggrType, dir: $dir, hwType: $hwType, name: $name")

          val variable = tywaves_symbol_table.Variable(
            name.name,
            dataType.typeName,
            tywaves_symbol_table.hwtype.from_string(hwType.name, Some(dir.name)),
            tywaves_symbol_table.realtype.Bundle(Seq.empty, None),
          )
          _tywavesState.scopes = _tywavesState.scopes :+
            tywaves_symbol_table.Scope(fieldName, Seq.empty, Seq.empty)
        }
      case v: Vec[Data] =>
        for (i <- 0 until v.length) {
          parseElement(elId, Name(name.name + "[" + i + "]", name.scope, parentModule), dir, hwType, v(i), parentModule)
        }

    }
    // TODO: Implement for Aggregate types
  }

  /**
   * Parse a [[Data]].
   *
   * This function handles special cases of aggregate types.
   */
  def parseElement(
      elId:         ElId,
      name:         Name,
      dir:          Direction,
      hwType:       HardwareType,
      dataType:     Data,
      parentModule: String,
  ): Unit =
    dataType match {
      case aggr: Bundle    => parseAggregate(elId, name, dir, hwType, aggr, parentModule)
      case aggr: Vec[Data] => parseAggregate(elId, name, dir, hwType, aggr, parentModule) // TODO: Implement
      case _ =>
        // TODO: other cases need to be implemented. For now, simply add the element to the map
        if (hwType == HardwareType("Port"))
          flattenedPorts.put(elId.addName(name.name), (name, dir, hwType, Type(dataType.typeName)))
        allElements.put(elId.addName(name.name), (name, dir, Type(dataType.typeName)))
      case _ => throw new Exception(s"Failed to parse type $dataType. Unknown type.")
    }
  //  ??? // TODO: Implement for Data types

  /** Parse a [[chiselIR.Command]]. In FIRRTL, commands are Statements */
  override def parseBodyStatement(scope: String, body: chiselIR.Command, parentModule: String): Unit = {
    val parseRes = body match {
      case chiselIR.DefWire(sourceInfo, dataType)             => Some((sourceInfo, dataType, HardwareType("Wire")))
      case chiselIR.DefReg(sourceInfo, dataType, _)           => Some((sourceInfo, dataType, HardwareType("Register")))
      case chiselIR.DefRegInit(sourceInfo, dataType, _, _, _) => Some((sourceInfo, dataType, HardwareType("Register")))

      case _: chiselIR.Connect      => Console.err.println("ChiselIRParser: Parsing Connect. Skip."); None
      case _: chiselIR.DefPrim[?]   => Console.err.println("ChiselIRParser: Parsing DefPrim. Skip."); None
      case _: chiselIR.WhenBegin    => Console.err.println("ChiselIRParser: Parsing WhenBegin. Skip."); None
      case _: chiselIR.WhenEnd      => Console.err.println("ChiselIRParser: Parsing WhenEnd. Skip."); None
      case _: chiselIR.Printf       => Console.err.println("ChiselIRParser: Parsing Printf. Skip."); None
      case _: chiselIR.AltBegin     => Console.err.println("ChiselIRParser: Parsing AltBegin. Skip."); None
      case _: chiselIR.OtherwiseEnd => Console.err.println("ChiselIRParser: Parsing OtherwiseEnd. Skip."); None
      case a =>
        println(s"a a a: $a")
        None
        ???
    }
    parseRes match {
      case Some((sourceInfo, dataType, hwType)) =>
        parseElement(
          createId(sourceInfo),
          Name(dataType.toNamed.name, scope, parentModule),
          Direction(dataType.direction.toString),
          hwType,
          dataType,
          parentModule,
        )
      case None => // Skip
    }

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
