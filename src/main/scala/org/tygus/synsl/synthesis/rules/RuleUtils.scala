package org.tygus.synsl.synthesis.rules

import org.tygus.synsl.SynSLException
import org.tygus.synsl.language.Statements.{Load, SeqComp, Statement}
import org.tygus.synsl.synthesis.{StmtProducer, Subderivation}

/**
  * @author Ilya Sergey
  */

trait RuleUtils {

  val exceptionQualifier: String

  case class SynthesisRuleException(msg: String) extends SynSLException(exceptionQualifier, msg)

  protected[synthesis] def ruleAssert(assertion: Boolean, msg: String): Unit = if (!assertion) throw SynthesisRuleException(msg)

  def pureKont(rulename: String): StmtProducer =
    stmts => {
      ruleAssert(stmts.lengthCompare(1) == 0, s"Rule $rulename expects 1 premise and got ${stmts.length}")
      stmts.head
    }

  def prepend(s: Statement, rulename: String): StmtProducer =
    stmts => {
      ruleAssert(stmts.lengthCompare(1) == 0, s"Rule $rulename expects 1 premise and got ${stmts.length}")
      val rest = stmts.head
      s match {
        // Do not generate read for unused variables
        case Load(y, tpy, x, offset) => if (rest.usedVars.contains(y)) SeqComp(s, rest) else rest
        case _ => SeqComp(s, rest)
      }
  }

  def append(s: Statement, rulename: String): StmtProducer =
    stmts => {
      ruleAssert(stmts.lengthCompare(1) == 0, s"Rule $rulename expects 1 premise and got ${stmts.length}")
      val rest = stmts.head
      SeqComp(rest, s)
    }


  // Sort a sequence of alternative subderivations (where every subderivation contains a single goal)
  // by the footprint of their latest rule application,
  // so that sequential applications of the rule are unlikely to cause out-of-order derivations
  def sortAlternativesByFootprint(alts: Seq[Subderivation]): Seq[Subderivation] = {
    alts.sortBy(_.subgoals.head.deriv.applications.head)
  }

  def nubBy[A,B](l:List[A], p:A=>B):List[A] =
  {
    def go[A,B](l:List[A], p:A=>B, s:Set[B], acc:List[A]):List[A] = l match
    {
      case Nil => acc.reverse
      case x::xs if s.contains(p(x)) => go(xs,p,s,acc)
      case x::xs                     => go(xs,p,s+p(x),x::acc)
    }
    go(l,p,Set.empty,Nil)
  }
}
