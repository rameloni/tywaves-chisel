package tywaves.circuitmapper

import chisel3.RawModule
import chisel3.stage.ChiselCircuitAnnotation
import chisel3.tywaves.circuitparser.{ChiselIRParser, FirrtlIRParser}
import firrtl.stage.FirrtlCircuitAnnotation
import tywaves.hglddparser.DebugIRParser
import tywaves.utils.UniqueHashMap

/**
 * This class is used to map the Chisel IR to the VCD file.
 *
 * It is used to extract the Chisel IR and the Firrtl IR from the ChiselStage
 * and then use it to generate the VCD file.
 */
class MapChiselToVcd[T <: RawModule](generateModule: () => T, private val workingDir: String = "workingDir") {

  val logSubDir = s"$workingDir/tywaves-log"
  // Step 1. Get the annotation from the execution of ChiselStage and add the FirrtlCircuitAnnotation
  private val chiselStageAnno    = TypedConverter.getChiselStageAnno(generateModule, workingDir = logSubDir)
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

  // Step 3. Parse the ChiselIR and FirrtlIR. In this step modules, ports, and internal elements are extracted
  //  They are stored in UniqueHashMaps and associated with their respective elementIds
  val firrtlIRParser = new FirrtlIRParser
  val chiselIRParser = new ChiselIRParser

  firrtlIRParser.parseCircuit(circuitFirrtlIR)
  chiselIRParser.parseCircuit(circuitChiselIR)

  // Step 4. Parse the debug information
  val gDebugIRParser = new DebugIRParser(workingDir, TypedConverter.getDebugIRFile(gOpt = true))
  val debugIRParser =
    new DebugIRParser(workingDir, TypedConverter.getDebugIRFile(gOpt = false)) // TODO: check if this is needed or not

  gDebugIRParser.parse()
  debugIRParser.parse()

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

  /** This method is used to dump the parsed maps to the log directory. */
  def dumpLog(): Unit = {
    firrtlIRParser.dumpMaps(s"$logSubDir/FirrtlIRParsing.log")
    chiselIRParser.dumpMaps(s"$logSubDir/ChiselIRParsing.log")

    gDebugIRParser.dump(s"$logSubDir/gDebugIRParser.log")
    debugIRParser.dump(s"$logSubDir/DebugIRParser.log")
  }

  /**
   * This method is used to map the Chisel IR to the VCD file. It tries to
   * associate each element of Chisel IR with Firrtl IR. If an element of the
   * next IR is not found a None is returned for the missing IR. Thus the output
   * will be something similar to:
   *   - `(Some(..), Some(..))`
   *   - `(Some(..), None)`
   *   - `(None, Some(..))`
   */
  def mapCircuits(): Unit = {

    def joinAndDump[K, V1, V2](
        map1: UniqueHashMap[K, V1],
        map2: UniqueHashMap[K, V2],
    )(dumpfile: String, append: Boolean = true): Unit = {
//      val join = map1.keySet.intersect(map2.keySet)
      val join = map1.keySet.union(map2.keySet)
        .map { key =>
          (key, (map1.get(key), map2.get(key)))
        }.toMap

      val bw = new java.io.BufferedWriter(new java.io.FileWriter(dumpfile, append))
      join.foreach {
        case (elId, (firrtlIR, chiselIR)) =>
          bw.write(s"elId: $elId\n" +
            s"\tchiselIR: $chiselIR\n" +
            s"\tfirrtlIR: $firrtlIR\n")
      }
      bw.close()
    }

    // Map modules
    firrtlIRParser.modules.zip(chiselIRParser.modules).foreach {
      case ((firrtlElId, (firrtlName, firrtlModule)), (chiselElId, (chiselName, chiselModule))) =>
        println(s"firrtlElId: $firrtlElId, firrtlName: $firrtlName")
        println(s"chiselElId: $chiselElId, chiselName: $chiselName")
    }
    println("Joined Modules:")
    joinAndDump(firrtlIRParser.modules, chiselIRParser.modules)(s"$logSubDir/JoinedModules.log")
    println("\n-----------------------------")

    println("Joined Ports FirrtlIR:")
    joinAndDump(firrtlIRParser.ports, chiselIRParser.ports)(s"$logSubDir/JoinedPorts.log")

    println("\n-----------------------------")

    println("Joined Flattened Ports FirrtlIR:")
    joinAndDump(firrtlIRParser.flattenedPorts, chiselIRParser.flattenedPorts)(s"$logSubDir/JoinedFlattenedPorts.log")

    println("\n-----------------------------")

    println("Joined All Elements FirrtlIR:")
    joinAndDump(firrtlIRParser.allElements, chiselIRParser.allElements)(s"$logSubDir/JoinedAllElements.log")

  }
}
