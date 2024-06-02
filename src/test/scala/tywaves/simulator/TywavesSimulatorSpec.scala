package tywaves.simulator

import tywaves.simulator.TywavesSimulator._
import tywaves.simulator.simulatorSettings._
import chisel3._
import gcd.GCD
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.must.Matchers

import java.nio.file.{Files, Paths}

class TywavesSimulatorSpec extends AnyFunSpec with Matchers {

  private def gcdTb(gcd: => GCD): Unit = {
    gcd.io.a.poke(24.U)
    gcd.io.b.poke(36.U)
    gcd.io.loadValues.poke(1.B)
    gcd.clock.step()
    gcd.io.loadValues.poke(0.B)
    gcd.clock.stepUntil(sentinelPort = gcd.io.resultIsValid, sentinelValue = 1, maxCycles = 10)
    gcd.io.resultIsValid.expect(true.B)
    gcd.io.result.expect(12)
  }

  describe("New TywavesFunctionalities") {
    resetBeforeEachRun()

    it("runs GCD with waveform generation") {
      simulate(new GCD(), Seq(VcdTrace, WithTywavesWaveforms(false)), simName = "runs_gcd_with_waveform_generation") {
        gcd =>
          gcdTb(gcd)
      }

      assert(Files.exists(Paths.get("test_run_dir/GCD/TywavesSimulator/runs_gcd_with_waveform_generation/trace.vcd")))
      assert(Files.exists(
        Paths.get(
          "test_run_dir/GCD/TywavesSimulator/runs_gcd_with_waveform_generation/tywaves-log/tywavesState.json"
        )
      ))
    }

    it("runs GCD with waveform generation and custom name trace") {
      simulate(
        new GCD(),
        Seq(VcdTrace, NameTrace("gcdTest"), WithTywavesWaveforms(false)),
        simName = "runs_gcd_with_waveform_generation",
      ) { gcd =>
        gcdTb(gcd)
      }

      assert(Files.exists(Paths.get("test_run_dir/GCD/TywavesSimulator/runs_gcd_with_waveform_generation/gcdTest.vcd")))
      assert(Files.exists(
        Paths.get(
          "test_run_dir/GCD/TywavesSimulator/runs_gcd_with_waveform_generation/tywaves-log/tywavesState.json"
        )
      ))
    }

    it("raises an exception when Tywaves is used without VcdTrace") {
      intercept[Exception] {
        simulate(new GCD(), Seq(WithTywavesWaveforms(false)))(_ => gcdTb _)
      }
    }

  }

  describe("Tywaves with ParametricSimulator Functionalities") {

    resetBeforeEachRun()

    it("runs GCD correctly without settings") {
      simulate(new GCD())(gcd => gcdTb(gcd))
    }

    it("runs GCD with VCD trace file") {
      simulate(new GCD(), Seq(VcdTrace))(gcd => gcdTb(gcd))
      assert(Files.exists(Paths.get("test_run_dir/GCD/TywavesSimulator/defaultSimulation/trace.vcd")))
    }

    it("runs GCD with VCD underscore trace") {
      simulate(new GCD(), Seq(VcdTraceWithUnderscore))(gcd => gcdTb(gcd))
      assert(Files.exists(Paths.get("test_run_dir/GCD/TywavesSimulator/defaultSimulation/trace_underscore.vcd")))
    }

    it("runs GCD with VCD trace file with name") {
      simulate(new GCD(), Seq(VcdTrace, NameTrace("gcdTbTraceName")))(gcd => gcdTb(gcd))
      assert(Files.exists(Paths.get("test_run_dir/GCD/TywavesSimulator/defaultSimulation/gcdTbTraceName.vcd")))
    }

    it("runs underscore when VcdTrace and VcdTraceWithUnderscore are used") {
      simulate(new GCD(), Seq(VcdTrace, VcdTraceWithUnderscore))(gcd => gcdTb(gcd))
      assert(Files.exists(Paths.get("test_run_dir/GCD/TywavesSimulator/defaultSimulation/trace_underscore.vcd")))

      simulate(new GCD(), Seq(VcdTraceWithUnderscore, VcdTrace))(gcd => gcdTb(gcd))
      assert(Files.exists(Paths.get("test_run_dir/GCD/TywavesSimulator/defaultSimulation/trace_underscore.vcd")))
    }

    it("runs GCD with VCD trace file with name and VCD underscore trace") {
      simulate(new GCD(), Seq(VcdTrace, VcdTraceWithUnderscore, NameTrace("gcdTb1")))(gcd => gcdTb(gcd))
      assert(Files.exists(Paths.get("test_run_dir/GCD/TywavesSimulator/defaultSimulation/gcdTb1_underscore.vcd")))

      simulate(new GCD(), Seq(VcdTraceWithUnderscore, VcdTrace, NameTrace("gcdTb2")))(gcd => gcdTb(gcd))
      assert(Files.exists(Paths.get("test_run_dir/GCD/TywavesSimulator/defaultSimulation/gcdTb2_underscore.vcd")))

      simulate(new GCD(), Seq(NameTrace("gcdTb3"), VcdTraceWithUnderscore, VcdTrace))(gcd => gcdTb(gcd))
      assert(Files.exists(Paths.get("test_run_dir/GCD/TywavesSimulator/defaultSimulation/gcdTb3_underscore.vcd")))
    }

    it("uses a name for the simulation") {
      simulate(new GCD(), Seq(VcdTrace, NameTrace("gcdTb1")), simName = "use_a_name_for_the_simulation")(gcd =>
        gcdTb(gcd)
      )
      assert(Files.exists(Paths.get("test_run_dir/GCD/TywavesSimulator/use_a_name_for_the_simulation/gcdTb1.vcd")))
    }

    it("save the workdir with a name") {
      simulate(new GCD(), Seq(VcdTrace, SaveWorkdirFile("myWorkdir")))(gcd => gcdTb(gcd))
      assert(Files.exists(Paths.get("test_run_dir/GCD/TywavesSimulator/defaultSimulation/myWorkdir")))
    }

    it("uses firtool args") {
      simulate(new GCD(), Seq(WithFirtoolArgs(Seq("-g", "--emit-hgldd")), SaveWorkdirFile("myWorkdir2")))(gcd =>
        gcdTb(gcd)
      )
      assert(Files.exists(Paths.get("test_run_dir/GCD/TywavesSimulator/defaultSimulation/myWorkdir2")))
      assert(Files.exists(
        Paths.get("test_run_dir/GCD/TywavesSimulator/defaultSimulation/myWorkdir2/support-artifacts/GCD.dd")
      ))
    }

  }

  describe("TywavesSimulator Exceptions") {
    resetBeforeEachRun()
    it("throws an exception when NameTrace is used without VcdTrace or VcdTraceWithUnderscore") {
      intercept[Exception] {
        simulate(new GCD(), Seq(NameTrace("")))(_ => gcdTb _)
      }
    }

    it("throws an exception with two or more NameTrace") {
      intercept[Exception] {
        simulate(new GCD(), Seq(VcdTrace, NameTrace("a"), NameTrace("b")))(_ => gcdTb _)
      }
    }

    it("throws an exception with two or more SaveWorkdir") {
      intercept[Exception] {
        simulate(new GCD(), Seq(SaveWorkdir, SaveWorkdir))(_ => gcdTb _)
      }
    }

    it("throws an exception with two or more SaveWorkdirFile") {
      intercept[Exception] {
        simulate(new GCD(), Seq(SaveWorkdirFile("a"), SaveWorkdirFile("b")))(_ => gcdTb _)
      }
    }

  }
}
