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

  def joinMapCircuits[K](listMaps: Seq[(String, UniqueHashMap[K, ?])]): Map[K, Seq[(String, Option[?])]] =
    listMaps.map(_._2.keySet).reduce(_ union _)
      .map { key =>
        (key, listMaps.map { case (name, map) => (name, map.get(key)) })
      }.toMap

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
                case (Name(_, _, _), Direction(_), HardwareType(_, _), Type(_), VerilogSignals(names)) =>
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

  def createTywavesState(): Unit = {
    import io.circe.syntax._
    import java.io.PrintWriter
    import tywaves_symbol_table._
    import tywaves_symbol_table.tywaves_encoders._

    val groupIrPerElement: Map[ElId, Seq[(String, Option[?])]] = joinMapCircuits(
      Seq(
        ("chiselIR", chiselIRParser.allElements),
        ("firrtlIR", firrtlIRParser.allElements),
        ("debugIR", gDebugIRParser.signals),
      )
    )

    // Each element is associated to the 3 IRs
    // The goal here is to create a Tywavestate
    var scopes = Seq.empty[Scope]
    groupIrPerElement.foreach {
      case (elId, irs) =>
        println()
        println("ElId: " + elId)
        println("Found elements: " + irs.size)
        // real tywavescope: the one in chiselIR
        val realTywaveScope = irs.filter {
          case (ir, _) => ir == "chiselIR"
        }.head match {
          case (_, Some((Name(_, _, tywaveScope), Direction(_), Type(_)))) =>
            tywaveScope
          case other =>
            throw new NotImplementedError(s"This branch shouldn't be reached. $other")
        }
        // Iterate over the IRs
        irs.foreach {
          case (ir, Some(_value)) =>
            val value = _value
            val scope =
              tywaves_symbol_table.Scope(
                realTywaveScope,
                findChildVariables(
                  value,
                  ir,
                  gDebugIRParser.signals.values.toSeq,
                  chiselIRParser.allElements.values.toSeq,
                ),
                findChildScopes(value),
              )
            scopes = scopes :+ scope
            println(scope)
          case (ir, None) => println(s"IR: $ir", None)
        }
    }

    // Output the TywavesState in json
    val tywaveState = tywaves_symbol_table.TywaveState(mergeScopes(scopes)).asJson
    val printWriter = new PrintWriter(s"$logSubDir/tywavesState.json")
    printWriter.write(tywaveState.spaces2) // Use spaces2 to format the JSON with indentation
    printWriter.close()

  }

  /// Merge common scopes together
  def mergeScopes(scopes: Seq[tywaves_symbol_table.Scope]): Seq[tywaves_symbol_table.Scope] = {
    // Join all the scopes with the same names
    val groupScopes = scopes.groupBy(_.name)

    groupScopes.map(
      _._2.reduce((a, b) =>
        tywaves_symbol_table.Scope(
          a.name,
          a.childVariables ++ b.childVariables,
          a.childScopes ++ b.childScopes,
        )
      )
    ).toSeq
  }

  /// Find a tywave scope
  private def findTywaveScope[Tuple](tuple: Tuple): String =
    tuple match {
      case (Name(_, _, tywaveScope), Direction(_), HardwareType(_, _), Type(_), VerilogSignals(_)) => tywaveScope
      case (Name(_, _, tywaveScope), Direction(_), Type(_))                                        => tywaveScope
      case other =>
        throw new NotImplementedError("This branch shouldn't be reached.")
    }

  private def findChiselTypeName[Tuple](nameGuess: String, listChiselInfo: Seq[Tuple]): String =
    listChiselInfo.filter {
      case (Name(name, scope, _), Direction(_), Type(_)) =>
        if (name == nameGuess) true else false
      case _ => false
    }.head match {
      case (Name(_, _, _), Direction(_), Type(typeName)) => typeName
      case _ => throw new NotImplementedError("This branch shouldn't be reached.")
    }

  /// Find the child variables of a given tuple for a given representation
  private def findChildVariables[Tuple](
      tuple:          Tuple,
      ir:             String,
      listVcdInfo:    Seq[Tuple],
      listChiselInfo: Seq[Tuple],
  ): Seq[tywaves_symbol_table.Variable] = {
    var childVariables = Seq.empty[tywaves_symbol_table.Variable]
    if (ir == "debugIR")
      tuple match {
        case (
              Name(name, parentScope, tywaveScope),
              Direction(dir),
              HardwareType(hardwareType, size),
              Type(_),
              VerilogSignals(verilogSignals),
            ) =>
          val listChildren = listVcdInfo.filter {
            case (
                  Name(childName, scope, _),
                  Direction(_),
                  HardwareType(_, size),
                  Type(_),
                  VerilogSignals(_),
                ) =>
              val nameWithScope = parentScope + "_" + name
              if (scope == nameWithScope) true else false
            case _ => false
          } // If there's only one signal, it's a leaf: however the leaf can be a reference to a bundle and not only a ground signal
          if (verilogSignals.length <= 1 && listChildren.isEmpty) {
            childVariables = childVariables :+ tywaves_symbol_table.Variable(
              name,
              findChiselTypeName(name, listChiselInfo),
              tywaves_symbol_table.hwtype.from_string(hardwareType, Some(dir)),
              realType = tywaves_symbol_table.realtype.Ground(size.getOrElse(0), verilogSignals.head),
            )
          } else {

//            val listChildrenChisel = listChiselInfo.filter {
//              case (
//                    Name(_, scope, _),
//                    Direction(_),
//                    Type(_),
//                  ) =>
//                val nameWithScope = tywaveScope + "_" + name
//                if (scope == nameWithScope) true else false
//              case _ => false
//            }
            var subVariables = Seq.empty[tywaves_symbol_table.Variable]
            listChildren.foreach { child =>
              subVariables = subVariables ++ findChildVariables(child, ir, listVcdInfo, listChiselInfo)
            }

            childVariables = childVariables ++ Seq(tywaves_symbol_table.Variable(
              name,
              findChiselTypeName(name, listChiselInfo),
              tywaves_symbol_table.hwtype.from_string(hardwareType, Some(dir)),
              realType = tywaves_symbol_table.realtype.Bundle(
                subVariables,
                vcdName = Some(name),
              ),
            ))
          }
          childVariables
        case (Name(_, _, _), Direction(_), Type(_)) =>
          childVariables
        case other =>
          throw new NotImplementedError("This branch shouldn't be reached.")
      }
    else {
      Seq.empty
    }
  }

  private def findChildScopes[Tuple](value: Tuple): Seq[tywaves_symbol_table.Scope] =
    Seq.empty
}
