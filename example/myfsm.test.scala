//> using scala "2.13.14"
//> using dep "com.github.rameloni::tywaves-chisel-api:0.4.0-SNAPSHOT"
//> using dep "org.chipsalliance::chisel:6.4.0"
//> using plugin "org.chipsalliance:::chisel-plugin:6.4.0"
//> using options "-unchecked", "-deprecation", "-language:reflectiveCalls", "-feature", "-Xcheckinit", "-Xfatal-warnings", "-Ywarn-dead-code", "-Ywarn-unused", "-Ymacro-annotations"
//> using dep "org.scalatest::scalatest:3.2.18"

// DO NOT EDIT THE ORTHER OF THESE IMPORTS (it will be solved in future versions)
import tywaves.simulator._
import tywaves.simulator.simulatorSettings._
import chisel3._
// _root_ disambiguates from package chisel3.util.circt if user imports chisel3.util._
//import _root_.circt.stage.ChiselStage
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import circt.stage.ChiselStage

// Enum of possible states
object MyFSMStates extends ChiselEnum {
  val IDLE, StateA, StateB, END = Value
}

class FSM extends Bundle {

  val inputState = IO(Input(MyFSMStates()))
  val state      = RegInit(MyFSMStates.IDLE)
  val stateNxt   = WireInit(MyFSMStates.IDLE)

  val endConst = WireInit(MyFSMStates.END)

  when(state === MyFSMStates.IDLE) {
    stateNxt := MyFSMStates.StateA
  }.elsewhen(state === MyFSMStates.StateA) {
    stateNxt := MyFSMStates.StateB
  }.elsewhen(state === MyFSMStates.StateB) {
    stateNxt := MyFSMStates.END
  }.otherwise {
    stateNxt := MyFSMStates.IDLE
  }

  when(inputState === MyFSMStates.END) {
    state := MyFSMStates.IDLE
  }.otherwise {
    state := stateNxt
  }
}

class MyFSM extends Module {
  val fsm = new FSM

  val io = IO(new Bundle {
    val inputState = Input(MyFSMStates())
  })

  val aConstBundle = Wire(new Bundle {
    val bit = Bool()
    val bv = UInt(32.W)
    val subbundle = new Bundle {
      val x = SInt(3.W)
    }
  })
  aConstBundle.bit := 1.B
  aConstBundle.bv := 34.U
  aConstBundle.subbundle.x := 2.S
}


class MyFSMTest extends AnyFunSpec with Matchers {

  describe("TywavesSimulator") {
    it("runs MyFSM correctly") {
      import TywavesSimulator._
      val chiselStage = new ChiselStage(true)
      
      chiselStage.execute(
        args = Array("--target", "chirrtl"),
        annotations = Seq(
          chisel3.stage.ChiselGeneratorAnnotation(() => new MyFSM()),
          circt.stage.FirtoolOption("-g"),
          circt.stage.FirtoolOption("--emit-hgldd"),
        ),
      )
      simulate(new MyFSM(), Seq(VcdTrace, WithTywavesWaveformsGo(true)), simName = "runs_MYFSM_correctly_launch_tywaves_and_go") {
        fsm =>
          fsm.clock.step(10)
          fsm.clock.step(10)
      }
      simulate(new MyFSM(), Seq(VcdTrace, WithTywavesWaveforms(true)), simName = "runs_MYFSM_correctly_launch_tywaves") {
        fsm =>
          fsm.clock.step(10)
          fsm.io.inputState.poke(MyFSMStates.StateA)
          fsm.clock.step(10)
      }
    }
  }

}
