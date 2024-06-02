package bar

import chisel3._
class Baz(n: Int) extends Bundle {
  val a = UInt(n.W)
  val b = UInt(n.W)

  val nestedBundle = new Bundle {
    val z = Bool()
  }
}

class Bar extends Module {
  val io = IO(new Bundle {
    val a   = Input(Bool())
    val b   = Input(Bool())
    val out = Output(Bool())
  })

  val inputSum  = IO(Input(new Baz(8)))
  val outputSum = IO(Output(SInt(8.W)))

  when(inputSum.nestedBundle.z === true.B) {
    outputSum := inputSum.a.asSInt + inputSum.b.asSInt
  }.otherwise {
    outputSum := inputSum.a.asSInt - inputSum.b.asSInt
  }

  val cable =
    Wire(Bool()) // do not use reserved verilog words as val names (val wire) -> tywaves-demo does not work for them yet
  cable := io.a & io.b

  io.out := cable
}
object MainBar extends App {
  import circt.stage.ChiselStage

  println(
    ChiselStage.emitSystemVerilog(
      new Bar,
      firtoolOpts = Array(
        "-O=debug",
        "-g",
        "-emit-hgldd",
        "-output-final-mlir=Fooa.mlir",
      ),
    )
  )
}
