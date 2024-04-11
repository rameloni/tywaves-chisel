package tywaves.simulator

import chisel3.RawModule

trait ParametricSimulatorInterface {
  private var simulator = new ParametricSimulator

  /** If true, the simulator will be reset before running each simulation */
  private var _resetSimulationBeforeRun = false

  /** Use this method to run a simulations */
  def simulate[T <: RawModule](
      module:   => T,
      settings: Seq[SimulatorSettings] = Seq(),
      simName:  String = "defaultSimulation",
  )(body: T => Unit): Unit = {
    if (_resetSimulationBeforeRun)
      reset()
    simulator.simulate(module, settings, simName)(body)
  }

  /**
   * Use this method to manually reset the simulator and run multiple
   * independent simulations
   */
  def reset(): Unit =
    simulator = new ParametricSimulator

  def resetBeforeEachRun(): Unit =
    _resetSimulationBeforeRun = true
}
