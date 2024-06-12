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

  /** Use this method to run a simulations */
  def simulate[T <: RawModule](
      module:   => T,
      settings: Seq[SimulatorSettings] = Seq(),
      simName:  String = "defaultSimulation",
  )(body: T => Unit): Unit = {
    if (_resetSimulationBeforeRun)
      reset()

    val containTywaves = settings.exists(_.isInstanceOf[Tywaves])

    val finalSettings =
      if (containTywaves)
        settings ++ Seq(FirtoolArgs(Seq("-O=debug", "-g", "--emit-hgldd", "--split-verilog", "-o=WORK.v")))
      else settings
    simulator.simulate(module, finalSettings, simName)(body)

    if (simulator.finalTracePath.nonEmpty && containTywaves) {

//      val mapChiselToVcd = new MapChiselToVcd(() => module, workingDir = simulator.wantedWorkspacePath)(
//        topName = "TOP",
//        tbScopeName = Workspace.testbenchModuleName,
//        dutName = "dut",
//      )
//      mapChiselToVcd.createTywavesState()
      // in the trace file
      // Add the scopes to the module: TOP, svsimTestbench, dut

      val extraScopes = Seq("TOP", Workspace.testbenchModuleName, "dut")

      val anno = TypedConverter.getChiselStageAnno(() => module, simulator.wantedWorkspacePath)
      // Find the top module name
      val topModuleName = anno.collectFirst {
        case chisel3.stage.ChiselCircuitAnnotation(circuit) => circuit.name
      }

      if (finalSettings.contains(Tywaves(true)))
        TywavesInterface.run(
          simulator.finalTracePath.get,
          Some(TypedConverter.getDebugIRDir(gOpt = true)),
          extraScopes,
          topModuleName,
        )
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
