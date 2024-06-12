package hierarchicalmodules

import chisel3.{Bits, Data, Module, Printable, UInt, UIntFactory, Wire, dontTouch, fromBooleanToLiteral, fromIntToWidth}
import chisel3.util.Counter
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.must.Matchers
import tywaves.simulator.TraceVcd

class UseCounter extends AnyFunSpec with Matchers {

  class Char {
    val value: UInt = UInt(8.W)

    def apply: UInt = value
  }
  object Char {
    def apply(): Char = new Char
  }

  class MyCounter extends Module {
    import chisel3._

    val out: UInt = IO(Output(UInt(8.W)))

    val cnt: Counter = Counter(4)

    cnt.inc()
    out := cnt.value
  }

  describe("Blink with tywaves simulator") {
    import tywaves.simulator.TywavesSimulator._
    import tywaves.simulator.simulatorSettings._

    it("should work") {
      simulate(
        new MyCounter,
        Seq(VcdTrace, WithTywavesWaveforms(false), SaveWorkdirFile("workdir")),
        simName = "blink_with_tywaves_sim_should_work",
      ) { dut =>
        dut.clock.step(10)
      }
    }
  }
}
