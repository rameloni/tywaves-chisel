package hierarchicalmodules

import chisel3._

class SubBlink(n: Int) extends Module {
  val io = IO(new Bundle {
    val enable = Input(Bool())
    val led    = Output(Bool())
  })
  val blinkerx = Module(new Blink(n))

  blinkerx.io.enable := io.enable
  io.led             := blinkerx.io.led
}

class AnotherModule extends Module {
  val out = IO(Output(UInt(2.W)))

  val outReg = RegInit(false.B)
  outReg := ~outReg
  out    := outReg
}
class AMultiBlink extends Module {
  val io = IO(new Bundle {
    val enable: Bool = Input(Bool())
    val leds:   UInt = Output(UInt(4.W))
  })

  val anotherModule: AnotherModule = Module(new AnotherModule)

  val blinker:  SubBlink = Module(new SubBlink(5))
  val blinker2: Blink    = Module(new Blink(3))
  val blinker3: Blink    = Module(new Blink(2))

  blinker.io.enable  := io.enable
  blinker2.io.enable := io.enable
  blinker3.io.enable := io.enable

  when(anotherModule.out === 1.U) {
    blinker.io.enable  := false.B
    blinker2.io.enable := false.B
  }
  io.leds :=
    (blinker.io.led << 3).asUInt + (blinker.io.led << 2).asUInt + (blinker.io.led << 1).asUInt + (blinker.io.led << 0).asUInt

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
