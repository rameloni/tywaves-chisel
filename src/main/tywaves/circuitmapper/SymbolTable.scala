package tywaves.circuitmapper

import scala.math.Ordered.orderingToOrdered

object Defaults {
  lazy val elId   = ElId("", 0, 0)
  lazy val name   = Name("", "")
  lazy val hwType = HardwareType("")
  lazy val dir    = Direction("")
  lazy val typ    = Type("")
}

case class ElId(
    source: String,
    row:    Int,
    col:    Int,
    name:   String = "",// Optionally the name of the element
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
}
case class Type(name: String) extends Ordered[Type] {
  override def compare(that: Type): Int = this.name compare that.name

} // TODO: add pretty name to type
case class HardwareType(name: String)
case class Direction(name: String) extends Ordered[Direction] {
  override def compare(that: Direction): Int = this.name compare that.name
}
