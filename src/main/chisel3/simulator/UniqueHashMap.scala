package chisel3.simulator

import scala.collection.mutable

class UniqueHashMap[K, V] extends mutable.HashMap[K, V] {
  override def put(key: K, value: V): Option[V] = {
    if (super.contains(key))
      throw new Exception(s"Key $key already exists in the map. This is likely a bug in the parser. " +
        s"Probably a new ID mapping is needed or the element is parsed twice.")
    super.put(key, value)
  }
}