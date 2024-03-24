package tywaves.simulator

import svsim.verilator

/** Trait to represent the simulator settings */
protected trait SimulatorSettings

/**
 * [[TraceVcd]] is an abstracted representation of the
 * [[verilator.Backend.CompilationSettings.TraceStyle.Vcd]] that can be used by
 * the user of the simulator
 */
private[simulator] case class TraceVcd(traceUnderscore: Boolean = false)
    extends SimulatorSettings {
  import verilator.Backend.CompilationSettings.TraceStyle
  val trace: TraceStyle = TraceStyle.Vcd(traceUnderscore)
}

private[simulator] case class Tywaves() extends SimulatorSettings

/** Package object to expose the simulator settings */
package object simSettings {
  // Interface to the simulation
  val EnableTrace:               TraceVcd = TraceVcd(false)
  val EnableTraceWithUnderscore: TraceVcd = TraceVcd(true)
  val LaunchTywavesWaveforms:    Tywaves  = Tywaves()
}
