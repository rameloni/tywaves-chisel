package chiselmapper
import chisel3.RawModule
import chisel3.simulator.{ChiselIRParser, CircuitParser, FirrtlIRParser}
import chisel3.stage.{ChiselCircuitAnnotation, ChiselGeneratorAnnotation}
import circt.stage.ChiselStage
import firrtl.AnnotationSeq
import firrtl.stage.FirrtlCircuitAnnotation
import upickle.default._

/** This object exposes the Convert phase used in ChiselStage */
private object TypedConverter {
  private lazy val converter   = new chisel3.stage.phases.Convert
  private lazy val chiselStage = new circt.stage.ChiselStage

  val args = Array("--target", "systemverilog", "--split-verilog")
  val defaultAnnotations = Seq(
    circt.stage.FirtoolOption("-disable-annotation-unknown")
    //      firrtl.options.TargetDirAnnotation(workspace.supportArtifactsPath),
  )

  def addFirrtlAnno(annotations: AnnotationSeq): AnnotationSeq =
    converter.transform(annotations)

  def getChiselStageAnno[T <: RawModule](generateModule: () => T): AnnotationSeq = {
    val annotations = Seq(ChiselGeneratorAnnotation(generateModule)) ++ defaultAnnotations
    chiselStage.execute(args, annotations) // execute returns the passThrough annotations in CIRCT transform stage
  }

}

/**
 * This class is used to map the Chisel IR to the VCD file.
 *
 * It is used to extract the Chisel IR and the Firrtl IR from the ChiselStage
 * and then use it to generate the VCD file.
 */
class MapChiselToVcd[T <: RawModule](generateModule: () => T, private val workingDir: String = "workingdir") {

  // Step 1. Get the annotation from the execution of ChiselStage and add the FirrtlCircuitAnnotation
  private val chiselStageAnno    = TypedConverter.getChiselStageAnno(generateModule)
  private val completeChiselAnno = TypedConverter.addFirrtlAnno(chiselStageAnno)

  // Step 2. Extract the chiselIR and firrtlIR from the annotations
  val circuitChiselIR = completeChiselAnno.collectFirst {
    case ChiselCircuitAnnotation(circuit) => circuit
  }.getOrElse {
    throw new Exception("Could not find ChiselCircuitAnnotation. It is expected after the ChiselStage")
  }
  val circuitFirrtlIR = completeChiselAnno.collectFirst {
    case FirrtlCircuitAnnotation(circuit) => circuit
  }.getOrElse {
    throw new Exception("Could not find firrtl.stage.FirrtlCircuitAnnotation. It is expected after the ChiselStage")
  }
  val x = 0

  val parser = new FirrtlIRParser
  parser.parse(circuitFirrtlIR)
  parser.dumpMaps("FirrtlIRParsing.log")
  val chiselParser = new ChiselIRParser
  chiselParser.parse(circuitChiselIR)
  chiselParser.dumpMaps("ChiselIRParsing.log")


  def printDebug(): Unit = {
    println("Chisel Stage Annotations:")
    println(chiselStageAnno)
    println("Chisel IR:")
    println(circuitChiselIR)
    println()
    println("Firrtl IR:")
    println(circuitFirrtlIR)
    println(circuitFirrtlIR.serialize)

  }

//  apply()
}
