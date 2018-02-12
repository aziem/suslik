package org.tygus.synsl.logic

import org.tygus.synsl.{PrettyPrinting, Substitutable}
import org.tygus.synsl.language.Expressions.{Expr, Var, Ident}

/**
  * Separation logic fragment
  */
sealed abstract class Heaplet extends PrettyPrinting with Substitutable[Heaplet] {
  // Collect certain sub-expressions
  def collectE[R <: Expr](p: Expr => Boolean): Set[R] = {
    def collector(acc: Set[R])(h: Heaplet): Set[R] = h match {
      case PointsTo(v, offset, value) =>
        val acc1 = if (p(v)) acc + v.asInstanceOf[R] else acc
        acc1 ++ value.collect(p)
      case Block(v, sz) =>
        if (p(v)) acc + v.asInstanceOf[R] else acc
      case SApp(_, args) => args.foldLeft(acc)((a, e) => a ++ e.collect(p))
    }

    collector(Set.empty)(this)
  }

  def vars: Set[Var] = collectE(_.isInstanceOf[Var])

}

/**
  * var + offset :-> value
  */
case class PointsTo(id: Var, offset: Int = 0, value: Expr) extends Heaplet {
  override def pp: Ident = {
    val head = if (offset <= 0) id.pp else s"(${id.pp} + $offset)"
    s"$head :-> ${value.pp}"
  }

  def subst(sigma: Map[Var, Expr]): Heaplet = {
    val e = sigma.getOrElse(id, id)
    assert(e.isInstanceOf[Var], s"Substitution into non-variable [${e.pp} / ${id.pp}] in points-to $pp")
    PointsTo(e.asInstanceOf[Var], offset, value.subst(sigma))
  }
}

/**
  * block(var, size)
  */
case class Block(id: Var, sz: Int) extends Heaplet {
  override def pp: Ident = {
    s"[${id.pp}, $sz]"
  }

  def subst(sigma: Map[Var, Expr]): Heaplet = {
    val e = sigma.getOrElse(id, id)
    assert(e.isInstanceOf[Var], s"Substitution into non-variable [${e.pp} / ${id.pp}] in points-to $pp")
    Block(e.asInstanceOf[Var], sz)
  }
}

/**
  * Predicate application
  */
case class SApp(pred: Ident, args: Seq[Expr]) extends Heaplet {
  override def pp: String = s"$pred(${args.map(_.pp).mkString(", ")})"

  def subst(sigma: Map[Var, Expr]): Heaplet = SApp(pred, args.map(_.subst(sigma)))
}


case class SFormula(chunks: List[Heaplet]) extends PrettyPrinting with Substitutable[SFormula] {
  override def pp: Ident = if (chunks.isEmpty) "emp" else chunks.map(_.pp).mkString(" ** ")

  def subst(sigma: Map[Var, Expr]): SFormula = SFormula(chunks.map(_.subst(sigma)))

  // Collect certain sub-expressions
  def collectE[R <: Expr](p: Expr => Boolean): Set[R] = {
    chunks.foldLeft(Set.empty[R])((a, h) => a ++ h.collectE(p))
  }

  def isEmp: Boolean = chunks.isEmpty

  def **(other: SFormula): SFormula = SFormula(chunks ++ other.chunks)

  def -(h: Heaplet): SFormula = SFormula(chunks.filterNot(elm => elm == h))

  def -(hs: Seq[Heaplet]): SFormula = {
    val hSet = hs.toSet
    SFormula(chunks.filterNot(elm => hSet.contains(elm)))
  }

  // TODO: implement replacement of subformula by another one
}

// TODO: extend with inductive predicates
