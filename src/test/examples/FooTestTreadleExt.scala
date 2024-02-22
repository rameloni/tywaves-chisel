package examples

import chiseltest._
import chiseltest.iotesters.PeekPokeTester
import org.scalatest.flatspec.AnyFlatSpec
import tywaves.tester.TypedChiselScalatestTester
import tywaves.typedTreadle.TypedTreadleBackendAnnotation

class FooTestTreadleExt extends AnyFlatSpec with TypedChiselScalatestTester {
  behavior of "FooTest"

  it should "run peek poke" in {
    test(new Foo)
      .withAnnotations(Seq(WriteVcdAnnotation, TreadleBackendAnnotation))
      .runPeekPoke(new FooPeekPokeTester(_))
  }
  it should "run peek poke with extended treadle" in {
    test(new Foo)
      .withAnnotations(Seq(WriteVcdAnnotation, TypedTreadleBackendAnnotation))
      .runPeekPoke(new FooPeekPokeTester(_))
  }
  it should "run normally" in {
    test(new Foo)
      .withAnnotations(Seq(WriteVcdAnnotation, TreadleBackendAnnotation)) {
        c => RunFoo(c)
      }
  }

  it should "run normally with extended treadle" in {
    test(new Foo)
      .withAnnotations(Seq(WriteVcdAnnotation, TypedTreadleBackendAnnotation)) {
        c => RunFoo(c)
      }
  }


}
