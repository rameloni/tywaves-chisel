package examples

//> using scala "2.13.12"
//> using lib "org.chipsalliance::chisel::6.0.0"
//> using plugin "org.chipsalliance:::chisel-plugin::6.0.0"
//> using options "-unchecked", "-deprecation", "-language:reflectiveCalls", "-feature", "-Xcheckinit", "-Xfatal-warnings", "-Ywarn-dead-code", "-Ywarn-unused", "-Ymacro-annotations"

import chisel3._
import circt.stage.ChiselStage

// To test the nested module case
class Bar extends Module {
  val io = IO(new Bundle {
    val a   = Input(Bool())
    val b   = Input(Bool())
    val out = Output(Bool())
  })

  io.out := io.a & io.b
}

class Simple extends Bundle

class MyBundle extends Simple {
  val a = UInt(8.W)
  val b = UInt(8.W)
  val c = UInt(8.W)

  val bundle = new Bundle {
    val z = Bool()
  }
}

trait Function0[@specialized(Unit, Int, Double) T] {
  def apply: T
}

class Foo extends Module {
  val x    = IO(Input(Bool())).suggestName("cia.o");
  val s    = IO(Input(new MyBundle)).suggestName("cia_o")
  val io_a = Wire(Bool())

  val io = IO(new Bundle {
    val a   = Input(Bool())
    val b   = Input(Bool())
    val out = Output(UInt(8.W))
  })

  val reg = RegInit(0.U(8.W))

  dontTouch(io_a)

  io_a   := 1.U
  io.out := io.a + io.b

  printf(p"s: $s\n")
}

object Main extends App {

  val c = ChiselStage.convert(new Foo)

  println(
    ChiselStage.emitSystemVerilog(
      gen = new Foo,
      firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info"),
    )
  )

  println(
    ChiselStage.emitCHIRRTL(
      gen = new Foo
//      args = Array("--help")
    )
  )

  println(
    ChiselStage.emitFIRRTLDialect(
      gen = new Foo,
//      args = Array("--help")
//      firtoolOpts = Array("--chisel-interface-out-dir=outchisel")
      firtoolOpts = Array("-h", "--chisel-interface-out-dir=outchisel"),
    )
  )

}
