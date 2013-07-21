package nbot

import java.io.FileNotFoundException
import scala.io.Source

class NickRPN (tryNickCmd:(NicklistMeta)=>Option[Set[String]]) extends CommandDelegate[NicklistMeta,Set[String]]  {
  def rpnFold(accTuple:(NicklistMeta,Seq[Set[String]]), term:String) = (accTuple._1, {
    val m = accTuple._1
    val stack = accTuple._2
    // TODO: this can be done better
    val oper = (x:Set[String],y:Set[String]) => term match {
      case "+" => x.union(y)
      case "-" => x.diff(y)
      case "&" => x.intersect(y)
      case "@" => x.diff(y).union(y.diff(x))
    }

    try {
      stack match {
        case Seq(b,a, rest @ _ *) =>
          rest.+:(oper(a,b))
      }
    } catch {
      // we are checking for the match error in the oper
      // function, not the Seq matching.
      // This runs if the term is not an operation
      case _:MatchError =>
        stack.+:(if (term.equals("*")) {
          m.nicklist(m.channel).map(_._2)
        } else {
          tryNickCmd(m.copy(cmd = term)).getOrElse(Set())
        })
    }})

  def rpnCalc(m:NicklistMeta, rpn: String) : Option[Set[String]] = {
    val result = rpn.split(" ").foldLeft((m,Seq[Set[String]]()))(rpnFold)._2
    println(result)
    if (result.size == 1) Some(result(0)) else None
  }

  def tryCmd(m:NicklistMeta): Option[Set[String]] = m.cmd.split(" ",2) match {
    case Array("showgroups",rpn) =>
      rpnCalc(m, rpn)

    case Array(rpnfile,_*) =>
      try {
        val file = Source.fromFile("resources/nicklists/"+rpnfile+".set").getLines
        if (file.hasNext) {
          println("found file line")
          rpnCalc(m, file.next)
        } else {
          None
        }
      } catch {
        case _:FileNotFoundException => None
      }

    case _ =>
      None
  }
}
