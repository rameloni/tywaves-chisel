package hierarchicalmodules

import chisel3.fromBooleanToLiteral
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.must.Matchers

class MultiBlinkTest extends AnyFunSpec with Matchers {

  private def blinkTb(dut: => AMultiBlink): Unit = {
    import chisel3.simulator.PeekPokeAPI._
    dut.reset.poke(true.B)
    dut.io.enable.poke(true.B)
    dut.clock.step(5)
    dut.reset.poke(false.B)

    dut.clock.step() // 1
    dut.clock.step() // 2
    dut.clock.step() // 3
    dut.clock.step() // 4
    dut.clock.step() // 5
    dut.clock.step() // 6
    dut.clock.step()
    dut.clock.step(30)
  }

  describe("Multi Blink with parametric simulator") {
    import tywaves.simulator.ParametricSimulator._
    import tywaves.simulator.simulatorSettings._

    it("should work") {
      simulate(
        new AMultiBlink,
        Seq(VcdTrace, WithFirtoolArgs(Seq("-O=debug", "-g"))),
        simName = "multiblink_with_parametric_sim_should_work",
      ) { dut =>
        blinkTb(dut)
      }
    }
  } // end of describe("MultiBlink with parametric simulator")

  describe("MultiBlink with tywaves simulator") {
    import tywaves.simulator.TywavesSimulator._
    import tywaves.simulator.simulatorSettings._

    it("should work") {
      simulate(
        new AMultiBlink,
        Seq(VcdTrace, WithTywavesWaveforms(true), SaveWorkdirFile("workdir")),
        simName = "multiblink_with_tywaves_sim_should_work",
      ) { dut =>
        blinkTb(dut)
      }
    }
  } // end of describe("Blink with tywaves simulator")
}
