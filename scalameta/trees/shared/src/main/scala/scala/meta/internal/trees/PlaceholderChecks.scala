package scala.meta.internal.trees

import scala.annotation.tailrec
import scala.collection.mutable
import scala.meta.{Term, Tree}

object PlaceholderChecks {

  def isPlaceholder(tree: Tree): Boolean = tree match {
    case _: Term.Placeholder => true
    case Term.Ascribe(_: Term.Placeholder, _) => true
    case Term.Repeated(_: Term.Placeholder) => true
    case _ => false
  }

  def isBlockPlaceholder(args: List[Tree]): Boolean = args match {
    case List(Term.Block(List(arg))) => isPlaceholder(arg)
    case _ => false
  }

  def hasPlaceholder(tree: Tree, includeArg: => Boolean): Boolean = {
    val queue = new mutable.Queue[Tree]
    @tailrec
    def iter: Boolean =
      queue.dequeue() match {
        case t if isPlaceholder(t) => t.ne(tree) || includeArg
        case t: Term.Select => queue += t.qual; iter
        case t: Term.Tuple => t.args.exists(isPlaceholder)
        case t: Term.Apply =>
          isBlockPlaceholder(t.args) || t.args.exists(isPlaceholder) || { queue += t.fun; iter }
        case t: Term.ApplyInfix =>
          isBlockPlaceholder(t.args) || { queue += t.lhs; queue ++= t.args; iter }
        case t: Term.ApplyUnary => queue += t.arg; iter
        case t: Term.ApplyType => queue += t.fun; iter
        case t: Term.New => queue += t.init; iter
        case t: Term.Repeated => queue += t.expr; iter
        case t: Term.AnonymousFunction => t.ne(tree) && queue.nonEmpty && iter
        case t => t.children.exists(isPlaceholder) || queue.nonEmpty && iter
      }
    queue += tree
    iter
  }

}
