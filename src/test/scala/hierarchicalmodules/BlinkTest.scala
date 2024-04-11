package hierarchicalmodules

import chisel3.fromBooleanToLiteral
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.must.Matchers

class BlinkTest extends AnyFunSpec with Matchers {

  private def blinkTb(dut: => Blink): Unit = {
    import chisel3.simulator.PeekPokeAPI._
    dut.reset.poke(true.B)
    dut.io.enable.poke(true.B)
    dut.clock.step(5)
    dut.reset.poke(false.B)

    dut.io.led.expect(false.B)
    dut.clock.step() // 1
    dut.io.led.expect(false.B)
    dut.clock.step() // 2
    dut.io.led.expect(false.B)
    dut.clock.step() // 3
    dut.io.led.expect(false.B)
    dut.clock.step() // 4
    dut.io.led.expect(true.B)
    dut.clock.step() // 5
    dut.io.led.expect(true.B)
    dut.clock.step() // 6
    dut.io.led.expect(true.B)
    dut.clock.step()
    dut.clock.step(30)
  }

  describe("Blink with parametric simulator") {
    import tywaves.simulator.ParametricSimulator._
    import tywaves.simulator.simulatorSettings._

    it("should work") {
      simulate(
        new Blink(4),
        Seq(VcdTrace, WithFirtoolArgs(Seq("-O=debug", "-g"))),
        simName = "blink_with_parametric_sim_should_work",
      ) { dut =>
        blinkTb(dut)
      }
    }
  } // end of describe("Blink with parametric simulator")

  describe("Blink with tywaves simulator") {
    import tywaves.simulator.TywavesSimulator._
    import tywaves.simulator.simulatorSettings._

    it("should work") {
      simulate(
        new Blink(4),
        Seq(VcdTrace, WithTywavesWaveforms(true), SaveWorkdirFile("workdir")),
        simName = "blink_with_tywaves_sim_should_work",
      ) { dut =>
        blinkTb(dut)
      }
    }
  } // end of describe("Blink with tywaves simulator")
}
