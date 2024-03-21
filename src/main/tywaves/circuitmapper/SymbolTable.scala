package tywaves.circuitmapper

import io.circe.generic.extras.semiauto.deriveConfiguredEncoder

import scala.math.Ordered.orderingToOrdered

// TODO: Define a case class to output the information in json with circe

object Defaults {
  lazy val elId   = ElId("", 0, 0)
  lazy val name   = Name("", "", "")
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

case class Name(name: String, scope: String, tywaveScope: String) extends Ordered[Name] {
  def addTywaveScope(parentModule: String): Name = this.copy(tywaveScope = parentModule)
  override def compare(that: Name): Int =
    (this.name, this.scope, this.tywaveScope) compare ((that.name, that.scope, that.tywaveScope))
  override def toString: String = s"Name: $name, scope: $scope, tywaveScope: $tywaveScope"
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

object tywaves_symbol_table {
  import io.circe._
  object tywaves_encoders {
    import io.circe.generic.extras._
    implicit val customConfiguration: Configuration =
      Configuration.default.withSnakeCaseMemberNames.withSnakeCaseMemberNames

    implicit val encodedTywaveState: Encoder[TywaveState] = deriveConfiguredEncoder[TywaveState]
    implicit val encodedScope:       Encoder[Scope]       = deriveConfiguredEncoder[Scope]
    implicit val encodedVariable:    Encoder[Variable]    = deriveConfiguredEncoder[Variable]

    implicit val encodeHwType: Encoder[hwtype.HwType] = Encoder.instance {
      case hwtype.Wire    => Json.fromString("wire")
      case hwtype.Reg     => Json.fromString("reg")
      case hwtype.Mem     => Json.fromString("mem")
      case hwtype.Unknown => Json.fromString("unknown")
      case _: hwtype.Port => Json.Null
    }

    implicit val encodePort: Encoder[hwtype.Port] = deriveConfiguredEncoder[hwtype.Port]
    implicit val encodeDirection: Encoder[direction.Directions] = Encoder.instance {
      case direction.Input   => Json.fromString("input")
      case direction.Output  => Json.fromString("output")
      case direction.Inout   => Json.fromString("inout")
      case direction.Unknown => Json.fromString("unknown")
    }

    implicit val encodeRealType: Encoder[realtype.RealType] = Encoder.instance {
      case realtype.Unknown => Json.fromString("unknown")
      case g: realtype.Ground => Json.obj("ground" -> encodeGround(g))
      case v: realtype.Vec    => Json.obj("vec" -> encodeVec(v))
      case b: realtype.Bundle => Json.obj("bundle" -> encodeBundle(b))
    }
    implicit val encodeGround: Encoder[realtype.Ground] = deriveConfiguredEncoder[realtype.Ground]
    implicit val encodeVec:    Encoder[realtype.Vec]    = deriveConfiguredEncoder[realtype.Vec]
    implicit val encodeBundle: Encoder[realtype.Bundle] = deriveConfiguredEncoder[realtype.Bundle]
  }

  /** The state for Tywaves */
  case class TywaveState(var scopes: Seq[Scope])

  /** A scope in the state */
  case class Scope(name: String, childVariables: Seq[Variable], childScopes: Seq[Scope])

  case class Variable(
      name:     String,
      typeName: String,
      hwType:   hwtype.HwType,
      realType: realtype.RealType,
  ) {
    def getWidth: Int = realType.getWidth
  }

  /** Hardware types */
  object hwtype {
    def from_string(tpe: String, dir: Option[String]): HwType =
      (tpe, dir) match {
        case ("Port", Some(dir)) => Port(direction.from_string(dir))
        case ("logic", _)        => Wire
        case _                   => Unknown
      }
    sealed trait HwType

    case object Wire extends HwType

    case object Reg extends HwType

    case class Port(dir: direction.Directions) extends HwType

    case object Mem extends HwType

    case object Unknown extends HwType
  }

  object direction {
    def from_string(dir: String): Directions =
      dir match {
        case "Input"  => Input
        case "Output" => Output
        case "Inout"  => Inout
        case _        => Unknown
      }

    sealed trait Directions

    case object Input   extends Directions
    case object Output  extends Directions
    case object Inout   extends Directions
    case object Unknown extends Directions
  }

  object realtype {
//    def from_string(tpe: String): RealType = {
//      tpe match {
//        case "Bool" => Ground(1, "")
//        case
//      }
//    }
    sealed trait RealType {
      def getWidth: Int = 0
    }
    case class Ground(width: Int, vcdName: String) extends RealType {
      override def getWidth: Int = width
    }
    case class Vec(size: Int, fields: Seq[Variable]) extends RealType {
      override def getWidth: Int = if (fields.nonEmpty)
        size * fields.head.realType.getWidth
      else 0
    }
    case class Bundle(fields: Seq[Variable], vcdName: Option[String]) extends RealType {
      override def getWidth: Int = fields.map(f => f.getWidth).sum
    }
    case object Unknown extends RealType
  }

}
