package tywaves.circuitmapper

import scala.math.Ordered.orderingToOrdered

// TODO: Define a case class to output the information in json with circe

object Defaults {
  lazy val elId   = ElId("", 0, 0)
  lazy val name   = Name("", "")
  lazy val hwType = HardwareType("")
  lazy val dir    = Direction("")
  lazy val typ    = Type("")
}

// TODO: Redefine the ElId in a more meaningful way
case class ElId(
    source: String,
    row:    Int,
    col:    Int,
    name:   String = "",// Optionally the name of the element
//    scope:  String, // TODO: Add scope to the ElId
//    implicitEl: Option[String] = None, /* This allows to handle special elements, automatically set */
) extends Ordered[ElId] {

  import scala.math.Ordered.orderingToOrdered

  def addName(name: String): ElId = this.copy(name = name)

  override def compare(that: ElId): Int =
    (this.source, this.row, this.col, this.name) compare ((that.source, that.row, that.col, that.name))

  override def toString: String = s"$name:\t$row:\t$col:\t$source"
}

case class Name(name: String, scope: String) extends Ordered[Name] {
  override def compare(that: Name): Int = (this.name, this.scope) compare ((that.name, that.scope))
  override def toString: String = s"Name: $name, scope: $scope"
}
case class Type(name: String) extends Ordered[Type] {
  override def compare(that: Type): Int = this.name compare that.name
  override def toString: String = s"Type: $name"

} // TODO: add pretty name to type
case class HardwareType(name: String) {
  override def toString: String = s"HardwareType: $name"

}
case class Direction(name: String) extends Ordered[Direction] {
  override def compare(that: Direction): Int = this.name compare that.name
  override def toString: String = s"Direction: $name"
}

case class VerilogSignals(names: Seq[String])
