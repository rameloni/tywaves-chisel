package memories

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.must.Matchers

import tywaves.simulator._
import tywaves.simulator.simulatorSettings._
import chisel3._

class BlockMemTest extends AnyFunSpec with Matchers {
  describe("TywavesSimulator") {
    import TywavesSimulator._

    it("runs BlockMem of UInt8") {
      val t = UInt(8.W)
      simulate(new BlockMem(15, t), Seq(VcdTrace, WithTywavesWaveforms(false)), simName = "runs_mem_uint8")(dut =>
        dut.clock.step(2)
      )
    }

    it("runs BlockMem of Bundle") {

      class ComplexElement extends Bundle {
        val a = new Bundle {
          val subA1 = UInt(8.W)
          val subA2 = SInt(8.W)
        }
        val payload = Bits(8.W)
      }

      val t = new ComplexElement
      simulate(
        new BlockMem(4, t),
        Seq(VcdTrace, WithTywavesWaveforms(false), SaveWorkdirFile("workdir")),
        simName = "runs_mem_bundle",
      )(dut => dut.clock.step(2))
    }

    it("runs BlockMem of Vec") {
      val t = Vec(4, UInt(8.W))
      simulate(new BlockMem(4, t), Seq(VcdTrace, WithTywavesWaveforms(false)), simName = "runs_mem_vec")(dut =>
        dut.clock.step(2)
      )
    }

    it("runs BlockMem of Enum") {
      object SelType extends ChiselEnum { val A, B, C = Value }
      val t = SelType()
      simulate(new BlockMem(4, t), Seq(VcdTrace, WithTywavesWaveforms(false)), simName = "runs_mem_enum")(dut =>
        dut.clock.step(2)
      )
    }

    it("runs BlockMem of Enum in Bundle") {
      object SelType extends ChiselEnum { val A, B, C = Value }
      class ComplexElement extends Bundle {
        val sel     = SelType()
        val payload = Bits(8.W)
      }

      val t = new ComplexElement
      simulate(new BlockMem(4, t), Seq(VcdTrace, WithTywavesWaveforms(false)), simName = "runs_mem_enum_bundle")(dut =>
        dut.clock.step(2)
      )
    }

    it("runs BlockMem of Enum in Vec") {
      object SelType extends ChiselEnum { val A, B, C = Value }
      val t = Vec(4, SelType())
      simulate(new BlockMem(4, t), Seq(VcdTrace, WithTywavesWaveforms(false)), simName = "runs_mem_enum_vec")(dut =>
        dut.clock.step(2)
      )
    }

    it("runs BlockMem of Enum in 2D-Vec") {
      object SelType extends ChiselEnum { val A, B, C = Value }
      val t = Vec(4, Vec(2, SelType()))
      simulate(new BlockMem(4, t), Seq(VcdTrace, WithTywavesWaveforms(false)), simName = "runs_mem_enum_2d_vec")(dut =>
        dut.clock.step(2)
      )
    }

  }
}
