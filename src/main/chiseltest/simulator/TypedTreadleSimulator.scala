package chiseltest
package simulator

//import chiseltest.simulator.{Simulator, SimulatorAnnotation, SimulatorContext, WriteWaveformAnnotation}

import firrtl2.CircuitState

case object TypedTreadleBackendAnnotation extends SimulatorAnnotation {
  override def getSimulator: Simulator = TypedTreadleSimulator
}

object TypedTreadleSimulator extends Simulator {

  /** Used to override functions [[name]], [[isAvailable]], [[supportsCoverage]], [[supportsLiveCoverage]] and [[createContext()]] */
  private val internalSimulator = TreadleSimulator

  override def name: String = "Typed_" + internalSimulator.name

  override def isAvailable: Boolean = internalSimulator.isAvailable

  override def waveformFormats: Seq[WriteWaveformAnnotation] = Seq(WriteVcdAnnotation)

  override def supportsCoverage: Boolean = internalSimulator.supportsCoverage

  override def supportsLiveCoverage: Boolean = internalSimulator.supportsLiveCoverage

  override def createContext(state: CircuitState): SimulatorContext = {
    println("TypedTreadleSimulator.createContext")

    internalSimulator.createContext(state)
  }


}
