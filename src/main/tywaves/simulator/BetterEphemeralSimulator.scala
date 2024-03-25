package tywaves.simulator

import chisel3.RawModule
import chisel3.simulator.withargs.SingleBackendSimulatorWithArgs
import chisel3.simulator.PeekPokeAPI
import svsim._
import tywaves.circuitmapper.MapChiselToVcd

/**
 * A simulator that uses [[svsim]] and [[PeekPokeAPI]] to run a simulation.
 *
 * It offers the possibility to output trace vcd file using the verilator
 * backend.
 */
object BetterEphemeralSimulator extends PeekPokeAPI {

  // Settings variable of the simulator
  private var _backendCompileSettings = verilator.Backend.CompilationSettings()
  private var _withWaveforms          = false
  private val _testRunDir             = "test_run_dir"
  private var _moduleDutName          = ""
  private val _simulatorName          = getClass.getSimpleName.stripSuffix("$")
  private var _simName                = "simulation"
  private def _wantedWorkspacePath =
    Seq(_testRunDir, _moduleDutName, _simulatorName, _simName).mkString("/")

  /** Launch and execute a simulation given a list of [[SimulatorSettings]]. */
  def simulate[T <: RawModule](
      module:   => T,
      settings: Seq[SimulatorSettings] = Seq(),
      simName:  String = "simulation",
  )(
      body: T => Unit
  ): Unit = {

    // Set the backend compile settings
    setBackendCompileSettings(settings)

    simulator.simulate(module) { simulatedModule =>
      // Set the controller settings
      setControllerSettings(simulatedModule.controller, settings)

      // Update the wanted workspace path
      _moduleDutName = simulatedModule.wrapped.name
      _simName = simName

      // Launch the actual simulation and return the result
      body(simulatedModule.wrapped)
    }.result

    // Cleanup the simulation after the execution
    simulator.cleanup()
    println(_wantedWorkspacePath)
    val mapChiselToVcd = new MapChiselToVcd(() => module, workingDir = _wantedWorkspacePath)(
      topName = "TOP",
      tbScopeName = Workspace.testbenchModuleName,
      dutName = "dut",
    )

    mapChiselToVcd.dumpLog()
    mapChiselToVcd.mapCircuits()
    mapChiselToVcd.createTywavesState()

    if (_withWaveforms) {
      val vcdPath         = s"${_wantedWorkspacePath}/workdir-default/trace.vcd"
      val chiselStatePath = mapChiselToVcd.tywavesStatePath
      TywavesInterface(vcdPath, Some(chiselStatePath))
    }
  }

  /**
   * Set the backend compile settings. It sets settings such as the trace style.
   */
  private def setBackendCompileSettings(settings: Seq[SimulatorSettings]): Unit = {
    settings.foreach {
      case t: TraceVcd =>
        _backendCompileSettings =
          verilator.Backend.CompilationSettings(
            traceStyle = Some(t.trace),
            outputSplit = None,
            outputSplitCFuncs = None,
            disabledWarnings = Seq(),
            disableFatalExitOnWarnings = false,
          )
      case _: Tywaves => _withWaveforms = true
      case _ =>
    }

    // Check if the trace style is set and tywaves is enabled
    if (_backendCompileSettings.traceStyle.isEmpty && _withWaveforms) {
      throw new Exception("Trace style must be set before enabling Tywaves")
    }
  }

  /**
   * Set the controller settings. It sets settings such as the trace output
   * enable.
   */
  private def setControllerSettings(controller: Simulation.Controller, settings: Seq[SimulatorSettings]): Unit =
    settings.foreach {
      case TraceVcd(_) => controller.setTraceEnabled(true)
      case _: Tywaves =>
      case s => println(s"Unknown Controller Setting $s")
    }

  // Simulators: DefaultSimulators
  private class DefaultSimulator(val workspacePath: String) extends SingleBackendSimulatorWithArgs[verilator.Backend] {
    val backend:     verilator.Backend = verilator.Backend.initializeFromProcessEnvironment()
    val tag:         String            = "default"
    val firtoolArgs: Seq[String]       = Seq("-g", "--emit-hgldd")

    val backendSpecificCompilationSettings: verilator.Backend.CompilationSettings = _backendCompileSettings
    val commonCompilationSettings: CommonCompilationSettings =
      CommonCompilationSettings(
        optimizationStyle = CommonCompilationSettings.OptimizationStyle.OptimizeForCompilationSpeed,
        availableParallelism = CommonCompilationSettings.AvailableParallelism.UpTo(4),
        defaultTimescale = Some(CommonCompilationSettings.Timescale.FromString("1ms/1ms")),
      )

    /**
     * Cleanup the simulation and move the simulation workspace to the wanted
     * workspace path.
     */
    def cleanup(): Unit = {
      val tmpDir  = os.Path(workspacePath)
      val workDir = os.pwd / os.RelPath(_wantedWorkspacePath)

      // os.move.into(tmpDir, workDir, replaceExisting = true, createFolders = true, atomicMove = true)
      if (os.exists(workDir)) os.remove.all(workDir)
      os.move(tmpDir, workDir, replaceExisting = true, createFolders = true, atomicMove = true)
    }

  }

  private lazy val simulator: DefaultSimulator = {

    val temporaryDirectory = System.getProperty("java.io.tmpdir")
    val className          = getClass.getName.stripSuffix("$")
    val id                 = java.lang.management.ManagementFactory.getRuntimeMXBean.getName

    val tmpWorkspacePath = Seq(temporaryDirectory, className, id).mkString("/")
    new DefaultSimulator(tmpWorkspacePath)
  }
}
