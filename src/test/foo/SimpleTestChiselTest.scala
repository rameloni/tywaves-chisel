package foo

import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import tywaves.circuitmapper.GenerateHgldd
import tywaves.hglddparser.DebugIRParser // Overload chiselSim with chiseltest

class SimpleTestChiselTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "SimpleTestChiselTest"
  it should "generate symbol table with chiseltest" in {
    val workdir     = "test_run_dir/symbolTableTest"
    val ddFile      = GenerateHgldd(() => new Bar, workdir)
    val debugParser = new DebugIRParser(workdir, ddFile)
    debugParser.parse()
    println("Running test: " + it)
    test(new Bar).withAnnotations(Seq(WriteVcdAnnotation)) {
      c =>
        c.io.a.poke(true)
        c.io.b.poke(false)
        c.clock.step()
    }
  }
}
