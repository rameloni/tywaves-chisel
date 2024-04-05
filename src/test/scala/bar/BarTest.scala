package bar

import org.scalatest.flatspec.AnyFlatSpec
import tywaves.simulator.BetterEphemeralSimulator._
import tywaves.simulator.simSettings

import chisel3.fromBooleanToLiteral

class BarTest extends AnyFlatSpec {
  behavior of "BarTest"
  it should "trace simple bar" in {
    simulate(
      module = new Bar,
      settings = Seq(simSettings.EnableTraceWithUnderscore, simSettings.LaunchTywavesWaveforms),
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
}
