package foo

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
  val wire = Wire(Bool())
  wire   := io.a & io.b
  io.out := wire
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
  val x    = IO(Input(Bool()))       // .suggestName("cia.o");
  val s    = IO(Input(new MyBundle)) // .suggestName("cia_o")
  val io_a = Wire(Bool())

  val io = IO(new Bundle {
    val a   = Input(Bool())
    val b   = Input(Bool())
    val out = Output(UInt(8.W))
  })

  val vec = VecInit(Seq.fill(4)(0.U(8.W)))
  val reg = RegInit(0.U(8.W))

  dontTouch(io_a)
  dontTouch(vec)

//  val myInternalBundle = Wire(new MyBundle)
//  myInternalBundle.a        := 0.U
//  myInternalBundle.b        := 0.U
//  myInternalBundle.c        := 0.U
//  myInternalBundle.bundle.z := false.B
//  dontTouch(myInternalBundle)

  io_a   := 1.U
  io.out := io.a + io.b

  printf(p"s: $s\n")
}

object MainFoo extends App {

  val stage = new ChiselStage
  val c     = ChiselStage.convert(new Foo)

  println(
    ChiselStage.emitCHIRRTL(
      gen = new Foo
    )
  )

}
