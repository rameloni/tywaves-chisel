//> using scala "2.13.14"
//> using dep "com.github.rameloni::tywaves-chisel-api:0.4.2-SNAPSHOT"
//> using dep "nl.tudelft::tydi-chisel::0.1.0"
//> using plugin "org.chipsalliance:::chisel-plugin:6.4.0"
//> using options "-unchecked", "-deprecation", "-language:reflectiveCalls", "-feature", "-Xcheckinit", "-Xfatal-warnings", "-Ywarn-dead-code", "-Ywarn-unused", "-Ymacro-annotations"
//> using dep "org.scalatest::scalatest:3.2.18"

// DO NOT EDIT THE ORTHER OF THESE IMPORTS (it will be solved in future versions)
import tywaves.simulator._
import tywaves.simulator.ParametricSimulator._
import tywaves.simulator.simulatorSettings._
import chisel3._

// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
//import _root_.circt.stage.ChiselStage
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import nl.tudelft.tydi_chisel._
import chisel3.util.Counter

object MyTypes {

  /** Bit(64) type, defined in pipelineSimple_types */
  def UInt_64_t = UInt(64.W)

  assert(this.UInt_64_t.getWidth == 64)

  /** Bit(64) type, defined in pipelineSimple_types */
  def generated_0_7_d3p7qsW3_29 = SInt(64.W)

  assert(this.generated_0_7_d3p7qsW3_29.getWidth == 64)
}

/**
 * Group element, defined in pipelineSimple_types. A composite type (like a
 * struct) that contains a value associated with a timestamp.
 */
class NumberGroup extends Group {
  val time  = MyTypes.UInt_64_t
  val value = MyTypes.generated_0_7_d3p7qsW3_29
}

/**
 * Group element, defined in pipelineSimple_types. A composite type (like a
 * struct) that represents the stats of the implemented algorithm.
 */
class Stats extends Group {
  val average = (new UInt_64_t)
  val max     = MyTypes.UInt_64_t
  val min     = MyTypes.UInt_64_t
  val sum     = MyTypes.UInt_64_t
}

/** Stream, defined in pipelineSimple_types. */
class Stats_stream(private val e: TydiEl = new Stats, n: Int = 1, d: Int = 1, c: Int = 1, r: Boolean = false, u: TydiEl = Null())
    extends PhysicalStreamDetailed(e = new Stats, n = n, d = d, c = c, r = r, u = u)

object Stats_stream {
  def apply(): Stats_stream = Wire(new Stats_stream())
}

/** Stream, defined in pipelineSimple_types. */
class Generated_0_36_xeHH4woS_24
    extends PhysicalStreamDetailed(e = new NumberGroup, n = 1, d = 2, c = 1, r = false, u = Null())

object Generated_0_36_xeHH4woS_24 {
  def apply(): Generated_0_36_xeHH4woS_24 = Wire(new Generated_0_36_xeHH4woS_24())
}

/** Stream, defined in pipelineSimple_types. */
class UInt_64_t_stream extends PhysicalStreamDetailed(e=new UInt_64_t, n=1, d=1, c=1, r=false, u=Null())

object UInt_64_t_stream {
  def apply(): UInt_64_t_stream = Wire(new UInt_64_t_stream())
}

/** Bit(64), defined in pipelineSimple_types. */
class UInt_64_t extends BitsEl(64.W)

/** Bit(64), defined in pipelineSimple_types. */
class Generated_0_7_d3p7qsW3_29 extends BitsEl(64.W)

/**
 * Streamlet, defined in pipelineSimple. Top level interface.
 */
class PipelineSimple_interface extends TydiModule {

  /** Stream of [[in]] with input direction. */
  val inStream = Generated_0_36_xeHH4woS_24().flip

  /** IO of [[inStream]] with input direction. */
  val in = inStream.toPhysical

  /** Stream of [[out]] with output direction. */
  val outStream = Stats_stream()

  /** IO of [[outStream]] with output direction. */
  val out = outStream.toPhysical

  val in2Stream = UInt_64_t_stream().flip
  /** IO of [[in2Stream]] with input direction. */
  val in2: PhysicalStream = in2Stream.toPhysical
}

/**
 * Streamlet, defined in pipelineSimple. Interface for the agg function.
 */
class Reducer_interface extends TydiModule {

  /** Stream of [[in]] with input direction. */
  val inStream = Generated_0_36_xeHH4woS_24().flip

  /** IO of [[inStream]] with input direction. */
  val in = inStream.toPhysical

  /** Stream of [[out]] with output direction. */
  val outStream = Stats_stream()

  /** IO of [[outStream]] with output direction. */
  val out = outStream.toPhysical
}

/**
 * Streamlet, defined in pipelineSimple. Interface for the non negative filter:
 * df.filter(col("value") >= 0).
 */
class Instance_NonNegativeFilter_interface_RefToVargenerated_0_36_KLOZ7sts_25_1 extends TydiModule {

  /** Stream of [[in]] with input direction. */
  val inStream = Generated_0_36_xeHH4woS_24().flip

  /** IO of [[inStream]] with input direction. */
  val in = inStream.toPhysical

  /** Stream of [[out]] with output direction. */
  val outStream = Generated_0_36_xeHH4woS_24()

  /** IO of [[outStream]] with output direction. */
  val out = outStream.toPhysical
}

/**
 * Implementation, defined in pipelineSimple. Implementation of
 * df.filter(col("value") >= 0).
 */
class NonNegativeFilter extends Instance_NonNegativeFilter_interface_RefToVargenerated_0_36_KLOZ7sts_25_1 {
  // Filtered only if the value is non-negative
  // inStream.valid tells us if the input is valid
  private val canPass: Bool = inStream.el.value >= 0.S && inStream.valid

  // Connect inStream to outStream
  // This is equivalent of connecting al the fields of the two streams
  // Every future assignment will overwrite the previous one
  outStream := inStream

  // Always ready to accept input
  inStream.ready := true.B

  // if (canPass) then { it can go out } else { it is not forwarded }
  outStream.valid := canPass && outStream.ready
  outStream.strb  := inStream.strb(0) && canPass

  // All the other signals are forwarded
  //  outStream.stai := inStream.stai
  //  outStream.endi := inStream.endi
  //  outStream.last := inStream.last
  //  outStream.el := inStream.el
}

/**
 * Implementation, defined in pipelineSimple. Top level implementation. It
 * instantiates the subcomponents and connects them together.
 */
class PipelineSimple extends PipelineSimple_interface {
  inStream  := DontCare
  outStream := DontCare // Probably not used
  in2Stream := DontCare

  // Modules
  private val filter  = Module(new NonNegativeFilter)
  private val reducer = Module(new Reducer)

  // Connections
  filter.in  := in          // This will overload the inStream := in
  reducer.in := filter.out
  out        := reducer.out // This will overload the out := outStream -> that's why outStream is not used
}

/**
 * Implementation, defined in pipelineSimple. Implementation of the agg
 * function.
 */
class Reducer extends Reducer_interface {
  // Set the data width to 64 bits, such as the [[MyTypes]] types
  private val dataWidth = 64.W

  // Computing min and max
  private val maxVal: BigInt = BigInt(Long.MaxValue)        // Must work with BigInt or we get an overflow
  private val cMin:   UInt   = RegInit(maxVal.U(dataWidth)) // REG for the min value
  private val cMax:   UInt   = RegInit(0.U(dataWidth))      // REG for the max value
  // Computing the sum
  private val cSum: UInt = RegInit(0.U(dataWidth)) // REG for the sum
  // Computing the avg
  private val nValidSamples: Counter = Counter(Int.MaxValue) // The number of samples received (valid && strb(0))
  private val nSamples:      Counter = Counter(Int.MaxValue) // The number of samples received (valid)

  // Set the streams IN ready and OUT valid signals
  // inReady = if (maxVal > 0) { true.B } else { false.B}
  inStream.ready := (if (maxVal > 0) true.B else false.B)
  // inStream.ready := true.B

  // The output is valid only if we have received at least one sample
  outStream.valid := nSamples.value > 0.U

  // When a value is received
  when(inStream.valid) {
    // Get the value from th input stream
    val value = inStream.el.value.asUInt
    nSamples.inc()

    // Check the strb line and perform the updates
    when(inStream.strb(0)) {
      cMin := cMin min value
      cMax := cMax max value
      cSum := cSum + value
      nValidSamples.inc()
    }
  }
  // Set the output stream
  outStream.el.sum     := cSum
  outStream.el.min     := cMin
  outStream.el.max     := cMax
  outStream.el.average.value := Mux(nValidSamples.value > 0.U, cSum / nValidSamples.value, 0.U)

  // Set the output stream control signals:
  // they are fixed since the sum, min, max and average are updated every cycle
  // and they hold the same value if no change is performed
  outStream.strb    := 1.U
  outStream.stai    := 0.U
  outStream.endi    := 1.U
  outStream.last(0) := inStream.last(0)
}

class PipelineSimpleTest extends AnyFunSpec with Matchers {
  describe("ParametricSimulator") {
    it("runs Pipeline Simple correctly") {
      simulate(new PipelineSimple, Seq(VcdTrace, SaveWorkdirFile("aaa"))) {
        dut => dut.clock.step()
      }
    }
  }

  describe("TywavesSimulator") {
    it("runs Pipeline Simple correctly") {
      import TywavesSimulator._

      simulate(
        new PipelineSimple(),
        Seq(VcdTrace, WithTywavesWaveforms(true)),
        simName = "runs_GCD_correctly_launch_tywaves",
      ) {
        dut => dut.clock.step()

      }
    }
  }

}
