package chisel3.simulator

import svsim.verilator

/** Trait to represent the simulator settings */
private trait Settings[T] {
  val trace: T
}

/**
 * [[TraceVcd]] is an abstracted representation of the
 * [[verilator.Backend.CompilationSettings.TraceStyle.Vcd]] that can be used by
 * the user of the simulator
 */
private[simulator] case class TraceVcd(traceUnderscore: Boolean = false)
    extends Settings[verilator.Backend.CompilationSettings.TraceStyle] {
  import verilator.Backend.CompilationSettings.TraceStyle
  val trace: TraceStyle = TraceStyle.Vcd(traceUnderscore)
}

/** Package object to expose the simulator settings */
package object simSettings {
  // Interface to the simulation
  val EnableTrace:               TraceVcd = TraceVcd(false)
  val EnableTraceWithUnderscore: TraceVcd = TraceVcd(true)
}
