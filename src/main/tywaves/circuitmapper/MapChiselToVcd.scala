package tywaves.circuitmapper

import chisel3.RawModule
import chisel3.stage.ChiselCircuitAnnotation
import chisel3.tywaves.circuitparser.{ChiselIRParser, FirrtlIRParser}
import firrtl.stage.FirrtlCircuitAnnotation

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

  val parser       = new FirrtlIRParser
  val chiselParser = new ChiselIRParser

  parser.parseCircuit(circuitFirrtlIR)
  chiselParser.parseCircuit(circuitChiselIR)

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

  def dumpLog(): Unit = {
    val logSubDir = s"$workingDir/tywaves-log"
    parser.dumpMaps(s"$logSubDir/FirrtlIRParsing.log")
    chiselParser.dumpMaps(s"$logSubDir/ChiselIRParsing.log")
  }
}
