package tywaves.circuitmapper

import chisel3.RawModule
import chisel3.stage.ChiselGeneratorAnnotation
import firrtl.AnnotationSeq

/** This object exposes the Convert phase used in ChiselStage */
private object TypedConverter {
  private lazy val converter   = new chisel3.stage.phases.Convert
  private lazy val chiselStage = new circt.stage.ChiselStage

  private val args = Array("--target", "systemverilog", "--split-verilog")
  private val defaultAnnotations = Seq(
    circt.stage.FirtoolOption("-disable-annotation-unknown")
    //      firrtl.options.TargetDirAnnotation(workspace.supportArtifactsPath),
  )

  def addFirrtlAnno(annotations: AnnotationSeq): AnnotationSeq =
    converter.transform(annotations)

  def getChiselStageAnno[T <: RawModule](generateModule: () => T, workingDir: String = "workingDir"): AnnotationSeq = {
    val annotations = Seq(ChiselGeneratorAnnotation(generateModule)) ++ defaultAnnotations
    chiselStage.execute(
      args ++ Array("--target-dir", workingDir),
      annotations,
    ) // execute returns the passThrough annotations in CIRCT transform stage
  }

}
