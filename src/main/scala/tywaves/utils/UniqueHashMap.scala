package tywaves.utils

import java.nio.file.Paths
import scala.collection.mutable

class UniqueHashMap[K, V] extends mutable.HashMap[K, V] {

  private val _debugList = new mutable.ListBuffer[(K, V)]()
  private val debugList  = new mutable.ListBuffer[(K, V, Int)]()

  /** Same implementation of [[mutable.HashMap.put]] */
  def putOrReplace(key: K, value: V): Option[V] =
    super.put(key, value)

  override def put(key: K, value: V): Option[V] = {
    if (super.contains(key)) {
      this.foreach(Console.err.println(_))

      throw new Exception(s"Key $key already exists in the map. This is likely a bug in the parser.\n" +
        s"Probably a new ID mapping is needed or the element is parsed twice.\n" +
        s"Val is $value.")
    }
    if (_debugList.contains((key, value))) {
      // Get that element and update that element
      val index = _debugList.indexOf((key, value))
      debugList(index) = ((key, value, debugList(index)._3 + 1))
    } else {
      _debugList += ((key, value))
      debugList += ((key, value, 0))
    }
    super.put(key, value)
  }

  def dumpFile(file: String, header: String, append: Boolean = true): Unit = {
    val path = Paths.get(file)
    java.nio.file.Files.createDirectories(path.getParent)
    val bw = new java.io.BufferedWriter(new java.io.FileWriter(file, append))
    bw.write(s"\n$header\n")

    this.foreach { case (key, value) =>
      bw.write(s"$key: $value\n")
    }
    bw.close()
  }

  def log(header: String = ""): Unit = {
    println(s"\n$header\n")
    this.foreach { case (key, value) =>
      println(s"$key: $value")
    }
  }
  def debugLog(): Unit =
    debugList.foreach(println)
}
