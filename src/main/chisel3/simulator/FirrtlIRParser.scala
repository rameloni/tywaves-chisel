package chisel3.simulator

class FirrtlIRParser extends CircuitParser[firrtl.ir.Circuit] {
  // Collection of all modules in the circuit
  lazy val modules        = new UniqueHashMap[ElId, (Name, firrtl.ir.DefModule)]()
  lazy val ports          = new UniqueHashMap[ElId, (Name, Direction, Type, firrtl.ir.Port)]()
  lazy val flattenedPorts = new UniqueHashMap[ElId, (Name, Direction, HardwareType, Type)]()
  lazy val allElements    = new UniqueHashMap[ElId, (Name, Direction, Type)]()

  /** Parse a whole [[firrtl.ir.Circuit]] */
  override def parse(circuitFirrtlIR: firrtl.ir.Circuit): Unit =
    circuitFirrtlIR.modules.foreach(parse)

  /** Parse a whole [[firrtl.ir.DefModule]] */
  def parse(firrtlModule: firrtl.ir.DefModule): Unit = {
    // Parse generic info and create an ID for the module
    val (name, info) = (firrtlModule.name, firrtlModule.info)
    val elId         = this.createId(info)

    modules.put(elId, (Name(name, "root"), firrtlModule)) // Add the module and its name

    // Parse the internals of the module
    firrtlModule match {
      case firrtl.ir.Module(_, _, ports, body) =>
        ports.foreach(parse(name, _))
        // TODO: Parse the body:
        parse(name, body)
      case firrtl.ir.ExtModule(_, name, ports, defname, params) =>
        println(s"ExtModule: name: $name, ports: $ports, defname: $defname, params: $params")

      case _ => throw new Exception(s"Failed to parse module $name. Unknown type.")
    }
  }

  /** Parse a [[firrtl.ir.Port]] */
  def parse(scope: String, port: firrtl.ir.Port): Unit = {
    // Parse generic info and create an ID for the port
    val (name, info, dir, firrtlType) = (port.name, port.info, port.direction, port.tpe)
    val elId                          = this.createId(info, Some(name))

    ports.put(
      elId,
      (Name(name, scope), Direction(dir.toString), Type(firrtlType.toString), port),
    ) // Add the port and its name

    // Parse the type to build flattened ports
    firrtlType match {
      case agg: firrtl.ir.AggregateType =>
        println(s"AggregateType: $agg")
        parse(elId, Name(name, scope), Direction(dir.toString), HardwareType("Port"), agg)
      case _ => parse(elId, Name(name, scope), Direction(dir.toString), HardwareType("Port"), firrtlType)
    }
  }

  /**
   * Specialized function for AggregateTypes like Bundles and Vecs.
   *
   * It unwraps Bundles and Vecs and executes specific parse functions of other
   * types.
   */
  def parse(elId: ElId, name: Name, dir: Direction, hwType: HardwareType, aggrType: firrtl.ir.AggregateType): Unit = {
    flattenedPorts.put(elId.addName(name.name), (name, dir, hwType, Type(aggrType.getClass.getName)))
    allElements.put(elId.addName(name.name), (name, dir, Type(aggrType.getClass.getName)))
    aggrType match {
      case b @ firrtl.ir.BundleType(fields) =>
        fields.foreach { case firrtl.ir.Field(fieldName, flip, tpe) =>
          parse(elId, Name(fieldName, name.name), dir, hwType, tpe)
        }
      case v @ firrtl.ir.VectorType(tpe, size) =>
        for (i <- 0 until size) {
          //          flattenedPorts.put(elId.addName(name.name + "[" + i + "]"), (name, dir, hwType, Type(tpe.toString)))
          //          internalElements.put(elId.addName(name.name + "[" + i + "]"), (name, dir, Type(tpe.toString)))
          parse(elId, Name(name.name + "[" + i + "]", name.scope), dir, hwType, tpe)
        }
      //        ??? // TODO: Implement
    }
  }

  /**
   * Parse a [[firrtl.ir.Type]].
   *
   * This function handles special cases of aggregate types.
   */
  def parse(elId: ElId, name: Name, dir: Direction, hwType: HardwareType, firrtlType: firrtl.ir.Type): Unit = {
    import firrtl.ir._
    firrtlType match {
      case aggr: BundleType => parse(elId, name, dir, hwType, aggr)
      case aggr: VectorType => parse(elId, name, dir, hwType, aggr) // TODO: Implement
      case ClockType | AsyncResetType | ResetType |
          UIntType(_) | SIntType(_) | AnalogType(_) =>
        flattenedPorts.put(elId.addName(name.name), (name, dir, hwType, Type(firrtlType.toString)))
        allElements.put(elId.addName(name.name), (name, dir, Type(firrtlType.toString)))
      case _ => throw new Exception(s"Failed to parse type $firrtlType. Unknown type.")
    }
  }

  /** Parse a [[firrtl.ir.Statement]] */
  def parse(scope: String, body: firrtl.ir.Statement): Unit = {
    import firrtl.ir._
    body match {
      case Block(stmts) => stmts.foreach(parse(scope, _))
      case DefWire(info, name, tpe) =>
        val elId = this.createId(info, Some(name))
        allElements.put(elId, (Name(name, scope), Direction("no dir"), Type(tpe.toString)))
        parse(elId, Name(name, scope), Direction("no dir"), HardwareType("Wire"), tpe)

      case DefRegisterWithReset(info, name, tpe, clock, reset, init) =>
        val elId = this.createId(info, Some(name))
        allElements.put(elId, (Name(name, scope), Direction("no dir"), Type(tpe.toString)))
        parse(elId, Name(name, scope), Direction("no dir"), HardwareType("Register"), tpe)

      case _: Connect       => Console.err.println("Parsing Connect. Skip.")
      case _: DefNode       => Console.err.println("Parsing DefNode. Skip.")
      case _: Conditionally => Console.err.println("Parsing Conditionally. Skip.")
      case a => // TODO: other cases to be implemented
        println("aaa: " + a);
        ???
    }
  }

  /**
   * Create an Id from [[firrtl.ir.Info]] to systematically identify the element
   */
  def createId(info: firrtl.ir.Info, specialPort: Option[String] = None): ElId =
    info match {
      case firrtl.ir.NoInfo =>
        println(Console.RED + "Warning: Bad ID NoInfo" + Console.RESET)
        ElId(specialPort.getOrElse("NoInfo"), 0, 0)
      case f: firrtl.ir.FileInfo =>
        val (source, row, col) = f.split
        ElId(source, row.toInt, col.toInt)

      case _ => throw new Exception(s"Failed to create ID from $info. Unknown type.")
    }

  def dumpMaps(): Unit = {
    println()
    // Change color
    println(Console.CYAN)
    println("Modules:")
    modules.foreach { case (name, module) =>
      println(s"\t$name: $module")
    }
    println("Ports:")
    ports.foreach { case (name, port) =>
      println(s"\t$name: $port")
    }

    println("\nFlattened Ports:")
    flattenedPorts.foreach { case (name, port) =>
      println(s"\t$name: $port")
    }

    println(Console.GREEN)
    println("\nInternal Elements:")
    allElements.foreach { case (name, el) =>
      println(s"\t$name: $el")
    }
    println(Console.RESET)
  }
}
