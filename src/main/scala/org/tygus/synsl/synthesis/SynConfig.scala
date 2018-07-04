package org.tygus.synsl.synthesis

import org.tygus.synsl.language.PrettyPrinting
import org.tygus.synsl.SynSLException
import scala.Console.RED

/**
  * @author Ilya Sergey
  */

case class SynConfig( // Timeout and logging
                      printDerivations: Boolean       = true,
                      assertSuccess: Boolean          = true,
                      timeOut: Long                   = DEFAULT_TIMEOUT,
                      // Synthesis params
                      startingDepth: Int              = 100,
                      maxOpenDepth: Int               = 1,
                      maxCloseDepth: Int              = 1,
                      branchAbductionEnabled: Boolean = false
                    ) extends PrettyPrinting {
  override def pp: String =
    List(s"maxOpenDepth = $maxOpenDepth",
      s"maxCloseDepth = $maxCloseDepth",
      s"branchAbductionEnabled = $branchAbductionEnabled")
      .mkString(", ")
}

case class SynTimeOutException(msg: String) extends Exception(msg)

case class SynthesisException(msg: String) extends Exception(msg)

