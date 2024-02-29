package chisel3.simulator

import svsim._
import chisel3.RawModule

private trait Settings[T] {
  val trace: T
}

private[simulator] case class TraceVcd(traceUnderscore: Boolean = false)
    extends Settings[verilator.Backend.CompilationSettings.TraceStyle] {
  val trace = verilator.Backend.CompilationSettings.TraceStyle.Vcd(traceUnderscore)
}

package object settings {
  // Interface to the simulation
  val EnableTrace               = TraceVcd(false)
  val EnableTraceWithUnderscore = TraceVcd(true)
}

object BetterEphemeralSimulator extends PeekPokeAPI {
  import chisel3.simulator.settings._

  private var _backendCompileSettings = verilator.Backend.CompilationSettings()

  private var _moduleDutName: String = ""

  private var _wantedWorkspacePath: String =
    Seq("test_run_dir", _moduleDutName, getClass.getSimpleName.stripSuffix("$")).mkString("/")

  // Launch and execute a simulation
  def simulate[T <: RawModule, S](module: => T, settings: Seq[Settings[S]] = Seq())(
      body: T => Unit
  ): Unit =
    synchronized {
      setBackendCompileSettings(settings) // Set the generic settings
      simulator.simulate(module) { (controller, dut) =>
        setControllerSettings(controller, settings)
        println(_backendCompileSettings)
        _wantedWorkspacePath = Seq("test_run_dir", dut.name, getClass.getSimpleName.stripSuffix("$")).mkString("/")

        body(dut)
      }.result
    }

  // Set the backend compile settings
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

  // Set the controller settings
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
    println(backendSpecificCompilationSettings)
    sys.addShutdownHook {
      // Get this dir whole path
      println(s"This path of the program is ${new java.io.File(".").getCanonicalPath}")
      println(s"Moving $workspacePath to ${_wantedWorkspacePath}")
      Runtime.getRuntime.exec(Array("mkdir", "-p", _wantedWorkspacePath)).waitFor()
      Runtime.getRuntime.exec(Array("mv", workspacePath, _wantedWorkspacePath)).waitFor()
    }
  }

  private lazy val simulator: DefaultSimulator = {
    val temporaryDirectory = System.getProperty("java.io.tmpdir")
    val className          = getClass.getName.stripSuffix("$")
    val id = java.time.LocalDateTime.now()
      .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss"))
    _wantedWorkspacePath = Seq(temporaryDirectory, className, id).mkString("/")
    new DefaultSimulator(_wantedWorkspacePath)
  }
}
