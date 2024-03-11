package chisel3.tywaves.circuitparser

import tywaves.utils.UniqueHashMap

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
case class Type(name: String) // TODO: add pretty name to type
case class HardwareType(name: String)
case class Direction(name: String)

trait CircuitParser[T, ModuleT, PortT, AggregateT, ElementT, BodyStatementT] {

  lazy val modules        = new UniqueHashMap[ElId, (Name, ModuleT)]()
  lazy val ports          = new UniqueHashMap[ElId, (Name, Direction, Type, PortT)]()
  lazy val flattenedPorts = new UniqueHashMap[ElId, (Name, Direction, HardwareType, Type)]()
  lazy val allElements    = new UniqueHashMap[ElId, (Name, Direction, Type)]()

  def parseCircuit(circuit: T): Unit
  def parseModule(module:   ModuleT): Unit
  def parsePort(scope:      String, port: PortT): Unit
  def parseAggregate(elId: ElId, name: Name, dir: Direction, hwType: HardwareType, agg: AggregateT): Unit = {
    if (hwType == HardwareType("Port"))
      flattenedPorts.put(elId.addName(name.name), (name, dir, hwType, Type(agg.getClass.getName)))
    allElements.put(elId.addName(name.name), (name, dir, Type(agg.getClass.getName)))
  }

  def parseElement(elId:        ElId, name:   Name, dir: Direction, hwType: HardwareType, element: ElementT): Unit
  def parseBodyStatement(scope: String, body: BodyStatementT): Unit

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
