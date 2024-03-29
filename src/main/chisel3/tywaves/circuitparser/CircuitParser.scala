package chisel3.tywaves.circuitparser

import tywaves.circuitmapper.{Direction, ElId, HardwareType, Name, Type}
import tywaves.utils.UniqueHashMap

trait CircuitParser[T, ModuleT, PortT, AggregateT, ElementT, BodyStatementT] {

  lazy val modules        = new UniqueHashMap[ElId, (Name, ModuleT)]()
  lazy val ports          = new UniqueHashMap[ElId, (Name, Direction, Type)]()
  lazy val flattenedPorts = new UniqueHashMap[ElId, (Name, Direction, HardwareType, Type)]()
  lazy val allElements    = new UniqueHashMap[ElId, (Name, Direction, Type)]()

  def parseCircuit(circuit: T): Unit
  def parseModule(module:   ModuleT): Unit
  def parsePort(scope:      String, port: PortT, parentModule: String): Unit

  def getWidth(agg: AggregateT): Int = {
    val widthPattern = "<(\\d+)>".r
    agg match {
      case fir: firrtl.ir.AggregateType =>
        fir match {
          case firrtl.ir.BundleType(fields) => fields.map(f =>
              f.tpe match {
                case firrtl.ir.GroundType(width) => width.serialize match {
                    case widthPattern(width) => width.toInt
                  }
                case _: firrtl.ir.AggregateType => this.getWidth(f.tpe.asInstanceOf[AggregateT])
              }

            // extract the number <width> from the GroundType
            ).sum
          case firrtl.ir.VectorType(firrtl.ir.GroundType(width), size) => width.serialize match {
              case widthPattern(width) => width.toInt * size
            }
        }

      case chisel: chisel3.Record    => chisel.getWidth
      case aggr:   chisel3.Aggregate => aggr.getWidth
    }
  }

  def parseAggregate(
      elId:         ElId,
      name:         Name,
      dir:          Direction,
      hwType:       HardwareType,
      agg:          AggregateT,
      parentModule: String,
  ): Unit = {
    val aggString = agg match {
      case fir:    firrtl.ir.AggregateType => fir.getClass.getName
      case chisel: chisel3.Record          => chisel.className
      case aggr:   chisel3.Aggregate       => aggr.typeName
    }
    if (hwType == HardwareType("Port", Some(this.getWidth(agg))))
      flattenedPorts.put(
        elId.addName(name.name),
        (name.addTywaveScope(parentModule), dir, hwType, Type(aggString)),
      )

    allElements.put(elId.addName(name.name), (name.addTywaveScope(parentModule), dir, Type(aggString)))
  }

  def parseElement(
      elId:         ElId,
      name:         Name,
      dir:          Direction,
      hwType:       HardwareType,
      element:      ElementT,
      parentModule: String,
  ): Unit
  def parseBodyStatement(scope: String, body: BodyStatementT, parentModule: String): Unit

  def dumpMaps(fileDump: String): Unit = {
    modules.dumpFile(fileDump, "Modules:", append = false)
    ports.dumpFile(fileDump, "Ports:")
    flattenedPorts.dumpFile(fileDump, "Flattened Ports:")
    allElements.dumpFile(fileDump, "Internal Elements:")
  }
  def dumpMaps(): Unit = {
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
