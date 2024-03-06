package bar
// The high level simulation API: it uses svsim internally
import chisel3.simulator.BetterEphemeralSimulator._
import chisel3.simulator.simSettings
import org.scalatest.flatspec.AnyFlatSpec

object RunBarFoo {
  def apply(c: => BarFoo): Unit = {
    // Inputs and expected results
    val a = Seq(0, 1, 0, 1)
    val b = Seq(0, 0, 1, 1)

    // Reset
    c.io.a.poke(false)
    for (i <- a.zip(b))
      c.io.a.poke(i._1)
  }
}

class BarTest extends AnyFlatSpec {
  behavior of "FooTest"

  it should "trace with underscore" in {
    simulate(new BarFoo, Seq(simSettings.EnableTraceWithUnderscore)) {
      RunBarFoo(_)
    }
  }

  it should "trace" in {
    simulate(new BarFoo, Seq(simSettings.EnableTrace)) { c =>
      c.io.a.poke(true)
    }
  }

}
