//> using scala "2.13.14"
//> using dep "com.github.rameloni::tywaves-chisel-api:0.4.0-SNAPSHOT"
//> using dep "org.chipsalliance::chisel:6.4.0"
//> using plugin "org.chipsalliance:::chisel-plugin:6.4.0"
//> using options "-unchecked", "-deprecation", "-language:reflectiveCalls", "-feature", "-Xcheckinit", "-Xfatal-warnings", "-Ywarn-dead-code", "-Ywarn-unused", "-Ymacro-annotations"
//> using dep "org.scalatest::scalatest:3.2.19"

import chisel3._
import circt.stage._
import chisel3.util._
import tywaves.simulator._
import tywaves.simulator.simulatorSettings._

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class DetectTwoOnes extends Module {
  val io = IO(new Bundle {
    val in      = Input(Bool())
    val out     = Output(Bool())
  })

  object State extends ChiselEnum { val sNone, sOne1, sTwo1s = Value }
  val state = RegInit(State.sNone)

  // Tmp signal 1
  val isOne = Wire(Bool())
  isOne := io.in
  // Tmp signal 2
  val willBeTwo1s = io.in && (state === State.sOne1 || state === State.sTwo1s)

  io.out := (state === State.sTwo1s)

  switch(state) {
    is(State.sNone) { when(isOne) { state := State.sOne1 } }
    is(State.sOne1) { 
        when(isOne) { state := State.sTwo1s }.otherwise { state := State.sNone } 
    }
    is(State.sTwo1s) { when(!isOne) { state := State.sNone } }
  }
}


class DetectTwoOnesTest extends AnyFunSpec with Matchers {

    import TywavesSimulator._
    def runTest(fsm: DetectTwoOnes) = {
        // Inputs and expected results
        val inputs   = Seq(0, 0, 1, 0, 1, 1, 0, 1, 1, 1)
        val expected = Seq(0, 0, 0, 0, 0, 1, 0, 0, 1, 1)

        // Reset
        fsm.io.in.poke(0)
        fsm.clock.step(1)

        for (i <- inputs.indices) {
            fsm.io.in.poke(inputs(i))
            fsm.clock.step(1)
            //	c.clock.getStepCount
            fsm.io.out.expect(expected(i))
            //	System.out.println(s"In: ${inputs(i)}, out: ${expected(i)}")
        }
    }

  describe("TywavesSimulator") {
    it("runs DetectTwoOnes correctly") {
      val chiselStage = new ChiselStage(true)
      
      chiselStage.execute(
        args = Array("--target", "chirrtl"),
        annotations = Seq(
          chisel3.stage.ChiselGeneratorAnnotation(() => new DetectTwoOnes()),
          FirtoolOption("-g"),
          FirtoolOption("--emit-hgldd"),
        ),
      )

      simulate(new DetectTwoOnes(), Seq(VcdTrace, WithTywavesWaveformsGo(true)), simName = "runs_detect2ones") {
        fsm =>
            runTest(fsm)
      }

    }
  }

}