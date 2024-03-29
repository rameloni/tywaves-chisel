package foo

// The high level simulation API: it uses svsim internally
import tywaves.simulator.BetterEphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import tywaves.simulator.simSettings

object RunFoo {
  /** Run multiple times */
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
    // val testName = testNames.headOption.getOrElse("Unknown Test")
    simulate(new Foo, Seq(simSettings.EnableTraceWithUnderscore), simName = "trace_with_underscore") {
      println("Running test: " + it)
      RunFoo(_)
    }
  }

  it should "trace" in {
    simulate(new Foo, Seq(simSettings.EnableTrace, simSettings.LaunchTywavesWaveforms), simName = "trace") { c =>
      c.io.a.poke(true)
      c.io.b.poke(0)
    }
  }

}
