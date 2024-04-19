# Tywaves demo project: Chisel backend

Demo backend of the Tywaves project: a type based waveform viewer for Chisel
and [Tydi-Chisel](https://github.com/abs-tudelft/Tydi-Chisel) circuits.

This repo contains functions that parse the information of a Chisel circuit and the debug info emitted by the firtool
compiler, simulate a circuit using ChiselSim and combine all the information and pass them to the
[surfer-tywaves-demo](https://gitlab.com/rameloni/surfer-tywaves-demo) frontend for visualization (an extension of the
Surfer waveform viewer written in Rust to support Chisel constructs).

Since it is a demo project, it has been tested and developed for a restricted set of examples (it may not work with
all circuits). In the [features](#features) section, you can find the already supported features.
This demo served only as an example to show how the waveform viewer can potentially look like and **for initial feedback
and suggestions from the community**.

> If you are interested in using the tool and have any feedback on its implementation, please open an issue or contact
> me.

**Starting from this demo project, a better implementation and integration in the Chisel/Firrtl infrastructure will be
developed with the aim of solving the issues
addressed [here](https://github.com/rameloni/Tydi-Chisel-testing-frameworks-analysis).**


> Do not use Verilog / System Verilog reserved keywords in your Chisel circuit (i.e. `wire`, `reg`).
> In that case, `firtool` will change the `var_name` (which should be `wire`) field of the emitted HGLDD to get a legal
> name as explained in the comments
> [here](https://github.com/llvm/circt/blob/37fbe5e5f5c7a07064d02cea8bf4e8454178fc0e/lib/Target/DebugInfo/EmitHGLDD.cpp#L163C1-L175C2).
> Thus, it is not possible to match Chisel/FIRRTL with the `var_name` in HGLDD.
>
> HGLDD is a file format emitted for other existing tools based on verilog simulations and verilog keywords.
> **I am using it temporarily and in the future firtool will be able to emit a new file format more consistent with
> tywaves.**

# Table of contents

- [Installation](#installation)
    - [Install surfer-tywaves-demo](#install-surfer-tywaves-demo)
    - [Publish locally this scala project](#publish-locally-this-scala-project)
- [Use it on your project](#use-it-on-your-project)
- [Example output](#example-output)
- [Features](#features)
- [How it works internally](#how-it-works-internally)
    - [Drawbacks](#drawbacks)

# Installation

You can run `make all` to install all the pre-requisites and **this library**.

## Prerequisite: Install [surfer-tywaves-demo](https://gitlab.com/rameloni/surfer-tywaves-demo/-/tree/tywaves)

The makefile contains a rule to clone the frontend repository, build and install it.

```bash
make install-surfer-tywaves
make clean # To remove the cloned repository
```

The frontend will be installed as `surfer-tywaves` executable.

## Install and publish locally this library

```bash
make install-chisel-fork # TEMPORARY NEEDED: Install the chisel fork with the needed changes in the development branch
make install-tywaves-backend
```

Once published locally, the `tywaves-demo-backend` can be used by adding the following line to the `build.sbt` file:

```scala
libraryDependencies += "com.github.rameloni" %% "tywaves-demo-backend" % "0.1.0-SNAPSHOT"
```

# Use it on your project

The `TywavesBackend` provides 2 simulators with functionalities to simulate a circuit
through [svsim](https://github.com/chipsalliance/chisel/tree/main/svsim), emit VCD
traces and generate the symbol table for the surfer-tywaves waveform viewer itself automatically:

- [ParametricSimulator](./src/main/scala/tywaves/simulator/ParametricSimulator.scala): provides some generic features
  such as VCD trace emission, name the trace file, pass additional arguments to firtool before simulation, save the
  workspace of svsim
- [TywavesSimulator](./src/main/scala/tywaves/simulator/TywavesSimulator.scala): it extends the parametric simulator in
  order to generate the symbol table for Tywaves waveform viewer and provides an option to launch the waveform viewer
  after the simulation

> While `TywavesSimulator` is a central part of the Tywaves project and its functionalities are not fully supported
> yet, the `ParametricSimulator` is able to simulate any Chisel circuit. In case you need to simulate a circuit that is
> not supported by `TywavesSimulator`, you can use `ParametricSimulator` to emit a VCD trace (however, you will not have
> a "chisel" representation of the signals in the waveform viewer).
>
> If you want to try the functionalities of `Tywaves` then `TywavesSimulator` is the right choice.
> But, if you want to visualize waveforms of any chisel circuit without issues related to features not supported yet,
> you should make use of `ParametricSimulator`.

The following example shows how it is possible also to:

- Enable the trace of the simulation
- Set the name of the simulation (it will be used to create a folder with a user defined name for the traces and
  workspace of svsim)
- Launch the waveform viewer after the simulation
- Use tywaves and expect API to test the circuit

### Use TywavesSimulator

```scala
import tywaves.simulator.TywavesSimulator._
import tywaves.simulator.simulatorSettings._
import org.scalatest.flatspec.AnyFlatSpec

class GCDTest extends AnyFunSpec with Matchers {
  describe("TywavesSimulator") {
    it("runs GCD correctly") {
      simulate(new GCD(), Seq(VcdTrace, WithTywavesWaveforms(true)), simName = "runs_GCD_correctly_launch_tywaves") {
        gcd =>
          gcd.io.a.poke(24.U)
          gcd.io.b.poke(36.U)
          gcd.io.loadValues.poke(1.B)
          gcd.clock.step()
          gcd.io.loadValues.poke(0.B)
          gcd.clock.stepUntil(sentinelPort = gcd.io.resultIsValid, sentinelValue = 1, maxCycles = 10)
          gcd.io.resultIsValid.expect(true.B)
          gcd.io.result.expect(12)
      }
    }
  }
}
```

### Use ParametricSimulator

```scala
import tywaves.simulator.ParametricSimulator._
import tywaves.simulator.simulatorSettings._
import org.scalatest.flatspec.AnyFlatSpec

class GCDTest extends AnyFunSpec with Matchers {
  describe("ParametricSimulator") {
    it("runs GCD correctly") {
      simulate(new GCD(), Seq(VcdTrace, SaveWorkdirFile("GCD_parametricSimulator_workdir")), simName = "runs_GCD_correctly") {
        gcd =>
          gcd.io.a.poke(24.U)
          gcd.io.b.poke(36.U)
          gcd.io.loadValues.poke(1.B)
          gcd.clock.step()
          gcd.io.loadValues.poke(0.B)
          gcd.clock.stepUntil(sentinelPort = gcd.io.resultIsValid, sentinelValue = 1, maxCycles = 10)
          gcd.io.resultIsValid.expect(true.B)
          gcd.io.result.expect(12)
      }
    }
  }
}
```

# Example output

The following images show the classic and tywaves waveform visualization of the [GCD](./src/test/scala/gcd/GCD.scala)
module.
It is possible to see that the left picture does not provide any information about Chisel level types and hierarchy.

```scala
class GCD extends Module {
  val io = IO(new Bundle {
    val a             = Input(UInt(32.W))
    val b             = Input(UInt(32.W))
    val loadValues    = Input(Bool())
    val result        = Output(UInt(32.W))
    val resultIsValid = Output(Bool())
  })

  val x = Reg(UInt(32.W))
  val y = Reg(UInt(32.W))

  when(x > y)(x := x -% y).otherwise(y := y -% x)
  when(io.loadValues) {
    x := io.a
    y := io.b
  }
  io.result := x
  io.resultIsValid := y === 0.U
}
```

| Only VCD loaded                                    | Tywaves (VCD + symbol table)                                |
|----------------------------------------------------|-------------------------------------------------------------|
| ![VCD GCD waveform](./images/vcd-gcd-waveform.png) | ![Tywaves GCD waveform](./images/tywaves-gcd-waveforms.png) |

# Features

- [x] Parse and map Chisel/FIRRTL/Verilog circuits
- [x] Emit VCD traces from the simulator (both with and without underscores in the signal names)
- [x] Automatically generate the symbol table for the waveform viewer
    - [x] Dump Chisel types in the final symbol table
    - [x] Represent hierarchical structures of bundles
    - [ ] Represent vectors
    - [ ] Represent enums
    - [ ] Represent hierarchical modules
        - [x] Generic submodules (all different types of modules)
        - [x] Variants of the same module (i.e. parametric module)
        - [ ] Instances of the same module
    - [ ] For loops code generation
    - [ ] Reg with init

# How it works internally

The following diagram shows the main components of the demo project and how they interact with each other.
![Tywaves backend diagram](./images/tywaves-backend-diagram.png)

This backend retrieves, parses and finally maps together the Intermediate Representations (IR) of the Chisel, Firrtl and
debug info emitted by the firtool (HGLDD) to output a symbol table that can be used by the frontend to display the
waveform.
It aims to map each high level signal (Chisel) to the low level signal (in System Verilog and in the VCD/FST trace) and
vice versa. In this way it would be possible to access a variable/signal value from any waveform viewer able to support
a **multi-level typed** view. For this demo I managed to do so by:

- parsing Chisel IR, Firrtl IR, and the HGLDD (debug info emitted by firtool to link verilog and firrtl)
- retrieving and joining signals together by identifying IDs (shared between IRs) based on signal names and source
  locators
- emitting the symbol table suited for the waveform viewer

However, this approach has some issues associated with the IRs parsing and mapping. The [drawbacks](#drawbacks) section
explains them and suggests a solution to them that I will implement.

Considering only Firrtl, using the HGLDD file would be enough, but it does provide only information about
FIRRTL-to-SystemVerilog mapping, so it does not contain user types information.

In this small example if I use only HGLDD I would be able to see that they are both bundles, but it is not possible to
see that they are actually `MyFloat` and `IntCoordinates` respectively. Also `Bool`, `UInt`, `SInt` would not be
retrieved from HGLDD/FIRRTL only. From here the reason to use Chisel IR to get the user types information.

```scala
class MyFloat extends Bundle {
  val sign        = Bool()
  val exponent    = UInt(8.W)
  val significand = UInt(23.W)
}

class IntCoordinates extends Bundle {
  val x = SInt(32.W)
  val y = SInt(32.W)
}
```

## Drawbacks

The current approach accesses and uses the Chisel IR which is private to the package `chisel3` since it is part of the
chisel **internals,** and it is not meant to be used by external tools. It is not stable, it may change and this may
compromise compatibility with future chisel versions. Using Chisel IR in this way will make relatively hard to maintain
this project for future Chisel versions.

Mapping different IRs together requires to find common characteristics between them. Different IRs have different
information, reserved keywords and syntax. This can lead to different ways to represent variables and changes to their
names. For example, a variable `x` child of a bundle `b` may be represented with `b_x` in Verilog, but it is not
guaranteed when there are some conflicting variables.
Therefore, this methodology requires to find an ID for each signal which is unique within the same IR, but it
is shared between IRs. Finding an ID with these characteristics is not trivial at all since it really depends on the
characteristics emitted during the different elaboration/compilation phases. These information, even if available,
should remain consistent between different versions of the tools but this may not be guaranteed.
Currently, the IDs (`ElId`) used by the tool to join the different IRs are based on source locators (where a variable is
declared in a **source** module, not instance) contained in the IRs and names of the variables. However, this may cause
issues when signals and modules are generated using for loops. This explains why multiple instances of the same module
are not supported yet (the source locators of internal signals of multiple instances of the same module are the same).
Furthermore, finding the original chisel name from HGLDD requires manipulation based on the transform function used.

```scala
case class ElId(source: String, row: Int, col: Int, name: String)
```

Finally, this tool relies on HGLDD which is a file realized for Synopsys tools and its format is not stable since it
mainly depends on what Synopsys will need in the future.

These issues reveal the need for a more stable and consistent way to map different IRs together. Parsing the IRs
externally and joining them basing on a "guessed and unstable" ID is not an optimal solution (guessed and unstable since
it depends on internal characteristics of compilers).
Therefore, I planned to "integrate" a functionality to directly transfer Chisel information to FIRRTL. In this way,
the `firtool` would be able to access all the needed information for `surfer-tywaves-demo` to render the signals.
This would also allow to simplify the process that `tywaves-demo-backend` currently does to generate the symbol table,
improving performances. And it may extend the support to other languages/dialects in
the [CIRCT](https://circt.llvm.org/) ecosystem.

Despite the drawbacks, this demo successfully shows the potential of the Tywaves project and the feasibility of a Typed
Waveform Viewer for Chisel circuits.
