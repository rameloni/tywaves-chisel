package tywaves.tester

import chisel3.Module
import chiseltest.simulator.{Compiler, TypedTreadleSimulator}
import chiseltest.{ChiselScalatestTester, TestResult}
import firrtl2.AnnotationSeq
import org.scalatest.TestSuite
import tywaves.typedTreadle.ChiselMapper

/**
 * Wraps the [[ChiselScalatestTester]] by overriding its methods in order to invoke another simulator backend rather
 * than the currently supported.
 *
 * Its usage is the same as the [[ChiselScalatestTester]].
 * {{{
 *   class MyTest extends AnyFlatSpec with TypedChiselScalatestTester {
 *      // ...
 *   }
 * }}}
 */
trait TypedChiselScalatestTester extends ChiselScalatestTester {
  this: TestSuite =>
  private val DEBUG = true

  private val DEBUG_VERBOSE = true

  /** A [[TestBuilder]] wrapper */
  class TypedTestBuilder[T <: Module](
                                       override val dutGen: () => T,
                                       override val annotationSeq: AnnotationSeq,
                                       override val chiselAnnotationSeq: firrtl.AnnotationSeq) extends
    TestBuilder(dutGen, annotationSeq, chiselAnnotationSeq) {

//    val (dut, highFirrtl, lowFirrtl) = TypedTreadleSimulator.elaborate(dutGen, annotationSeq, chiselAnnotationSeq)
    val mapper = new ChiselMapper(dutGen, annotationSeq, chiselAnnotationSeq)
    //    override def apply(testFn: T => Unit): TestResult = {
    //      //      println(console_color + "TypedTestBuilder.apply")
    //      val tester = defaults.createDefaultTester(dutGen, finalAnnos, chiselAnnotationSeq)
    //      println("tester=" + tester)
    //
    //      //      if (DEBUG) {
    //      //        print(console_color(DEBUG))
    //      //        val ann = defaults.addDefaultSimulator(finalAnnos)
    //      //        for (ann <- ann) {
    //      //          println("ann=" + ann + " type=" + ann.getClass)
    //      //        }
    //      //        println("annotationSeq=" + ann)
    //      //      }
    //      // reset the color
    //      print(Console.RESET)
    //      runTest(tester)(testFn)
    //    }
    // Step 3: The apply function is called by the testbench
    override def apply(testFn: T => Unit): TestResult = {
      println(consoleColor + "TypedTestBuilder.apply" + Console.RESET)

      mapper.printCircuit()

      super.apply(testFn)
    }

    /** Override in order to return the [[TypedTestBuilder]].
     *
     * Internally it creates the original [[TestBuilder]] and it converts it using [[this]] method.
     *
     * STEP 2: Add the annotations to the unit test
     *
     */
    override def withAnnotations(annotationSeq: AnnotationSeq): TypedTestBuilder[T] = {
      println(consoleColor + "TypedTestBuilder.withAnnotations" + Console.RESET)
      if (DEBUG_VERBOSE) println("Annotations passed to the test: " + consoleColor(true) + annotationSeq + Console.RESET)
      //      val builder = super.withAnnotations(annotationSeq)
      new TypedTestBuilder(super.withAnnotations(annotationSeq))
    }


    /** Conversion from TestBuilder to TypedTestBuilder */
    def this(testBuilder: TestBuilder[T]) = {
      this(testBuilder.dutGen, testBuilder.annotationSeq, testBuilder.chiselAnnotationSeq)
    }

  } // TypedTestBuilder

  /** Overriding method to return a new [[TypedTestBuilder]] instead of a [[TestBuilder]]
   *
   * STEP 1: Build the unit test
   */
  override def test[T <: Module](dutGen: => T): TypedTestBuilder[T] = {
    println(consoleColor + "TypedChiselTester.test" + Console.RESET)
    new TypedTestBuilder(() => dutGen, Seq(), Seq()) // Checkme: old is new TestBuilder(() => dutGen, Seq(), Seq())
  }


  /** Debugging function */
  private def consoleColor(debug: Boolean): String = if (debug) {
    Console.BLUE
  } else {
    Console.GREEN
  }

  private def consoleColor: String = consoleColor(false)

}
