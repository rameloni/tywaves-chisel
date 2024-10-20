package memories

import chisel3._
import chisel3.util.log2Ceil


class MemIOBundle[T <: Data](depth: Int, t: T) extends Bundle {
  val rdAddr = Input(UInt(log2Ceil(depth).W))
  val rdData = Output(t)
  val wrEna  = Input(Bool())
  val wrData = Input(t)
  val wrAddr = Input(UInt(log2Ceil(depth).W))
}

/** A simple  module for testing memories in Tywaves */
class BlockMem[T <: Data](depth: Int, t: T) extends Module {
  val io = IO(new MemIOBundle(depth, t))

  val mem = SyncReadMem(depth, t)
  io.rdData := mem.read(io.rdAddr)

  when(io.wrEna) {
    mem.write(io.wrAddr, io.wrData)
  }
}

