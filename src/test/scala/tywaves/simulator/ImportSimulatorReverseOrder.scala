package tywaves.simulator

import gcd.GCD
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.must.Matchers

class ImportSimulatorReverseOrder extends AnyFunSpec with Matchers {
  describe("Issue 27") {
    it("Should import chisel before tywaves") {
      import chisel3._
      import tywaves.simulator.TywavesSimulator._
      simulate(new GCD()) {
        gcd =>
          gcd.clock.step()
          gcd.io.loadValues.poke(true.B)
      }
    }
    it("Should import chisel after tywaves") {
      import tywaves.simulator.TywavesSimulator._
      import chisel3._
      simulate(new GCD()) {
        gcd =>
          gcd.clock.step()
          gcd.io.loadValues.poke(true.B)
      }
    }
  }

}
