package chisel3
package tywaves.typedTreadle

import chisel3.RawModule
//import chisel3.internal.firrtl.Command
import chisel3.reflect.DataMirror
import chisel3.reflect.DataMirror.modulePorts
import chisel3.stage.{ChiselCircuitAnnotation, ChiselGeneratorAnnotation}
import firrtl.ir.Serializer
import firrtl.stage.FirrtlCircuitAnnotation

import scala.collection.mutable.ListBuffer

/** These steps are retrieved from [[chiseltest.simulator.ChiselBridge]] */
private[typedTreadle] object ChiselMapper {
  /** Elaborate all [[chisel3.stage.ChiselGeneratorAnnotation]]s into [[chisel3.stage.ChiselCircuitAnnotation]]s.
   *
   * The [[chisel3.stage.ChiselGeneratorAnnotation]] basically stores a chisel module (a function that returns a Chisel module)..
   *
   * The [[chisel3.stage.ChiselCircuitAnnotation]] stores a Chisel [[chisel3.internal.firrtl.Circuit]].
   *
   */
  private val elaboratePhase = new chisel3.stage.phases.Elaborate

  /** Run [[chisel3.stage.phases.AspectPhase]] if a [[chisel3.aop.Aspect]] is present.
   *
   * The [[chisel3.stage.phases.AspectPhase]] consumes the [[chisel3.stage.DesignAnnotation]] and converts every `Aspect`
   * into their annotations prior to executing FIRRTL.
   *
   * The [[chisel3.stage.DesignAnnotation]] contains the top-level elaborated Chisel design.
   */
  private val maybeAspectPhase = new chisel3.stage.phases.MaybeAspectPhase

  /** This prepares a [[chisel3.stage.ChiselCircuitAnnotation]] for compilation with FIRRTL. This does three things:
   *   - Uses [[chisel3.internal.firrtl.Converter]] to generate a [[firrtl.stage.FirrtlCircuitAnnotation]] which stores a [[firrtl.ir.Circuit]].
   *   - Extracts all [[firrtl.annotations.Annotation]]s from the [[chisel3.internal.firrtl.Circuit]]
   *   - Generates any needed `RunFirrtlTransformAnnotation`s from extracted `firrtl.annotations.Annotation`s
   */
  private val converter = new chisel3.stage.phases.Convert


}

class ChiselMapper[M <: RawModule](
                                    gen: () => M,
                                    userAnnos: firrtl2.AnnotationSeq,
                                    chiselAnnos: firrtl.AnnotationSeq
                                  ) {

  //  val ports = DataMirror.modulePorts(gen()) // This must be inside a builder context
//  val commands = ListBuffer[Command]()

  import ChiselMapper._

  // run Builder.build(Module(gen()))
  val genAnno = ChiselGeneratorAnnotation(gen)

  /** Elaborate all [[chisel3.stage.ChiselGeneratorAnnotation]]s into [[chisel3.stage.ChiselCircuitAnnotation]]s. */
  val elaborationAnnos: firrtl.AnnotationSeq = elaboratePhase.transform(genAnno +: chiselAnnos)

  /** Run [[chisel3.stage.phases.AspectPhase]] if a [[chisel3.aop.Aspect]] is present. */
  val aspectAnnos: firrtl.AnnotationSeq = maybeAspectPhase.transform(elaborationAnnos)

  /** This prepares a [[chisel3.stage.ChiselCircuitAnnotation]] for compilation with FIRRTL. */
  val converterAnnos: firrtl.AnnotationSeq = converter.transform(aspectAnnos)

  /** This gets the firrtl annos, such as `dontTouch` */
  val firrtlAnnos = converterAnnos.collectFirst({ case c: ChiselCircuitAnnotation => c }).get.circuit.firrtlAnnotations

  def printCircuit(): Unit = {
    println(emitHighFirrtl())
    println()
    ChiselMapperDebug.printCircuit(converterAnnos)
  }

  def printDiff(): Unit = ChiselMapperDebug.printDiffCirc(this)

  def emitHighFirrtl(): String = {
    val highFirrtl = converterAnnos.collectFirst { case c: FirrtlCircuitAnnotation => c }.get.circuit.serialize
    highFirrtl
  }

  def func = {

  }


}

