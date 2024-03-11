import chisel3.Bundle
import org.scalatest.flatspec.AnyFlatSpec

import scala.reflect.runtime.universe._
class MyBundle extends Bundle
class GetNameTest extends AnyFlatSpec {
  behavior of "GetNameTest"
  def getClassName[T: TypeTag](obj: T): String = {
    val className = typeOf[T].typeSymbol.asClass.name.toString
    s"$className"
  }
  def getName(obj: Any): String =
    obj.getClass.getCanonicalName match {
      case null => "Unknown Class Name"
      case _    => obj.getClass.getCanonicalName
    }

  def printNames(obj: Bundle): Unit = {
    println("GetSimpleName: " + obj.getClass.getSimpleName)
    println("GetName: " + obj.getClass.getName)
    println("GetCanonicalName: " + obj.getClass.getCanonicalName)
    println("GetTypeName: " + obj.getClass.getTypeName)
    println("GetType: " + obj.className)
  }

  it should "get anonymous class name" in {
    val anonBundle = new Bundle {}
    printNames(anonBundle)
    println(anonBundle.className)
    println(getClassName(anonBundle))
//    println(getClassName(an))
  }

  it should "get named class name" in {
    val namedBundle = new MyBundle
    printNames(namedBundle)
    println(getClassName(namedBundle))

  }

  it should "get anonymous class name from named class" in {
    val anonBundle = new MyBundle {}
    printNames(anonBundle)
    println(getClassName(anonBundle))

  }

}
