package tywaves.circuitmapper

import chisel3.RawModule
import chisel3.stage.ChiselGeneratorAnnotation
import tywaves.BuildInfo.firtoolBinaryPath

/** This case class is used to store the top module name */
private[tywaves] case class TopModuleName(private[circuitmapper] var name: Option[String])

/** This object contains methods to create debug information from firtool */
private[tywaves] object TypedConverter {
  private lazy val chiselStage = new circt.stage.ChiselStage(withDebug = true)

  private val chiselStageBaseArgs =
    Array("--target", "systemverilog", "--split-verilog", "--firtool-binary-path", firtoolBinaryPath)

  val firtoolBaseArgs: Seq[String] =
    Seq("-O=debug", "-g", "--emit-hgldd", "--split-verilog") // Run in debug mode compiled with optimizations
  // Directories where the debug information is stored
  private var hglddDebugDir   = "hgldd/debug"
  private var hglddWithOptDir = "hgldd/opt" // TODO: remove
  private var workingDir: Option[String] = None
  private val topModuleName = TopModuleName(None)

  // Default firtool options encoded as annotations for ChiselStage
  private val defaultFirtoolOptAnno = createFirtoolOptions(firtoolBaseArgs)
  //      "-disable-annotation-unknown",
  //      "--hgldd-output-prefix=<path>",
  /*,"--output-final-mlir=WORK.mlir"*/

  // Map any sequence of string into FirtoolOptions
  private def createFirtoolOptions(args: Seq[String]) = args.map(circt.stage.FirtoolOption)

  /**
   * Create debug information from firtool by using the
   * [[circt.stage.ChiselStage]]
   */
  def createDebugInfoHgldd[T <: RawModule](
      generateModule:        () => T,
      workingDir:            String = "workingDir",
      additionalFirtoolArgs: Seq[String],
  ): Unit = {
    this.workingDir = Some(workingDir)
    hglddWithOptDir = workingDir + "/" + hglddWithOptDir
    hglddDebugDir = workingDir + "/" + hglddDebugDir
    //    hglddDebugDir = workingDir + "/" + "support-artifacts"

    // Annotations for ChiselStage
    val annotations = Seq(ChiselGeneratorAnnotation(generateModule)) ++ defaultFirtoolOptAnno

    // Run without debug mode: TODO check it is actually not needed
    //    chiselStage.execute(
    //      chiselStageBaseArgs ++ Array("--target-dir", hglddWithOptDir),
    //      annotations,
    //    ) // execute returns the passThrough annotations in CIRCT transform stage

    val finalAnno = chiselStage.execute(
      chiselStageBaseArgs ++ Array("--target-dir", hglddDebugDir),
      createFirtoolOptions(additionalFirtoolArgs) ++ annotations ,
    ) // execute returns the passThrough annotations in CIRCT transform stage

    // Get the module name
    topModuleName.name = finalAnno.collectFirst {
      case chisel3.stage.ChiselCircuitAnnotation(circuit) => circuit.name
    }
  }

  /** Get the directory where the debug information is stored */
  def getDebugInfoDir(gOpt: Boolean): String =
    if (gOpt) hglddDebugDir
    else hglddWithOptDir

  /**
   * Return the working directory used in the last call of
   * [[createDebugInfoHgldd]]
   */
  def getWorkingDir: Option[String] = workingDir

  /**
   * Return the top module name of the last circuit used in the last call of
   * [[createDebugInfoHgldd]]
   */
  def getTopModuleName: Option[String] = topModuleName.name

}
