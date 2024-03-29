package foo

import chisel3._
import circt.stage.ChiselStage

class Simple extends Bundle

class MyBundle extends Simple {
  val a = UInt(8.W)
  val b = UInt(8.W)
  val c = UInt(8.W)

  val bundle = new Bundle {
    val z = Bool()
  }
}


class Foo extends Module {
  val x    = IO(Input(Bool()))
  val s    = IO(Input(new MyBundle))
  val io_a = Wire(Bool())

  val io = IO(new Bundle {
    val a   = Input(Bool())
    val b   = Input(Bool())
    val out = Output(UInt(8.W))
  })

  val vec = VecInit(Seq.fill(4)(0.U(8.W)))
//  val reg = RegInit(0.U(8.W))

  dontTouch(io_a)
  dontTouch(vec)

  io_a   := 1.U
  io.out := io.a + io.b

  printf(p"s: $s\n")
}

object MainFoo extends App {

  println(
    ChiselStage.emitSystemVerilog(
      new Foo,
      firtoolOpts = Array(
        "-O=debug",
        "-g",
        "-emit-hgldd",
        "-output-final-mlir=Fooa.mlir",
      ),
    )
  )

}
