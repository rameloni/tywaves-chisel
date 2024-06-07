package tywaves.simulator

private[tywaves] object TywavesInterface {

  private val program = tywaves.BuildInfo.surferTywavesBinaryPath

  def run(
      vcdPath:       String,
      hglddDirPath:  Option[String],
      extraScopes:   Seq[String],
      topModuleName: Option[String],
  ): Unit = {
    {
      import scala.sys.process._

      // Check if tywaves is installed
      val exitCode = s"which $program".!
      if (exitCode != 0)
        throw new Exception(s"$program not found on the PATH! Please install it running: make all\n")
    }

    val hglddDirCmd = hglddDirPath match {
      case Some(_) => Some("--hgldd-dir")
      case None    => None
    }

    val extraScopesCmd = (extraScopes, topModuleName) match {
      case (Nil, _) | (_, None) => Nil
      case _ => Seq("--extra-scopes") ++ extraScopes ++
          Seq("--top-module") ++ topModuleName
    }

    val cmd = Seq(program, vcdPath) ++ hglddDirCmd ++ hglddDirPath ++ extraScopesCmd

    // Execute and return to the caller
    val process = new ProcessBuilder(cmd: _*).inheritIO().start()
    // No wait for the process to finish
  }
}
