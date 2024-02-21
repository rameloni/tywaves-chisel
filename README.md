# TyWaves-samples
A repository that implements simple examples to test the feasibility of TyWaves solutions w.r.t. mapping from Chisel level code to final values dumped in trace files by the simulators.


## Key factors to take into account
- Keep in mind that the aim of this repo is to study the feasibility of each solution and **not to implement a solution**. So, it is important to avoid time waste. Getting the solution now is not the main purpose. 
- Maybe an annotation can be created to do the mapping. I am not sure, but I can guess that annotations can be used to push custom functions in the Chisel pipeline. If so, I could execute custom operations.

## Possible solutions
Here a list of identified solutions to map high-level code (Chisel) and values from low level simulation. The first two are based on simulator extensions, while the last one is independent from the simulator.

### 1. Treadle extension
This solution aims to extend a simulator that directly integrates on scala and, as a consequence, that can access (hopefully) chisel elaboration  
- Basic idea: wrap the simulator (create a new class that extends the treadle simulator) in order to add functionalities and output a "new extended vcd" trace file with knowledge about a custom representation.
- The goal is, of course, to keep it compatible with newer versions of treadle. That means, it is not a fork of treadle but, instead, a library that uses treadle.

> A **subtask** of this step is to check the usage of `Annotations` in Chisel. Maybe, they can be used to insert custom operations/instruction/elaborations in the Chisel pipeline. I found an example of a custom annotation [here: a `GtkWaveTransformAnnotation`](https://gist.github.com/kammoh/b3c85db9f2646a664f8dc84825f1bd1d) to be used as `WriteVcdAnnotation`.
>
> Also check `dontTouch` annotation to do so and the Chisel [webpage](https://www.chisel-lang.org/docs/explanations/annotations). Basically, I can try to copy that.

### 2. Verilator extension
This solution will extend another simulator, this time external from the Chisel domain. This would guarantee more extensibility compared to other that is Verilator
- Basic idea: similar to treadle, but now instead this aims to extend verilator for Chisel.
- Verilator is a Verilog/SystemVerilog simulator that compiles the input RTL circuit to a C++ behavioral model. It is the fastest open source simulator available.
- It can be used from chiseltest. This means that Chisel users can simulate their logic with both treadle and verilator.
- Check the FST API.

### 3. Simulator-independent solution
This solution aims to be independent from any simulator used. This means that it would leave the simulators unchanged and the whole mapping and value get is done externally.

> Maybe this solution will make use of either a wrapper, an external program or an annotation

I identified so far 2+1 potential roads to do that:
1. File elaboration: getting tydi (td), chisel (scala), firrtl (fir), trace (vcd/fst) files as inputs and elaborate them.
2. Chisel elaboration: exploit the Chisel elaboration in order to perform some intermediate operations, namely the mapping.

> The +1 solution is related to tydi representation. The tydi case can be reduced to a Chisel case, if the Tydi-lib and Tydi-lang will support decorations.
> 
> Since the tydi code is compiled to chisel, the chisel code can be decorated during this phase, creating a “default” tydi wave decoration that is kinda compatible with other future implementations.
