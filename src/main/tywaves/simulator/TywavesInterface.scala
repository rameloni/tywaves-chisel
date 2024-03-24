package tywaves.simulator

private[tywaves] object TywavesInterface {

  private val program = "surfer-tywaves"

  def apply(vcdPath: String, chiselState: Option[String]): Unit = {

    val chiselStateCmd = chiselState match {
      case Some(_) => Some("--chisel-state")
      case None    => None
    }
    val cmd = Seq(program, vcdPath) ++ chiselStateCmd ++ chiselState

    println(s"Executing: ${cmd.mkString(" ")}")
    // Execute and return to the caller
    val process = new ProcessBuilder(cmd: _*).inheritIO().start()
    // No wait for the process to finish
  }
}
