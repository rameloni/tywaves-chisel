package tywaves.simulator

private[tywaves] object TywavesInterface {

  private val program = "surfer-tywaves"

  def run(vcdPath: String, chiselState: Option[String]): Unit = {
    {
      import scala.sys.process._

      // Check if tywaves is installed
      val exitCode = s"which $program".!
      if (exitCode != 0)
        throw new Exception(s"$program not found on the PATH! Please install it running: make all\n")
    }

    val chiselStateCmd = chiselState match {
      case Some(_) => Some("--chisel-state")
      case None    => None
    }
    val cmd = Seq(program, vcdPath) ++ chiselStateCmd ++ chiselState

    // Execute and return to the caller
    val process = new ProcessBuilder(cmd: _*).inheritIO().start()
    // No wait for the process to finish
  }
}
