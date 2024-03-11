package chisel3.tywaves.circuitparser

import firrtl.{ir => firrtlIR}
import tywaves.utils.UniqueHashMap
import tywaves.circuitmapper.{ElId, Name, Direction, Type, HardwareType}

class FirrtlIRParser
    extends CircuitParser[
      firrtlIR.Circuit,
      firrtlIR.DefModule,
      firrtlIR.Port,
      firrtlIR.AggregateType,
      firrtlIR.Type,
      firrtlIR.Statement,
    ] {
  // Collection of all modules in the circuit
  override lazy val modules        = new UniqueHashMap[ElId, (Name, firrtlIR.DefModule)]()
  override lazy val ports          = new UniqueHashMap[ElId, (Name, Direction, Type /*, firrtlIR.Port*/ )]()
  override lazy val flattenedPorts = new UniqueHashMap[ElId, (Name, Direction, HardwareType, Type)]()
  override lazy val allElements    = new UniqueHashMap[ElId, (Name, Direction, Type)]()

  /** Parse a whole [[firrtlIR.Circuit]] */
  override def parseCircuit(circuitFirrtlIR: firrtlIR.Circuit): Unit =
    circuitFirrtlIR.modules.foreach(parseModule)

  /** Parse a whole [[firrtlIR.DefModule]] */
  override def parseModule(firrtlModule: firrtlIR.DefModule): Unit = {
    // Parse generic info and create an ID for the module
    val (name, info) = (firrtlModule.name, firrtlModule.info)
    val elId         = this.createId(info)

    modules.put(elId, (Name(name, "root"), firrtlModule)) // Add the module and its name

    // Parse the internals of the module
    firrtlModule match {
      case firrtlIR.Module(_, _, ports, body) =>
        ports.foreach(parsePort(name, _))
        // TODO: Parse the body:
        parseBodyStatement(name, body)
      case firrtlIR.ExtModule(_, name, ports, defname, params) =>
        println(s"ExtModule: name: $name, ports: $ports, defname: $defname, params: $params")

      case _ => throw new Exception(s"Failed to parse module $name. Unknown type.")
    }
  }

  /** Parse a [[firrtlIR.Port]] */
  override def parsePort(scope: String, port: firrtlIR.Port): Unit = {
    // Parse generic info and create an ID for the port
    val (name, info, dir, firrtlType) = (port.name, port.info, port.direction, port.tpe)
    val elId                          = this.createId(info, Some(name))

    ports.put(
      elId,
      (Name(name, scope), Direction(dir.toString), Type(firrtlType.getClass.getSimpleName) /*, port*/ ),
    ) // Add the port and its name

    // Parse the type to build flattened ports
    firrtlType match {
      case agg: firrtlIR.AggregateType =>
        println(s"AggregateType: $agg")
        parseAggregate(elId, Name(name, scope), Direction(dir.toString), HardwareType("Port"), agg)
      case _ => parseElement(elId, Name(name, scope), Direction(dir.toString), HardwareType("Port"), firrtlType)
    }
  }

  /**
   * Specialized function for AggregateTypes like Bundles and Vecs.
   *
   * It unwraps Bundles and Vecs and executes specific parse functions of other
   * types.
   */
  override def parseAggregate(
      elId:     ElId,
      name:     Name,
      dir:      Direction,
      hwType:   HardwareType,
      aggrType: firrtlIR.AggregateType,
  ): Unit = {
    super.parseAggregate(elId, name, dir, hwType, aggrType)

    aggrType match {
      case firrtlIR.BundleType(fields) =>
        fields.foreach { case firrtlIR.Field(fieldName, _, tpe) =>
          parseElement(elId, Name(fieldName, name.name), dir, hwType, tpe)
        }
      case firrtlIR.VectorType(tpe, size) =>
        for (i <- 0 until size) {
          //          flattenedPorts.put(elId.addName(name.name + "[" + i + "]"), (name, dir, hwType, Type(tpe.toString)))
          //          internalElements.put(elId.addName(name.name + "[" + i + "]"), (name, dir, Type(tpe.toString)))
          parseElement(elId, Name(name.name + "[" + i + "]", name.scope), dir, hwType, tpe)
        }
      //        ??? // TODO: Implement
    }
  }

  /**
   * Parse a [[firrtlIR.Type]].
   *
   * This function handles special cases of aggregate types.
   */
  override def parseElement(
      elId:       ElId,
      name:       Name,
      dir:        Direction,
      hwType:     HardwareType,
      firrtlType: firrtlIR.Type,
  ): Unit = {
    import firrtlIR._
    firrtlType match {
      case aggr: BundleType => parseAggregate(elId, name, dir, hwType, aggr)
      case aggr: VectorType => parseAggregate(elId, name, dir, hwType, aggr) // TODO: Implement
      case ClockType | AsyncResetType | ResetType |
          UIntType(_) | SIntType(_) | AnalogType(_) =>
        if (hwType == HardwareType("Port"))
          flattenedPorts.put(elId.addName(name.name), (name, dir, hwType, Type(firrtlType.toString)))
        allElements.put(elId.addName(name.name), (name, dir, Type(firrtlType.toString)))
      case _ => throw new Exception(s"Failed to parse type $firrtlType. Unknown type.")
    }
  }

  /** Parse a [[firrtlIR.Statement]] */
  def parseBodyStatement(scope: String, body: firrtlIR.Statement): Unit = {
    import firrtlIR._
    body match {
      case Block(stmts)             => stmts.foreach(parseBodyStatement(scope, _))
      case DefWire(info, name, tpe) =>
//        allElements.put(elId, (Name(name, scope), Direction("no dir"), Type(tpe.toString)))
        parseElement(createId(info, Some(name)), Name(name, scope), Direction("no dir"), HardwareType("Wire"), tpe)

      case DefRegisterWithReset(info, name, tpe, _, _, _) =>
//        allElements.put(elId, (Name(name, scope), Direction("no dir"), Type(tpe.toString)))
        parseElement(createId(info, Some(name)), Name(name, scope), Direction("no dir"), HardwareType("Register"), tpe)

      case _: Connect       => Console.err.println("Parsing Connect. Skip.")
      case _: DefNode       => Console.err.println("Parsing DefNode. Skip.")
      case _: Conditionally => Console.err.println("Parsing Conditionally. Skip.")
      case a => // TODO: other cases to be implemented
        println("aaa: " + a)
        ???
    }
  }

  /**
   * Create an Id from [[firrtlIR.Info]] to systematically identify the element
   */
  def createId(info: firrtlIR.Info, specialPort: Option[String] = None): ElId =
    info match {
      case firrtlIR.NoInfo =>
        println(Console.RED + "Warning: Bad ID NoInfo" + Console.RESET)
        ElId(specialPort.getOrElse("NoInfo"), 0, 0)
      case f: firrtlIR.FileInfo =>
        val (source, row, col) = f.split
        ElId(source, row.toInt, col.toInt, name = specialPort.getOrElse(""))

      case _ => throw new Exception(s"Failed to create ID from $info. Unknown type.")
    }

}
