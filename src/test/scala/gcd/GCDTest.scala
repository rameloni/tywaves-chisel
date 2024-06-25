package gcd

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.must.Matchers

import tywaves.simulator._
import tywaves.simulator.ParametricSimulator._
import tywaves.simulator.simulatorSettings._
import chisel3._
//import chisel3._
class GCDTest extends AnyFunSpec with Matchers {
  describe("ParametricSimulator") {
    it("runs GCD correctly") {
      simulate(new GCD(), Seq(VcdTrace, SaveWorkdirFile("gcdWorkDir"))) { gcd =>
        gcd.io.a.poke(24.U)
        gcd.io.b.poke(36.U)
        gcd.io.loadValues.poke(1.B)
        gcd.clock.step()
        gcd.io.loadValues.poke(0.B)
        gcd.clock.stepUntil(sentinelPort = gcd.io.resultIsValid, sentinelValue = 1, maxCycles = 10)
        gcd.io.resultIsValid.expect(true.B)
        gcd.io.result.expect(12)
      }
    }
  }

  describe("TywavesSimulator") {
    it("runs GCD correctly") {
      import TywavesSimulator._

      simulate(
        new GCD(),
        Seq(VcdTrace, WithTywavesWaveforms(false), WithFirtoolArgs(Seq("-g", "--emit-hgldd")), SaveWorkdir),
        simName = "runs_GCD_correctly",
      ) {
        gcd =>
          gcd.io.a.poke(24.U)
          gcd.io.b.poke(36.U)
          gcd.io.loadValues.poke(1.B)
          gcd.clock.step()
          gcd.io.loadValues.poke(0.B)
          gcd.clock.stepUntil(sentinelPort = gcd.io.resultIsValid, sentinelValue = 1, maxCycles = 10)
          gcd.io.resultIsValid.expect(true.B)
          gcd.io.result.expect(12)
      }
    }
  }

}
