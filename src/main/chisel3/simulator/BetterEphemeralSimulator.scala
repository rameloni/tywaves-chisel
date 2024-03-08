package chisel3.simulator

import svsim._
import chisel3.RawModule
import chiselmapper.MapChiselToVcd


/**
 * A simulator that uses [[svsim]] and [[PeekPokeAPI]] to run a simulation.
 *
 * It offers the possibility to output trace vcd file using the verilator
 * backend.
 */
object BetterEphemeralSimulator extends PeekPokeAPI {
  import svsim.verilator.Backend.CompilationSettings.TraceStyle

  // Settings variable of the simulator
  private var _backendCompileSettings = verilator.Backend.CompilationSettings()
  private var _moduleDutName: String = ""
  private var _wantedWorkspacePath: String =
    Seq("test_run_dir", _moduleDutName, getClass.getSimpleName.stripSuffix("$")).mkString("/")

  /** Launch and execute a simulation given a list of [[Settings]]. */
  def simulate[T <: RawModule, S <: TraceStyle](
      module:   => T,
      settings: Seq[Settings[S]] = Seq(),
      simName:  String = "simulation",
  )(
      body: T => Unit
  ): Unit = {

    val mapChiselToVcd = new MapChiselToVcd(() => module)
//    mapChiselToVcd.printDebug()

    // Set the backend compile settings
    setBackendCompileSettings(settings)

    simulator.simulate(module) { simulatedModule =>
      // Set the controller settings
      setControllerSettings(simulatedModule.controller, settings)

      _wantedWorkspacePath =
        Seq(
          "test_run_dir",
          simulatedModule.elaboratedModule.wrapped.name,
          getClass.getSimpleName.stripSuffix("$"),
          simName,
        ).mkString("/")

      // Execute the simulation and return the result
      body(simulatedModule.elaboratedModule.wrapped)
    }.result

    // Cleanup the simulation after the execution
    simulator.cleanup()
  }

  /**
   * Set the backend compile settings. It sets settings such as the trace style.
   */
  private def setBackendCompileSettings[T](settings: Seq[Settings[T]]): Unit =
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
      case _ =>
    }

  /**
   * Set the controller settings. It sets settings such as the trace output
   * enable.
   */
  private def setControllerSettings[T](controller: Simulation.Controller, settings: Seq[Settings[T]]): Unit =
    settings.foreach {
      case TraceVcd(_) => controller.setTraceEnabled(true)
      case s           => println(s"Unknown setting $s")
    }

  // Simulators: DefaultSimulators
  private class DefaultSimulator(val workspacePath: String) extends SingleBackendSimulator[verilator.Backend] {
    val backend                            = verilator.Backend.initializeFromProcessEnvironment()
    val tag                                = "default"
    val commonCompilationSettings          = CommonCompilationSettings()
    val backendSpecificCompilationSettings = _backendCompileSettings

    println("Backend specific compilation settings: " + backendSpecificCompilationSettings)

    /** Cleanup the simulation */
    def cleanup(): Unit = {
      val tmpDir  = os.Path(workspacePath)
      val workDir = os.pwd / os.RelPath(_wantedWorkspacePath)

      // os.move.into(tmpDir, workDir, replaceExisting = true, createFolders = true, atomicMove = true)
      if (os.exists(workDir)) os.remove.all(workDir)
      os.move(tmpDir, workDir, replaceExisting = true, createFolders = true, atomicMove = true)
      println(s"Moving $tmpDir to $workDir")
    }
    //    sys.addShutdownHook {
    //        cleanup()
    //    }
  }

  private lazy val simulator: DefaultSimulator = {
    val temporaryDirectory = System.getProperty("java.io.tmpdir")
    val className          = getClass.getName.stripSuffix("$")
    //    val id = java.time.LocalDateTime.now()
    //      .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss"))
    val id = java.lang.management.ManagementFactory.getRuntimeMXBean.getName
    _wantedWorkspacePath = Seq(temporaryDirectory, className, id).mkString("/")
    new DefaultSimulator(_wantedWorkspacePath)
  }
}
