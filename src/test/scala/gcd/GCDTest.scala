package gcd

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.must.Matchers

import tywaves.simulator.BetterEphemeralSimulator._
import tywaves.simulator.simSettings

import chisel3._
class GCDTest extends AnyFunSpec with Matchers {
  describe("EphemeralSimulator") {
    it("runs GCD correctly") {
      simulate(new GCD(), Seq(simSettings.EnableTrace, simSettings.LaunchTywavesWaveforms)) { gcd =>
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
