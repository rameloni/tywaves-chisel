package examples

import chiseltest._
import chiseltest.iotesters.PeekPokeTester
import org.scalatest.flatspec.AnyFlatSpec

class FooPeekPokeTester(c: Foo) extends PeekPokeTester(c) {

  // Inputs and expected results
  val a = Seq(0, 1, 0, 1)
  val b = Seq(0, 0, 1, 1)

  // Reset
  poke(c.io.a, 0)
  poke(c.io.b, 1)
  step(1)

  for (i <- a.zip(b)) {
    poke(c.io.a, i._1)
    poke(c.io.b, i._2)
    step(1)
  }

}

object RunFoo {
  def apply(c: Foo): Unit = {
    // Inputs and expected results
    val a = Seq(0, 1, 0, 1)
    val b = Seq(0, 0, 1, 1)

    // Reset
    c.io.a.poke(0)
    c.io.b.poke(1)
    c.clock.step()

    for (i <- a.zip(b)) {
      c.io.a.poke(i._1)
      c.io.b.poke(i._2)
      c.clock.step()
    }
  }
}


class FooTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "FooTest"

  it should "run peek poke" in {
    test(new Foo)
      .withAnnotations(Seq(WriteVcdAnnotation, TreadleBackendAnnotation))
      .runPeekPoke(new FooPeekPokeTester(_))
  }

  it should "run normally" in {
    test(new Foo)
      .withAnnotations(Seq(WriteVcdAnnotation, TreadleBackendAnnotation)) {
        c => RunFoo(c)
      }
  }


}
