package chisel3.simulator

case class ElId(
    source: String,
    row:    Int,
    col:    Int,
    name:   String = "",// Optionally the name of the element
//    implicitEl: Option[String] = None, /* This allows to handle special elements, automatically set */
) {
  def addName(name: String): ElId = this.copy(name = name)
}

case class Name(name: String, scope: String)
case class Type(name: String)
case class Direction(name: String)

class CircuitParser {
  // Collection of all modules in the circuit
  lazy val modules        = new UniqueHashMap[ElId, (Name, firrtl.ir.DefModule)]()
  lazy val ports          = new UniqueHashMap[ElId, (Name, Direction, Type, firrtl.ir.Port)]()
  lazy val flattenedPorts = new UniqueHashMap[ElId, (Name, Direction, Type)]()

  /** Parse a whole [[firrtl.ir.Circuit]] */
  def parse(circuitFirrtlIR: firrtl.ir.Circuit): Unit =
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
      //  parse(body)
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
        parse(elId, Name(name, scope), Direction(dir.toString), agg)
      case _ => parse(elId, Name(name, scope), Direction(dir.toString), firrtlType)
    }
  }

  /**
   * Specialized function for AggregateTypes like Bundles and Vecs.
   *
   * It unwraps Bundles and Vecs and executes specific parse functions of other
   * types.
   */
  def parse(elId: ElId, name: Name, dir: Direction, aggrType: firrtl.ir.AggregateType): Unit =
    aggrType match {
      case b @ firrtl.ir.BundleType(fields) =>
        flattenedPorts.put(elId.addName(name.name), (name, dir, Type(b.getClass.getName)))
        fields.foreach { case firrtl.ir.Field(fieldName, flip, tpe) =>
          parse(elId, Name(fieldName, name.name), dir, tpe)
        }
      case v @ firrtl.ir.VectorType(tpe, size) => ??? // TODO: Implement
    }

  /**
   * Parse a [[firrtl.ir.Type]].
   *
   * This function handles special cases of aggregate types.
   */
  def parse(elId: ElId, name: Name, dir: Direction, firrtlType: firrtl.ir.Type): Unit = {
    import firrtl.ir._
    firrtlType match {
      case aggr: BundleType => parse(elId, name, dir, aggr)
      case VectorType(tpe, size) => ??? // TODO: Implement
      case ClockType | AsyncResetType | ResetType |
          UIntType(_) | SIntType(_) | AnalogType(_) =>
        flattenedPorts.put(elId.addName(name.name), (name, dir, Type(firrtlType.toString)))
      case _ => throw new Exception(s"Failed to parse type $firrtlType. Unknown type.")
    }
  }

  /** Parse a [[firrtl.ir.Statement]] */
  def parse(body: firrtl.ir.Statement): Unit =
    ???

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
    println(Console.RESET)
  }
}
