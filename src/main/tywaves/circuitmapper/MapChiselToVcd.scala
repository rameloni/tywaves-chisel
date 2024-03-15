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
   * will be something similar to:Seq
   *   - `(Some(..), Some(..))`
   *   - `(Some(..), None)`
   *   - `(None, Some(..))`
   */
  def mapCircuits(): Unit = {

    // Your processing logic here
    def joinAndDump[K](listMaps: Seq[(String, UniqueHashMap[K, ?])])(dumpFile: String): Unit = {

      val join = listMaps.map(_._2.keySet).reduce(_ union _)
        .map { key =>
          (key, listMaps.map { case (name, map) => (name, map.get(key)) })
        }.toMap

      println(join)

      val bw = new java.io.BufferedWriter(new java.io.FileWriter(dumpFile))
      join.foreach {
        case (elId, list) =>
          bw.write(s"elId: $elId\n")
          list.foreach {
            case (name, Some(value)) =>
              bw.write(s"\t$name: $value\n")

              // Check if value is the one with SystemVerilog names
              value match {
                case (Name(_, _), Direction(_), HardwareType(_), Type(_), VerilogSignals(names)) =>
                  bw.write(s"\t\"SystemVerilogNames and (maybe) VCD\": ${ujson.write(names)}\n")
                case _ =>
              }
            case (name, None) =>
              bw.write(s"\t$name: None\n")
          }
      }
      bw.close()
    }

    // Map modules
    firrtlIRParser.modules.zip(chiselIRParser.modules).foreach {
      case ((firrtlElId, (firrtlName, firrtlModule)), (chiselElId, (chiselName, chiselModule))) =>
        println(s"firrtlElId: $firrtlElId, firrtlName: $firrtlName")
        println(s"chiselElId: $chiselElId, chiselName: $chiselName")
    }

    joinAndDump(Seq(
      ("chiselIR", chiselIRParser.modules),
      ("firrtlIR", firrtlIRParser.modules),
      ("debugIR", gDebugIRParser.modules),
    ))(s"$logSubDir/JoinedModules.log")

    joinAndDump(Seq(
      ("chiselIR", chiselIRParser.ports),
      ("firrtlIR", firrtlIRParser.ports),
      ("debugIR", gDebugIRParser.ports),
    ))(s"$logSubDir/JoinedPorts.log")

    joinAndDump(Seq(
      ("chiselIR", chiselIRParser.flattenedPorts),
      ("firrtlIR", firrtlIRParser.flattenedPorts),
      ("debugIR", gDebugIRParser.flattenedPorts),
    ))(s"$logSubDir/JoinedFlattenedPorts.log")

    joinAndDump(Seq(
      ("chiselIR", chiselIRParser.allElements),
      ("firrtlIR", firrtlIRParser.allElements),
      ("debugIR", gDebugIRParser.allElements),
    ))(s"$logSubDir/JoinedAllElements.log")

    joinAndDump(Seq(
      ("chiselIR", chiselIRParser.allElements),
      ("firrtlIR", firrtlIRParser.allElements),
      ("debugIR", gDebugIRParser.signals),
    ))(s"$logSubDir/JoinedSignals.log")

  }
}
