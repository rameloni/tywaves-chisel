package tywaves.simulator

import chisel3.RawModule
import chisel3.simulator.PeekPokeAPI
import svsim.Workspace
import tywaves.circuitmapper.TypedConverter

object TywavesSimulator extends PeekPokeAPI {

  private[simulator] case class Tywaves(runWaves: Boolean) extends SimulatorSettings
  val WithTywavesWaveforms: Boolean => Tywaves = (runWaves: Boolean) => Tywaves(runWaves)

  private var simulator = new TywavesSimulator

  /** If true, the simulator will be reset before running each simulation */
  private var _resetSimulationBeforeRun = false

  /** Use this method to run a simulation */
  def simulate[T <: RawModule](
      module:   => T,
      settings: Seq[SimulatorSettings] = Seq(),
      simName:  String = "defaultSimulation",
  )(body: T => Unit): Unit = {

    // Create a new simulator instance
    val simulator = new TywavesSimulator

    val containTywaves = settings.exists(_.isInstanceOf[Tywaves])

    val finalSettings =
      if (containTywaves)
        settings ++ Seq(FirtoolArgs(Seq("-O=debug", "-g", "--emit-hgldd", "--split-verilog", "-o=WORK.v")))
      else settings

    simulator.simulate(module, finalSettings, simName)(body)

    if (simulator.finalTracePath.nonEmpty && containTywaves) {
      // Get the extra scopes created by ChiselSim backend: TOP, svsimTestbench, dut
      val extraScopes = Seq("TOP", Workspace.testbenchModuleName, "dut")

      // Create the debug info from the firtool and get the top module name
      // TODO: this may not be needed anymore, since the debug info can be generated directly from chiselsim, by giving the right options to firtool
      // But the problem is to call chiselstage with debug options
      TypedConverter.createDebugInfoHgldd(() => module, simulator.wantedWorkspacePath)

      // Run tywaves viewer if the Tywaves waveform generation is enabled by Tywaves(true)
      if (finalSettings.contains(Tywaves(true)))
        TywavesInterface.run(
          vcdPath = simulator.finalTracePath.get,
          hglddDirPath = Some(TypedConverter.getDebugInfoDir(gOpt = true)),
          extraScopes = extraScopes,
          topModuleName = TypedConverter.getTopModuleName,
        )
    } else if (containTywaves)
      throw new Exception("Tywaves waveform generation requires a trace file. Please enable VcdTrace.")

  }

  /**
   * Use this method to manually reset the simulator and run multiple
   * independent simulations
   */
  @deprecated
  def reset(): Unit =
    simulator = new TywavesSimulator

  @deprecated
  def resetBeforeEachRun(): Unit =
    _resetSimulationBeforeRun = true
}
class TywavesSimulator extends ParametricSimulator
