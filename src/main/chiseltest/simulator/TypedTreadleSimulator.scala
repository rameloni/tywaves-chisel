package chiseltest
package simulator

//import chiseltest.simulator.{Simulator, SimulatorAnnotation, SimulatorContext, WriteWaveformAnnotation}

import chisel3.RawModule
import firrtl2.{AnnotationSeq, CircuitState}

case object TypedTreadleBackendAnnotation extends SimulatorAnnotation {
  override def getSimulator: Simulator = TypedTreadleSimulator
}

/** New typed simulator. It wraps another simulator by adding functionalities to what is actually executed.
 *
 */
object TypedTreadleSimulator extends Simulator {

  /** Used to override functions [[name]], [[isAvailable]], [[supportsCoverage]], [[supportsLiveCoverage]] and [[createContext()]] */
  private val internalSimulator = TreadleSimulator

  override def name: String = "Typed_" + internalSimulator.name

  override def isAvailable: Boolean = internalSimulator.isAvailable

  override def waveformFormats: Seq[WriteWaveformAnnotation] = Seq(WriteVcdAnnotation)

  override def supportsCoverage: Boolean = internalSimulator.supportsCoverage

  override def supportsLiveCoverage: Boolean = internalSimulator.supportsLiveCoverage

  /** Start a new simulation using the Low FIRRTL circuit.
   *
   * @param state LoFirrtl circuit + annotations
   */
  override def createContext(state: CircuitState): SimulatorContext = {
    println("TypedTreadleSimulator.createContext")

    internalSimulator.createContext(state)
  }

  def elaborate(dutGen: () => RawModule, annos: AnnotationSeq, chiselAnnos: firrtl.AnnotationSeq): (RawModule, firrtl2.CircuitState, firrtl2.CircuitState) = {
    val (highFirrtl, dut) = Compiler.elaborate(dutGen, annos, chiselAnnos)
//    val lowFirrtl = Compiler.toLowFirrtl(highFirrtl)
    val lowFirrtl = highFirrtl
    (dut, highFirrtl, lowFirrtl)
  }

}
