package chisel3.tywaves.circuitparser

import com.typesafe.scalalogging.Logger
import firrtl.{ir => firrtlIR}
import tywaves.utils.UniqueHashMap
import tywaves.circuitmapper.{Direction, ElId, HardwareType, Name, Type}

@deprecated("This class is not used anymore. It is kept for reference.", "0.3.0")
class FirrtlIRParser
    extends CircuitParser[
      firrtlIR.Circuit,
      firrtlIR.DefModule,
      firrtlIR.Port,
      firrtlIR.AggregateType,
      firrtlIR.Type,
      firrtlIR.Statement,
    ] {

  private val logger = Logger(getClass.getName)

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
    val elId         = this.createId(info, Some(name))

    modules.put(elId, (Name(name, "root", "root"), firrtlModule)) // Add the module and its name

    // Parse the internals of the module
    firrtlModule match {
      case firrtlIR.Module(_, moduleName, _, _, ports, body) =>
        ports.foreach(parsePort(name, _, moduleName))
        // TODO: Parse the body:
        parseBodyStatement(name, body, moduleName)
      case firrtlIR.ExtModule(_, name, ports, defname, params) =>
        logger.debug(s"ExtModule: name: $name, ports: $ports, defname: $defname, params: $params")

      case _ => throw new Exception(s"Failed to parse module $name. Unknown type.")
    }
  }

  /** Parse a [[firrtlIR.Port]] */
  override def parsePort(scope: String, port: firrtlIR.Port, parentModule: String): Unit = {
    // Parse generic info and create an ID for the port
    val (name, info, dir, firrtlType) = (port.name, port.info, port.direction, port.tpe)
    val elId                          = this.createId(info, Some(name + scope))

    ports.put(
      elId,
      (Name(name, scope, parentModule), Direction(dir.toString), Type(firrtlType.getClass.getSimpleName) /*, port*/ ),
    ) // Add the port and its name

    // Parse the type to build flattened ports
    firrtlType match {
      case agg: firrtlIR.AggregateType =>
        parseAggregate(
          elId,
          Name(name, scope, parentModule),
          Direction(dir.toString),
          HardwareType("Port", Some(this.getWidth(agg))),
          agg,
          parentModule,
        )
      case _ =>
        parseElement(
          elId,
          Name(name, scope, parentModule),
          Direction(dir.toString),
          HardwareType("Port", None),
          firrtlType,
          parentModule,
        )
    }
  }

  /**
   * Specialized function for AggregateTypes like Bundles and Vecs.
   *
   * It unwraps Bundles and Vecs and executes specific parse functions of other
   * types.
   */
  override def parseAggregate(
      elId:         ElId,
      name:         Name,
      dir:          Direction,
      hwType:       HardwareType,
      aggrType:     firrtlIR.AggregateType,
      parentModule: String,
  ): Unit = {
    super.parseAggregate(elId, name, dir, hwType, aggrType, parentModule)

    aggrType match {
      case firrtlIR.BundleType(fields) =>
        fields.foreach { case firrtlIR.Field(fieldName, _, tpe) =>
          parseElement(elId, Name(fieldName, name.name, parentModule), dir, hwType, tpe, parentModule)
        }
      case firrtlIR.VectorType(tpe, size) =>
        for (i <- 0 until size) {
          //          flattenedPorts.put(elId.addName(name.name + "[" + i + "]"), (name, dir, hwType, Type(tpe.toString)))
          //          internalElements.put(elId.addName(name.name + "[" + i + "]"), (name, dir, Type(tpe.toString)))
          parseElement(elId, Name(name.name + "[" + i + "]", name.scope, parentModule), dir, hwType, tpe, parentModule)
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
      elId:         ElId,
      name:         Name,
      dir:          Direction,
      hwType:       HardwareType,
      firrtlType:   firrtlIR.Type,
      parentModule: String,
  ): Unit = {
    import firrtlIR._
    firrtlType match {
      case aggr: BundleType => parseAggregate(elId, name, dir, hwType, aggr, parentModule)
      case aggr: VectorType => parseAggregate(elId, name, dir, hwType, aggr, parentModule) // TODO: Implement
      case ClockType | AsyncResetType | ResetType |
          UIntType(_) | SIntType(_) | AnalogType(_) =>
        if (hwType == HardwareType("Port", None))
          flattenedPorts.put(elId.addName(name.name + parentModule), (name, dir, hwType, Type(firrtlType.toString)))
        allElements.put(elId.addName(name.name + parentModule), (name, dir, Type(firrtlType.toString)))
      case _ => throw new Exception(s"Failed to parse type $firrtlType. Unknown type.")
    }
  }

  /** Parse a [[firrtlIR.Statement]] */
  def parseBodyStatement(scope: String, body: firrtlIR.Statement, parentModule: String): Unit = {
    import firrtlIR._
    body match {
      case Block(stmts)             => stmts.foreach(parseBodyStatement(scope, _, parentModule))
      case DefWire(info, name, tpe) =>
//        allElements.put(elId, (Name(name, scope), Direction("no dir"), Type(tpe.toString)))
        parseElement(
          createId(info, Some(name)),
          Name(name, scope, parentModule),
          Direction("no dir"),
          HardwareType("Wire", None),
          tpe,
          parentModule,
        )

      case DefRegisterWithReset(info, name, tpe, _, _, _) =>
//        allElements.put(elId, (Name(name, scope), Direction("no dir"), Type(tpe.toString)))
        parseElement(
          createId(info, Some(name)),
          Name(name, scope, parentModule),
          Direction("no dir"),
          HardwareType("Register", None),
          tpe,
          parentModule,
        )
      case DefRegister(info, name, tpe, _) =>
        //        allElements.put(elId, (Name(name, scope), Direction("no dir"), Type(tpe.toString)))
        parseElement(
          createId(info, Some(name)),
          Name(name, scope, parentModule),
          Direction("no dir"),
          HardwareType("Register", None),
          tpe,
          parentModule,
        )
      case DefInstance(info, name, module, tpe) => {
        logger.error(s"DefInstance: name: $name, module: $module, tpe: $tpe. Skip.")
      }
      case _: Connect       => logger.debug("FirrtlIR parser: Parsing Connect. Skip.")
      case _: DefNode       => logger.debug("FirrtlIR parser: Parsing DefNode. Skip.")
      case _: Conditionally => logger.debug("FirrtlIR parser: Parsing Conditionally. Skip.")
      case a => // TODO: other cases to be implemented
        logger.error(s"Match case not covered: $a")
        ???
    }
  }

  /**
   * Create an Id from [[firrtlIR.Info]] to systematically identify the element
   */
  def createId(info: firrtlIR.Info, specialPort: Option[String] = None): ElId =
    info match {
      case firrtlIR.NoInfo =>
        logger.debug("Warning: Bad ID NoInfo")
        ElId(specialPort.getOrElse("NoInfo"), 0, 0)
      case f: firrtlIR.FileInfo =>
        val (source, row, col) = f.split
        ElId(source, row.toInt, col.toInt, name = specialPort.getOrElse(""))

      case _ => throw new Exception(s"Failed to create ID from $info. Unknown type.")
    }

}
