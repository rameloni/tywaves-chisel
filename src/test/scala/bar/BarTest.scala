package bar

import org.scalatest.flatspec.AnyFlatSpec
import tywaves.simulator.ParametricSimulator._
import tywaves.simulator.simulatorSettings._
import chisel3.fromBooleanToLiteral
import tywaves.simulator.TywavesSimulator

class BarTest extends AnyFlatSpec {
  behavior of "BarTest"
  it should "trace simple bar" in {
    simulate(
      module = new Bar,
      settings = Seq(VcdTraceWithUnderscore, SaveWorkdir),
      simName = "trace_simple_bar",
    ) { c =>
      c.clock.step()
      c.io.a.poke(true)
      c.io.b.poke(false)
      c.io.out.expect(false.B)

      c.clock.step()
      c.io.a.poke(true)
      c.io.b.poke(true)
      c.io.out.expect(true.B)

      c.clock.step()
      c.clock.step(cycles = 10)
    }
  }

  it should "trace simple bar with tywaves" in {
    import tywaves.simulator.TywavesSimulator._

    simulate(
      module = new Bar,
      settings = Seq(VcdTraceWithUnderscore, SaveWorkdir, WithTywavesWaveforms(true)),
      simName = "trace_simple_bar_with_tywaves",
    ) { c =>
      c.clock.step()
      c.io.a.poke(true)
      c.io.b.poke(false)
      c.io.out.expect(false.B)

      c.clock.step()
      c.io.a.poke(true)
      c.io.b.poke(true)
      c.io.out.expect(true.B)

      c.clock.step()
      c.clock.step(cycles = 10)
    }
  }
}
