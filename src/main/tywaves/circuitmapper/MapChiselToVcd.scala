package tywaves.circuitmapper

import chisel3.RawModule
import chisel3.stage.ChiselCircuitAnnotation
import chisel3.tywaves.circuitparser.{ChiselIRParser, FirrtlIRParser}
import firrtl.stage.FirrtlCircuitAnnotation
import tywaves.hglddparser.DebugIRParser
import tywaves.utils.UniqueHashMap

import scala.annotation.unused

/**
 * This class is used to map the Chisel IR to the VCD file.
 *
 * It is used to extract the Chisel IR and the Firrtl IR from the ChiselStage
 * and then use it to generate the VCD file.
 */
class MapChiselToVcd[T <: RawModule](generateModule: () => T, private val workingDir: String = "workingDir")(
    topName:     String,
    tbScopeName: String,
    dutName:     String,
) {

  private val logSubDir = s"$workingDir/tywaves-log"
  val tywavesStatePath  = s"$logSubDir/tywavesState.json"

  // Step 1. Get the annotation from the execution of ChiselStage and add the FirrtlCircuitAnnotation
  private val chiselStageAnno    = TypedConverter.getChiselStageAnno(generateModule, workingDir = logSubDir)
  private val completeChiselAnno = TypedConverter.addFirrtlAnno(chiselStageAnno)

  // Step 2. Extract the chiselIR and firrtlIR from the annotations
  private val circuitChiselIR = completeChiselAnno.collectFirst {
    case ChiselCircuitAnnotation(circuit) => circuit
  }.getOrElse {
    throw new Exception("Could not find ChiselCircuitAnnotation. It is expected after the ChiselStage")
  }
  private val circuitFirrtlIR = completeChiselAnno.collectFirst {
    case FirrtlCircuitAnnotation(circuit) => circuit
  }.getOrElse {
    throw new Exception("Could not find firrtl.stage.FirrtlCircuitAnnotation. It is expected after the ChiselStage")
  }

  // Step 3. Parse the ChiselIR and FirrtlIR. In this step modules, ports, and internal elements are extracted
  //  They are stored in UniqueHashMaps and associated with their respective elementIds
  private val firrtlIRParser = new FirrtlIRParser
  private val chiselIRParser = new ChiselIRParser

  firrtlIRParser.parseCircuit(circuitFirrtlIR)
  chiselIRParser.parseCircuit(circuitChiselIR)

  // Step 4. Parse the debug information
  private val gDebugIRParser = new DebugIRParser(workingDir, TypedConverter.getDebugIRFile(gOpt = true))
  //  val debugIRParser =
  //    new DebugIRParser(workingDir, TypedConverter.getDebugIRFile(gOpt = false)) // TODO: check if this is needed or not

  gDebugIRParser.parse()
  //  debugIRParser.parse()

  /** Print debug information */
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
    // debugIRParser.dump(s"$logSubDir/DebugIRParser.log")
  }

  private def joinMapCircuits[K](listMaps: Seq[(String, UniqueHashMap[K, ?])]): Map[K, Seq[(String, Option[?])]] =
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

    /** Join the list of maps and write them into files */
    def joinAndDump[K](listMaps: Seq[(String, UniqueHashMap[K, ?])])(dumpFile: String): Unit = {

      val bw = new java.io.BufferedWriter(new java.io.FileWriter(dumpFile))

      joinMapCircuits(listMaps).foreach {
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

  /**
   * Create the [[tywaves_symbol_table.TywaveState]] from the parsed IRs.
   *
   *   1. It first joins the maps of the circuits together.
   *   1. Then, for each element parsed from the debugIR (joined with the
   *      others), it creates the hierarchy of [[tywaves_symbol_table.Scope]]
   *      for the TywavesState by looking at the child variables and child
   *      scopes.
   */
  def createTywavesState(): Unit = {
    import io.circe.syntax._
    import tywaves_symbol_table._
    import tywaves_symbol_table.tywaves_encoders._

    import java.io.PrintWriter

    val groupIrPerElement: Map[ElId, Seq[(String, Option[?])]] = joinMapCircuits(
      Seq(
        ("chiselIR", chiselIRParser.allElements),
        ("firrtlIR", firrtlIRParser.allElements),
        ("debugIR", gDebugIRParser.signals),
      )
    )

    // Each element is associated to the 3 IRs
    // The goal here is to create a Tywavestate

    val (childVariables, childScopes) = groupIrPerElement.flatMap {
      case (elId, irs) =>
        // Iterate over the IRs
        irs.flatMap {
          case (ir, Some(value)) =>
            Some((findChildVariable(elId, value, ir), findChildScopes(value)))
          case (ir, None) =>
            println(s"IR without match: $ir", None)
            None
        }.filter { case (variableOpt, _) => variableOpt.isDefined }
          .map { case (variableOpt, scopes) => (variableOpt.get, scopes) }
    }.unzip

    val scopes = Seq(tywaves_symbol_table.Scope(name = dutName, childVariables.toSeq, childScopes.flatten.toSeq))
    // Finalize the scopes -> mergeScopes(scope) is not needed anymore
    val finalScopes = cleanFromFlattenedSignals(scopes)

    // Collect the top external ports
    val topPorts = finalScopes.flatMap {
      case Scope(_, childVariables, _) => childVariables.filter {
          case Variable(_, _, hwType, _) => hwType.isInstanceOf[tywaves_symbol_table.hwtype.Port]
        }
    }

    // Finalize the scopes
    val tbScope  = Scope(tbScopeName, topPorts, finalScopes)
    val topScope = Scope(topName, Seq.empty, Seq(tbScope))

    // Output the TywavesState in json
    val tywaveState = tywaves_symbol_table.TywaveState(Seq(topScope)).asJson
    val printWriter = new PrintWriter(s"$tywavesStatePath")
    printWriter.write(tywaveState.spaces2) // Use spaces2 to format the JSON with indentation
    printWriter.close()

  }

  /** Given a sequence of scopes: merge common scopes together */
  private def mergeScopes(scopes: Seq[tywaves_symbol_table.Scope]): Seq[tywaves_symbol_table.Scope] =
    // Join all the scopes with the same names
    scopes.groupBy(_.name).map(
      _._2.reduce((a, b) =>
        tywaves_symbol_table.Scope(
          a.name,
          a.childVariables ++ b.childVariables,
          a.childScopes ++ b.childScopes,
        )
      )
    ).toSeq

  /**
   * Clean from flattened signals If for example there is a signal that is part
   * of a bundle, it should be removed from the child signals of the scope. This
   * ensures that there will be only one reference to the signal.
   */
  private def cleanFromFlattenedSignals(scopes: Seq[tywaves_symbol_table.Scope]): Seq[tywaves_symbol_table.Scope] =
    scopes.map { scope =>
      // Get variables that are not "sub variables" of compound variables
      val childVariables = scope.childVariables.filter {
        case tywaves_symbol_table.Variable(name, _, _, _) =>
          // If this variable is not a child of another variable then keep it in the scope
          !scope.childVariables.exists {
            case tywaves_symbol_table.Variable(_, _, _, realType) =>
              realType match {
                case tywaves_symbol_table.realtype.Bundle(subVariables, _) =>
                  subVariables.exists(_.name == name)
                case _ => false
              }
          }
      }
      tywaves_symbol_table.Scope(scope.name, childVariables, scope.childScopes)
    }

  /** Find a tywave scope */
  @unused
  private def findTywaveScope[Tuple](tuple: Tuple): String =
    tuple match {
      case (Name(_, _, tywaveScope), Direction(_), HardwareType(_, _), Type(_), VerilogSignals(_)) => tywaveScope
      case (Name(_, _, tywaveScope), Direction(_), Type(_))                                        => tywaveScope
      case _ =>
        throw new NotImplementedError("This branch shouldn't be reached.")
    }

  /**
   * Find the Chisel type name of a variable given its [[ElId]]
   */
  private def findChiselTypeName(elIdGuess: ElId): String =
    chiselIRParser.allElements(elIdGuess) match {
      case (Name(_, _, _), Direction(_), Type(tpe)) =>
        tpe
      case _ => throw new NotImplementedError("This branch shouldn't be reached.")
    }

  /**
   * Find the hardware type of a potential port given its [[ElId]]
   */
  private def findPortHwType(elIdGuess: ElId): Option[tywaves_symbol_table.hwtype.HwType] =
    // Check within the ports otherwise
    firrtlIRParser.allElements.get(elIdGuess) match {
      case Some((Name(_, _, _), Direction(dir), Type(_))) =>
        if (dir == "Input" || dir == "Output")
          Some(tywaves_symbol_table.hwtype.from_string("Port", Some(dir)))
        else None
      case _ => None
    }

  /** Find the child variables of a given tuple for a given representation */
  private def findChildVariable[Tuple](elId: ElId, tuple: Tuple, ir: String): Option[tywaves_symbol_table.Variable] =
    if (ir == "debugIR")
      tuple match {
        case (
              Name(name, scope, _),
              Direction(dir),
              HardwareType(hardwareType, size),
              Type(_),
              VerilogSignals(verilogSignals),
            ) =>
          // Get the list of children of this variable
          val listChildren = gDebugIRParser.signals.filter {
            case (_: ElId, (Name(_, childScope, _), Direction(_), HardwareType(_, _), Type(_), VerilogSignals(_))) =>
              val guessChildScope = scope + "_" + name
              childScope == guessChildScope
            case _ => false
          }

          // If there's only one signal, it's a leaf: however the leaf can be a reference to a bundle and not only a ground signal
          val realType = if (verilogSignals.length <= 1 && listChildren.isEmpty)
            tywaves_symbol_table.realtype.Ground(size.getOrElse(0), verilogSignals.head)
          else
            // TODO: Understand how to handle other types
            tywaves_symbol_table.realtype.Bundle(
              fields = listChildren.flatMap { child =>
                findChildVariable(child._1, child._2, ir)
              }.toSeq,
              vcdName = Some(name),
            )

          // Add the variable to the list of children
          Some(tywaves_symbol_table.Variable(
            name = name,
            typeName = findChiselTypeName(elId),
            hwType = findPortHwType(elId).getOrElse(
              tywaves_symbol_table.hwtype.from_string(hardwareType, Some(dir))
            ),
            realType = realType,
          ))

        // In the other cases return the old list of children
        case (Name(_, _, _), Direction(_), Type(_)) => None
        case _ => throw new NotImplementedError("This branch shouldn't be reached.")
      }
    else {
      None
    }

  private def findChildScopes[Tuple](value: Tuple): Seq[tywaves_symbol_table.Scope] =
    // TODO: implement it to support submodules
    Seq.empty
}
