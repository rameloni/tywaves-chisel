package chisel3.tywaves.typedTreadle

import chisel3.Data
import chisel3.experimental.EnumAnnotations.EnumDefAnnotation
import chisel3.experimental.{ChiselAnnotation, annotate, requireIsHardware}
import firrtl.annotations.{ReferenceTarget, SingleTargetAnnotation}
import firrtl.ir.{IntWidth, Width}
import firrtl.transforms.DontTouchAnnotation
import firrtl2.ir.UIntType

object MyAnnotation {

  def serialize[T <: Data](data: T): String = {
    EnumDefAnnotation("Bla", Map.apply("a" -> 1, "b" -> 2)).serialize
  }
  def apply[T <: Data](data: T): T = {
    requireIsHardware(data, "Data marked dontTouch")
    annotate(new ChiselAnnotation {
      def toFirrtl = EnumDefAnnotation("Bla", Map.apply("a" -> 1, "b" -> 2))
    })
    data
  }
}

case class MyAnnotation(target: ReferenceTarget) extends SingleTargetAnnotation[ReferenceTarget] {

  //  def targets = Seq(target, target)
  def duplicate(n: ReferenceTarget) = this.copy(n)

  override def serialize: String = {
    val targetString = target.serialize

    val circuit = target.circuit
    val module = target.module
    val path = target.path
    val ref = target.ref
    val component = target.component

    val mystring = s"target: $targetString, circuit: $circuit, module: $module, path: $path, ref: $ref, component: $component"
    mystring + this.toString
  }

}
