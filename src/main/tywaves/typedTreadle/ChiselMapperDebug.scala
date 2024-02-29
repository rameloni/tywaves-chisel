package chisel3
package tywaves.typedTreadle

//import chisel3.RawModule

import chisel3.experimental.hierarchy.core.{Definition, IsInstantiable}
import chisel3._
import chisel3.experimental.BaseModule
import chisel3.internal.naming.NamingContext
import chisel3.stage.{ChiselCircuitAnnotation, ChiselGeneratorAnnotation}
import tywaves.typedTreadle.UseConverter
import firrtl2.transforms.DontTouchAnnotation

//import chisel3.internal.firrtl.{ir => chisel3firrtl}
private object Color {
  val RESET = Console.RESET
  val COMPONENT = Console.YELLOW
  val BUNDLE = Console.CYAN
  val DATA = Console.RED
  val PORT = Console.GREEN
  val DEFMODULE = Console.MAGENTA
  val COMMAND = Console.BOLD
  val CYAN = Console.CYAN
  val WHITE = Console.WHITE

  var colorStack = List(RESET)

  def apply(color: String): Unit = {
    colorStack = color :: colorStack
    print(color)
  }

  def reset(): Unit = {
    colorStack = colorStack.tail
    print(Console.RESET)
    print(colorStack.head)
  }

}

object ChiselMapperDebug {

  @deprecated("To be used in a builder context")
  def useDefinition[T <: BaseModule with IsInstantiable](proto: => T): Unit = {
    val x = Definition(proto)
    println(x)
  }

  private def tabs(n: Int): String = "\t" * n

  def printCircuit(annos: firrtl.AnnotationSeq): Unit = {
    annos.foreach {
      case c: ChiselCircuitAnnotation => println(c.serialize)
      case other => println("Not a ChiselCircuitAnnotation: " + other.getClass.getName)
    }

    val circuit: chisel3.internal.firrtl.Circuit = annos.collectFirst { case c: ChiselCircuitAnnotation => c }.get.circuit

    println(circuit.firrtlAnnotations.mkString("\n"))
    println(circuit.components.mkString("\n"))
    print("renames: ")
    circuit.renames.serialize

    circuit.components.foreach(c => printComponent(c))

  }

  def printComponent(c: chisel3.internal.firrtl.Component, nestedLevel: Int = 0): Unit = {
    Color(Color.COMPONENT)
    println(tabs(nestedLevel) + "Component:")
    println(tabs(nestedLevel) + "id: " + c.id)
    println(tabs(nestedLevel) + "name: " + c.name)

    // Print the ports
    c.ports.foreach(p => printPort(p, nestedLevel + 1, c))

    c match {
      case defModule: chisel3.internal.firrtl.DefModule => printDefModule(defModule, nestedLevel + 1)
    }
    Color.reset()

  }

  def printDefModule(d: chisel3.internal.firrtl.DefModule, nestedLevel: Int = 0): Unit = {
    Color(Color.DEFMODULE)
    println(tabs(nestedLevel) + "DefModule:")
    println(tabs(nestedLevel) + "id: " + d.id)
    println(tabs(nestedLevel) + "name: " + d.name)

    // Print the ports
    d.ports.foreach(p => printPort(p, nestedLevel + 1, component = d))
    d.commands.foreach(c => printCommand(c, nestedLevel + 1))
    //    d match {
    //      case defModule: chisel3.internal.firrtl.DefModule =>
    //        println(tabs(nestedLevel) + "Command: " + defModule.getClass.getName)
    //    }
    Color.reset()
  }

  /** Print a [[chisel3.internal.firrtl.Command]] */
  def printCommand(command: chisel3.internal.firrtl.Command, i: Int): Unit = {
    Color(Color.COMMAND)
    println(tabs(i) + "Command: " + command.getClass.getName)
    println(tabs(i) + "info: " + command.sourceInfo)

    command match {
      case defPrim: chisel3.internal.firrtl.DefPrim[Data] => {
        println(tabs(i) + "DefPrim: " + defPrim)
        defPrim.args.foreach(a => printArg(a, i + 1))
      }
      case connect: chisel3.internal.firrtl.Connect => println(tabs(i) + "Connect: " + connect)
      case _ => println(tabs(i) + "Not found: " + command.getClass.getName)
    }
    //    println(tabs(i) + "info: " + command.)
    Color.reset()
  }

  def printArg(arg: chisel3.internal.firrtl.Arg, i: Int): Unit = {
    Color(Color.COMMAND)
    println(tabs(i) + "Arg: " + arg.getClass.getName)

    //    println(tabs(i) + "contextualName: " + arg.contextualName)
    //    println(tabs(i) + "fullName:       " + arg.fullName)
    println(tabs(i) + "localName:      " + arg.localName)
    println(tabs(i) + "name:           " + arg.name)
    Color.reset()
  }

  /** Print a [[chisel3.internal.firrtl.Port]] */
  def printPort(p: chisel3.internal.firrtl.Port, nestedLevel: Int = 0, component: chisel3.internal.firrtl.Component) = {
    Color(Color.PORT)
    Color(Color.CYAN)
    val convertedPort = UseConverter.convert(p, p.dir)
    println("ConvertedPort: " + convertedPort)
    println("TopLevelType: " + p.id.getClass.getName)
    println("OriginalPort:  " + p)

    Color.reset()
    println(tabs(nestedLevel) + "Name Port: " + Name(p.id).unpack(component))
    println(tabs(nestedLevel) + "FullName Port: " + FullName(p.id).unpack(component))
    println(tabs(nestedLevel) + "Start Port: " + p.id.typeName)
    println(tabs(nestedLevel) + "id: " + p.id + "  ")
    printData(p.id, nestedLevel + 1, component = component)

    println(tabs(nestedLevel) + "direction: " + p.dir)
    println(tabs(nestedLevel) + "info: " + p.sourceInfo)
    println(tabs(nestedLevel) + "End Port: " + p.id.typeName)
    println()
    Color.reset()
  }

  /** Print a [[Data]] */
  def printData(data: chisel3.Data, nestedLevel: Int = 0, component: chisel3.internal.firrtl.Component): Unit = {
    Color(Color.DATA)
    printHasIdInformation(data, nestedLevel+1)
    println(tabs(nestedLevel) + firrtl.transforms.DontTouchAnnotation(data.toAbsoluteTarget).serialize)
    println(tabs(nestedLevel) + MyAnnotation.serialize(data))
    println(tabs(nestedLevel) + "Name Data:        " + Name(data).unpack(component))
    println(tabs(nestedLevel) + "FullName Data:    " + FullName(data).unpack(component))
    println(tabs(nestedLevel) + "Data:             " + data.getClass.getName)
    println(tabs(nestedLevel) + "width:            " + data.getWidth)
    println(tabs(nestedLevel) + "InstanceName:     " + data.instanceName)
    println(tabs(nestedLevel) + "pathName:         " + data.pathName)
    println(tabs(nestedLevel) + "parentPathName:   " + data.parentPathName)
    println(tabs(nestedLevel) + "parentModName:    " + data.parentModName)
    println(tabs(nestedLevel) + "toNamed:          " + data.toNamed)
    println(tabs(nestedLevel) + "toTarget:         " + data.toTarget)
    println(tabs(nestedLevel) + "toAbsoluteTarget: " + data.toAbsoluteTarget)
    val targ = data.toTarget
    val absTarg = data.toAbsoluteTarget
    //    println(tabs(nestedLevel) + "info: " + data.sourceInfo)

    // Check if Data is a composed type
    data match {
      case b: chisel3.Bundle => printBundle(b, nestedLevel + 1, component = component)
      case v: chisel3.Vec[Data] => printVec(v, nestedLevel + 1, component = component)
      case other => println(tabs(nestedLevel) + "Not found: " + other.getClass.getName)
    }
    Color.reset()
  }

  def printHasIdInformation(data: chisel3.InstanceId, nestedLevel: Int = 0):Unit = {
    Color(Console.UNDERLINED + Console.CYAN)
    println(tabs(nestedLevel) + "InstanceId or HasId")
    println(tabs(nestedLevel) + "InstanceName:   " +  data.instanceName)
    println(tabs(nestedLevel) + "pathName:       " +  data.pathName)
    println(tabs(nestedLevel) + "parentPathName: " +  data.parentPathName)
    println(tabs(nestedLevel) + "parentModName:  " +  data.parentModName)
    println(tabs(nestedLevel) + "toNamed:        " +  data.toNamed)
    println(tabs(nestedLevel) + "toTarget:       " +  data.toTarget)
    println(tabs(nestedLevel) + "toAbsoluteTarget: " +  data.toAbsoluteTarget)
    Color.reset()
  }

  /** Print a Bundle */
  def printBundle(b: chisel3.Bundle, nestedLevel: Int = 0, component: chisel3.internal.firrtl.Component): Unit = {
    Color(Color.BUNDLE)
    println(tabs(nestedLevel) + "Start Bundle: " + b.className)
    b.elements.foreach(e => {
      printData(e._2, nestedLevel + 1, component = component)
      println()
    })
    println(tabs(nestedLevel) + "End Bundle: " + b.getClass.getName)
    Color.reset()
  }

  def printVec(v: chisel3.Vec[Data], nestedLevel: Int = 0, component: chisel3.internal.firrtl.Component): Unit = {
    println(tabs(nestedLevel) + "Vec: " + v.getClass.getName)
    v.foreach(e => printData(e, nestedLevel + 1, component = component))
  }


  // Print the difference between the elaboration, aspect and conversion
  def printDiffCirc[M <: RawModule](c: ChiselMapper[M]): Unit = {
    val elabCircString = c.converterAnnos.collectFirst { case c: ChiselCircuitAnnotation => c }.get.serialize
    val aspeCircString = c.aspectAnnos.collectFirst { case c: ChiselCircuitAnnotation => c }.get.serialize
    val convCircString = c.converterAnnos.collectFirst { case c: ChiselCircuitAnnotation => c }.get.serialize

    // Check the differences
    if (elabCircString != aspeCircString) {
      println("Elaboration and Aspect are different")
    }

    if (elabCircString != convCircString) {
      println("Elaboration and Conversion are different")
    }

    if (aspeCircString != convCircString) {
      println("Aspect and Conversion are different")
    }
  }
}
