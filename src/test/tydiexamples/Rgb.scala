//package tydiexamples
//
//import chisel3._
//import chisel3.util.Counter
//
//import nl.tudelft.tydi_chisel._
//
//object MyTypes {
//
//  /** Bit(8) type, defined in main */
//  def generated_0_16_OqHuFqKi_21 = UInt(8.W)
//
//  assert(this.generated_0_16_OqHuFqKi_21.getWidth == 8)
//}
//
///** Group element, defined in main. */
//class Rgb extends Group {
//  val b = MyTypes.generated_0_16_OqHuFqKi_21
//  val g = MyTypes.generated_0_16_OqHuFqKi_21
//  val r = MyTypes.generated_0_16_OqHuFqKi_21
//}
//
///** Stream, defined in main. */
//class Generated_0_101_bHWhCFjR_22
//    extends PhysicalStreamDetailed(e = new Rgb, n = 2, d = 1, c = 1, r = false, u = Null())
//
//object Generated_0_101_bHWhCFjR_22 {
//  def apply(): Generated_0_101_bHWhCFjR_22 = Wire(new Generated_0_101_bHWhCFjR_22())
//}
//
///** Bit(8), defined in main. */
//class Generated_0_16_OqHuFqKi_21 extends BitsEl(8.W)
//
///** Stream, defined in main. */
//class Generated_0_86_q1AG1GZ7_18
//    extends PhysicalStreamDetailed(e = new Rgb, n = 1, d = 2, c = 1, r = false, u = new Rgb)
//
//object Generated_0_86_q1AG1GZ7_18 {
//  def apply(): Generated_0_86_q1AG1GZ7_18 = Wire(new Generated_0_86_q1AG1GZ7_18())
//}
//
///**
// * Streamlet, defined in main. RGB bypass streamlet documentation.
// */
//class Rgb_bypass extends TydiModule {
//
//  /** Stream of [[io.input]] with input direction. */
//  val inputStream = Generated_0_86_q1AG1GZ7_18().flip
//
//  /** Stream of [[io.input2]] with input direction. */
//  val input2Stream = Generated_0_101_bHWhCFjR_22().flip
//
//  /** Stream of [[io.output]] with output direction. */
//  val outputStream = Generated_0_86_q1AG1GZ7_18()
//
//  /** Stream of [[io.output2]] with output direction. */
//  val output2Stream = Generated_0_101_bHWhCFjR_22()
//
//  // Group of Physical IOs
//  val io = new Bundle {
//
//    /** IO of [[inputStream]] with input direction. */
//    val input = inputStream.toPhysical
//
//    /** IO of [[input2Stream]] with input direction. */
//    val input2 = input2Stream.toPhysical
//
//    /** IO of [[outputStream]] with output direction. */
//    val output = outputStream.toPhysical
//
//    /** IO of [[output2Stream]] with output direction. */
//    val output2 = output2Stream.toPhysical
//  }
//
//}
//
///**
// * Implementation, defined in main. RGB bypass implement documentation.
// */
//class Helloworld_rgb extends Rgb_bypass {
//  // Connections
//  outputStream  := inputStream
//  output2Stream := input2Stream
//
//  inputStream.ready := true.B // This will overload the previous connection
//
//  val accumulate = Counter(Int.MaxValue)
//
//  when(inputStream.valid) {
//    accumulate.value := accumulate.value + (inputStream.el.r + inputStream.el.g + inputStream.el.b) / 3.U
//  }
//
//  when(accumulate.value > 100.U && accumulate.value < 400.U) {
//    input2Stream.ready := true.B
//  }.otherwise(
//    input2Stream.ready := false.B
//  )
//}
