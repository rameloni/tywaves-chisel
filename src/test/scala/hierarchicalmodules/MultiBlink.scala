package hierarchicalmodules

import chisel3._

class SubBlink(n: Int) extends Module {
  val iox = IO(new Bundle {
    val enablex = Input(Bool())
    val ledx    = Output(Bool())
  })
  val blinkerx = Module(new Blink(n))

  blinkerx.io.enable := iox.enablex
  iox.ledx           := blinkerx.io.led
}

class AnotherModule extends Module {
  val out = IO(Output(UInt(2.W)))

  val outReg = RegInit(false.B)
  outReg := ~outReg
  out    := outReg
}
class AMultiBlink extends Module {
  val io = IO(new Bundle {
    val enable = Input(Bool())
    val leds   = Output(UInt(4.W))
  })

  val anotherModule = Module(new AnotherModule)

  val blinker = Module(new SubBlink(5))
  blinker.iox.enablex := io.enable
  when (anotherModule.out === 1.U) {
    blinker.iox.enablex := false.B
  }
  io.leds :=
    (blinker.iox.ledx << 3).asUInt + (blinker.iox.ledx << 2).asUInt + (blinker.iox.ledx << 1).asUInt + (blinker.iox.ledx << 0).asUInt
//  val blinks = Seq.fill(4)(Module(new Blink(5)))

//  for (i <- 1 until 5) {
//    val b = Module(new Blink(i))
//    b.io.enable := io.enable
//    io.leds(i - 1) := b.io.led
//  }

//  blinks.zip(io.leds).foreach { case (b, l) =>
//    b.io.enable := io.enable
//    l           := b.io.led
//  }
}

object MainMultiBlink extends App {
  import circt.stage.ChiselStage

  println(
    ChiselStage.emitSystemVerilog(
      new AMultiBlink,
      firtoolOpts = Array(
        "-O=debug",
        "-g",
        "--disable-all-randomization",
        "--strip-debug-info",
      ),
    )
  )
}
