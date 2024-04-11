package hierarchicalmodules

import chisel3._
import chisel3.util.Counter

class Blink(period: Int) extends Module {
  assert(period > 0, "limit must be greater than 0")
  val io = IO(new Bundle {
    val enable: Bool = Input(Bool())
    val led   : Bool = Output(Bool())
  })

  val cnt:    Counter = Counter(period)
  val ledReg: Bool    = RegInit(false.B)

  when(io.enable) {
    when(cnt.inc() && cnt.value === (period - 1).U) {
      ledReg := ~ledReg
    }
  }

  io.led := ledReg
}

object MainBlink extends App {
  import circt.stage.ChiselStage

  println(
    ChiselStage.emitSystemVerilog(
      new Blink(4),
      firtoolOpts = Array(
        "-O=debug",
        "-g",
        "--disable-all-randomization",
        "--strip-debug-info",
      ),
    )
  )
}
