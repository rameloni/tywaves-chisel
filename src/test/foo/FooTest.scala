package foo

// The high level simulation API: it uses svsim internally
import chisel3.simulator.BetterEphemeralSimulator._
import chisel3.simulator.simSettings
import org.scalatest.flatspec.AnyFlatSpec

object RunFoo {
  def apply(c: => Foo): Unit = {
    // Inputs and expected results
    val a = Seq(0, 1, 0, 1)
    val b = Seq(0, 0, 1, 1)

    // Reset
    c.io.a.poke(false)
    c.io.b.poke(true)
    c.clock.step()

    for (i <- a.zip(b)) {
      c.io.a.poke(i._1)
      c.io.b.poke(i._2)
      c.clock.step()
    }
  }
}

class FooTest extends AnyFlatSpec {
  behavior of "FooTest"

  it should "trace with underscore" in {
    simulate(new Foo, Seq(simSettings.EnableTraceWithUnderscore)) {
      RunFoo(_)
    }
  }

  it should "trace" in {
    simulate(new Foo, Seq(simSettings.EnableTrace)) { c =>
      c.io.a.poke(true)
      c.io.b.poke(0)
    }
  }

}
