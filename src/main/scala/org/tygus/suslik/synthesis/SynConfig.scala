package org.tygus.suslik.synthesis

import org.tygus.suslik.language.PrettyPrinting

/**
  * @author Ilya Sergey
  */

case class SynConfig(
                      // Synthesis params
                      startingDepth: Int        = 100,
                      maxOpenDepth: Int         = 1,
                      maxCloseDepth: Int        = 1,
                      branchAbduction: Boolean  = false,
                      phased: Boolean           = true,
                      invert: Boolean           = true,
                      fail: Boolean             = true,
                      commute: Boolean          = true,
                      // Timeout and logging
                      printStats: Boolean = true,
                      printDerivations: Boolean = true,
                      printFailed: Boolean      = false,
                      printTags: Boolean        = false,
                      assertSuccess: Boolean    = true,
                      logToFile: Boolean        = true,
                      timeOut: Long             = DEFAULT_TIMEOUT
                    ) extends PrettyPrinting {

  override def pp: String =
    ( (if (maxOpenDepth == defaultConfig.maxOpenDepth) Nil else List(s"maxOpenDepth = $maxOpenDepth")) ++
      (if (maxCloseDepth == defaultConfig.maxCloseDepth) Nil else List(s"maxCloseDepth = $maxCloseDepth")) ++
      (if (branchAbduction == defaultConfig.branchAbduction) Nil else List(s"branchAbduction = $branchAbduction")) ++
      (if (phased == defaultConfig.phased) Nil else List(s"phased = $phased")) ++
      (if (invert == defaultConfig.invert) Nil else List(s"invert = $invert")) ++
      (if (fail == defaultConfig.fail) Nil else List(s"fail = $fail")) ++
      (if (commute == defaultConfig.commute) Nil else List(s"commute = $commute"))
      ).mkString(", ")
}

case class SynTimeOutException(msg: String) extends Exception(msg)

case class SynthesisException(msg: String) extends Exception(msg)


