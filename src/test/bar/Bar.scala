package bar
//> using scala "2.13.12"
//> using repository sonatype-s01:snapshots
//> using lib "org.chipsalliance::chisel::6.0.0+74-0a437d8f-SNAPSHOT"
//> using plugin "org.chipsalliance:::chisel-plugin::6.0.0+74-0a437d8f-SNAPSHOT"
//> using options "-unchecked", "-deprecation", "-language:reflectiveCalls", "-feature", "-Xcheckinit", "-Xfatal-warnings", "-Ywarn-dead-code", "-Ywarn-unused", "-Ymacro-annotations"

import chisel3._
import circt.stage.ChiselStage

class Baz extends Bundle {
  val a = Flipped(UInt(2.W))
  val b = Bool()
}

class Bar extends RawModule {
  val io = IO(new Baz)

  io.b :<>= !io.a
}

class BarFoo extends Module {
  val io  = IO(new Baz)
  var tmp = Bool()
  tmp = (io.a % 2.U == 0.U).asBool
  val my_wire = Wire(Bool())
  my_wire := io.a
  dontTouch(my_wire)

  val my = Wire(new Bundle {
    val wire = Bool()
    val x    = Bool()
  })
  // This provides a different output than the previous version
  //  val my2 = new Bundle {
  //    val wire = Wire(Bool())
  //    val x    = Wire(Bool())
  //  }

  my.wire := io.a
  my.x    := io.a

  dontTouch(my.wire)
  dontTouch(my.x)
//  io_a :<= io.a
//  dontTouch(io_a)
  private val bar = Module(new Bar)
  io :<>= bar.io

//  io.b := io_a
}

object MainBar extends App {
  println(
    ChiselStage.emitSystemVerilog(
      new BarFoo,
      firtoolOpts = Array(
        "-O=debug",
        "-g",
        "-emit-hgldd",
        "-output-final-mlir=Fooa.mlir",
//        "--emit-omir",
      ),
    )
  )
}
