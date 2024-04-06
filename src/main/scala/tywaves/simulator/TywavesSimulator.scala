package tywaves.simulator

import chisel3.RawModule
import chisel3.simulator.PeekPokeAPI
import svsim.Workspace
import tywaves.circuitmapper.MapChiselToVcd

object TywavesSimulator extends PeekPokeAPI {

  private[simulator] case class Tywaves(runWaves: Boolean) extends SimulatorSettings
  val WithTywavesWaveforms: Boolean => Tywaves = (runWaves: Boolean) => Tywaves(runWaves)

  private var simulator = new TywavesSimulator

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

    val containTywaves = settings.exists(_.isInstanceOf[Tywaves])
    if (simulator.finalTracePath.nonEmpty && containTywaves) {

      val mapChiselToVcd = new MapChiselToVcd(() => module, workingDir = simulator.wantedWorkspacePath)(
        topName = "TOP",
        tbScopeName = Workspace.testbenchModuleName,
        dutName = "dut",
      )
      mapChiselToVcd.createTywavesState()

      if (settings.contains(Tywaves(true)))
        TywavesInterface.run(simulator.finalTracePath.get, Some(mapChiselToVcd.tywavesStatePath))

    } else if (containTywaves)
      throw new Exception("Tywaves waveform generation requires a trace file. Please enable VcdTrace.")

  }

  /**
   * Use this method to manually reset the simulator and run multiple
   * independent simulations
   */
  def reset(): Unit =
    simulator = new TywavesSimulator

  def resetBeforeEachRun(): Unit =
    _resetSimulationBeforeRun = true
}
class TywavesSimulator extends ParametricSimulator
