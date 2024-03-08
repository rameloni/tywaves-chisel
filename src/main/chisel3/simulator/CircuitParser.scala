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
case class Type(name: String) // TODO: add pretty name to type
case class HardwareType(name: String)
case class Direction(name: String)
trait CircuitParser[T] {
  def parse(circuit: T): Unit
  def dumpMaps(fileDump: String): Unit
  def dumpMaps(): Unit
}
