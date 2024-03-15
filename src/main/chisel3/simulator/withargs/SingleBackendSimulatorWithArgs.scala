package chisel3.simulator.withargs

import chisel3.RawModule
import chisel3.simulator.{SimulatedModule, Simulator}
import svsim.{Backend, CommonCompilationSettings, Workspace}

trait SingleBackendSimulatorWithArgs[B <: Backend] extends Simulator {

  val backend: B

  def tag: String

  def commonCompilationSettings: CommonCompilationSettings

  def backendSpecificCompilationSettings: backend.CompilationSettings

  final def processBackends(processor: Simulator.BackendProcessor): Unit =
    processor.process(backend)(tag, commonCompilationSettings, backendSpecificCompilationSettings)

  val firtoolArgs: Seq[String]

  override private[simulator] def _simulate[T <: RawModule, U](
      module: => T
  )(body: (SimulatedModule[T]) => U): Seq[Simulator.BackendInvocationDigest[U]] = {
    val workspace = new Workspace(path = workspacePath, workingDirectoryPrefix = workingDirectoryPrefix)
    workspace.reset()
    val elaboratedModule = workspace.elaborateGeneratedModule(() => module)(firtoolArgs)
    workspace.generateAdditionalSources()
    val compiler = new Simulator.WorkspaceCompiler(
      elaboratedModule,
      workspace,
      customSimulationWorkingDirectory,
      verbose,
      { (module: SimulatedModule[T]) =>
        val outcome = body(module)
        module.completeSimulation()
        outcome
      },
    )
    processBackends(compiler)
    compiler.results.toSeq
  }

  def simulate[T <: RawModule, U](
      module: => T
  )(body: (SimulatedModule[T]) => U): Simulator.BackendInvocationDigest[U] =
    _simulate(module)(body).head

}
