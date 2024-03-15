package chisel3.simulator

import chisel3._
import chisel3.reflect.DataMirror
import svsim.{ModuleInfo, Workspace}

package object withargs {

  implicit class ChiselWorkspaceWithArgs(workspace: Workspace) {
    def elaborateGeneratedModule[T <: RawModule](
        generateModule: () => T
    )(firtoolArgs: Seq[String] = Seq()): ElaboratedModule[T] = {
      var someDut: Option[T] = None

      val firtoolOptions = firtoolArgs.map(circt.stage.FirtoolOption)
      Console.err.println(firtoolOptions)
      val outputAnnotations = (new circt.stage.ChiselStage).execute(
        Array("--target", "systemverilog", "--split-verilog"),
        Seq(
          chisel3.stage.ChiselGeneratorAnnotation { () =>
            val dut = generateModule()
            someDut = Some(dut)
            dut
          },
          circt.stage.FirtoolOption("-disable-annotation-unknown"),
          firrtl.options.TargetDirAnnotation(workspace.supportArtifactsPath),
        ) ++
          firtoolOptions,
      )
      Console.println(outputAnnotations)

      // Move the relevant files over to primary-sources
      val filelist =
        new java.io.BufferedReader(new java.io.FileReader(s"${workspace.supportArtifactsPath}/filelist.f"))
      try {
        filelist.lines().forEach { immutableFilename =>
          var filename = immutableFilename
          /// Some files are provided as absolute paths
          if (filename.startsWith(workspace.supportArtifactsPath)) {
            filename = filename.substring(workspace.supportArtifactsPath.length + 1)
          }
          java.nio.file.Files.move(
            java.nio.file.Paths.get(s"${workspace.supportArtifactsPath}/$filename"),
            java.nio.file.Paths.get(s"${workspace.primarySourcesPath}/$filename"),
          )
        }
      } finally {
        filelist.close()
      }

      // Initialize Module Info
      val dut = someDut.get
      val ports = {

        /**
         * We infer the names of various ports since we don't currently have a
         * good alternative when using MFC. We hope to replace this once we get
         * better support from CIRCT.
         */
        def leafPorts(node: Data, name: String): Seq[(Data, ModuleInfo.Port)] =
          node match {
            case record: Record => {
              record.elements.toSeq.flatMap {
                case (fieldName, field) =>
                  leafPorts(field, s"${name}_${fieldName}")
              }
            }
            case vec: Vec[?] => {
              vec.zipWithIndex.flatMap {
                case (element, index) =>
                  leafPorts(element, s"${name}_${index}")
              }
            }
            case element: Element =>
              DataMirror.directionOf(element) match {
                case ActualDirection.Input =>
                  Seq((element, ModuleInfo.Port(name, isGettable = true, isSettable = true)))
                case ActualDirection.Output => Seq((element, ModuleInfo.Port(name, isGettable = true)))
                case _                      => Seq()
              }
          }
        // Chisel ports can be Data or Property, but there is no ABI for Property ports, so we only return Data.
        DataMirror.modulePorts(dut).flatMap {
          case (name, data: Data) => leafPorts(data, name)
          case _                  => Nil
        }
      }
      workspace.elaborate(
        ModuleInfo(
          name = dut.name,
          ports = ports.map(_._2),
        )
      )
      new ElaboratedModule(dut, ports)
    }
  }

}
