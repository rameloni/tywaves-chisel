package tywaves

/**
 * Collection of case classes to parse the HGLDD file using circe:
 * https://circe.github.io/circe/
 */
package object hglddparser {

  @deprecated(since = "0.3.0")
  /** Top component of the `hgldd.dd` file. */
  case class HglddTopInterface(HGLDD: HglddHeader, objects: Seq[HglddObject])

  /**
   * The "header" containing the file information (sources and emitted). For
   * example the scala and SystemVerilog file paths.
   *
   * @param version
   *   The version of the HGLDD
   * @param file_info
   *   The list of files about the generated circuit
   * @param hdl_file_index
   *   The index of the hdl file in the [[file_info]] list
   */
  @deprecated(since = "0.3.0")
  case class HglddHeader(version: String, file_info: Seq[String], hdl_file_index: Int)

  /**
   * An object containing information about the hgl and hdl. This object stores
   * somehow a high level information like the hierarchy and the name used in
   * Chisel. While the sig_name of value contains the hierarchy and verilog
   * name.
   *
   * @param kind
   *   The kind of object (e.g. struct, module, etc.)
   * @param module_name
   *   Optional param. If it is a module it contains the name of the module
   * @param hdl_loc
   *   Optional param to indicate the location of the object in the HDL
   * @param children
   *   Optional param which would contain the names of the children modules of
   *   the object
   */
  @deprecated(since = "0.3.0")
  case class HglddObject(
      hgl_loc:     HglLocation,
      kind:        String,
      obj_name:    String,
      port_vars:   Seq[PortVar],
      module_name: Option[String],
      hdl_loc:     Option[HdlLocation],
      children:    Option[Seq[Child]],
  )

  @deprecated(since = "0.3.0")
  case class HglLocation(
      begin_column: Int,
      begin_line:   Int,
      end_column:   Int,
      end_line:     Int,
      file:         Int,// The index of the file in the file_info list
  )
  @deprecated(since = "0.3.0")
  case class HdlLocation(begin_line: Int, end_line: Int, file: Int) // Imp: I can parse only the fields I need

  /**
   * Represents a port variable.
   *
   * @param var_name
   *   the name of the variable (local name)
   * @param type_name
   *   the type in the HDL
   */
  @deprecated(since = "0.3.0")
  case class PortVar(
      var_name:       String,
      hgl_loc:        HglLocation,
      value:          Option[Value],
      type_name:      String,
      packed_range:   Option[Seq[Int]],
      unpacked_range: Option[Seq[Int]],
  )

  /**
   * A value of a port var.
   *
   * @param bit_vector
   * @param sig_name
   *   the name of the [[PortVar]]. It is thus linked to the
   *   [[PortVar.var_name]]
   * @param opcode
   * @param operands
   *   this is contains values of nested fields for aggregates like bundles and
   *   vecs. So it is possible to rebuild the hierarchy
   */
  @deprecated(since = "0.3.0")
  case class Value(
      bit_vector: Option[String],
      sig_name:   Option[String],
      opcode:     Option[String],
      operands:   Option[Seq[Value]],
  )

  @deprecated(since = "0.3.0")
  case class Child(name: String, obj_name: String, module_name: String, hgl_loc: HglLocation, hdl_loc: HdlLocation)

}
