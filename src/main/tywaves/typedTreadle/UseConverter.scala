package chisel3
package tywaves.typedTreadle

import chisel3.internal.firrtl.Converter
import firrtl.annotations.Annotation
import firrtl.ir.{CircuitWithAnnos, Serializer}

object UseConverter {
  val io = (Input(UInt(8.W)))
  val direction = chisel3.SpecifiedDirection.Input
  val port = chisel3.internal.firrtl.Port(io, direction, chisel3.experimental.UnlocatableSourceInfo)

  val param = chisel3.experimental.IntParam(8)
  val paramConverted: firrtl.ir.Param = Converter.convert("cio", param)

  // TODO: How can I use this? How is it useful? I do not have yet the final name used in VCD.
  /** Convert a port to the respective FIRRTL */
  def convert(port: chisel3.internal.firrtl.Port, topDir: chisel3.SpecifiedDirection = chisel3.SpecifiedDirection.Unspecified): firrtl.ir.Port = {
    Converter.convert(port, topDir)
  }



}
