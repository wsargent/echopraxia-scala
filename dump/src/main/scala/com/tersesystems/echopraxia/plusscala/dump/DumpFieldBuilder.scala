package com.tersesystems.echopraxia.plusscala.dump

import com.tersesystems.echopraxia.api.Field
import com.tersesystems.echopraxia.plusscala.api.FieldBuilder

import scala.annotation.tailrec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

trait DumpFieldBuilder extends FieldBuilder {
  import DumpFieldBuilder.impl
  
  def dumpKeyValue[A](expr: A): Field = macro impl.keyValue[A]

}

object DumpFieldBuilder extends DumpFieldBuilder {
  
  private class impl(val c: blackbox.Context) {
    import c.universe._

    def keyValue[A: c.WeakTypeTag](expr: c.Tree): c.Tree = {
      val tpeA: Type = implicitly[WeakTypeTag[A]].tpe
      
      handle(tpeA, expr)
    }

    private def handle(tpeA: Type, expr: c.Tree) = {
      // taken from https://github.com/dwickern/scala-nameof
      @tailrec def extract(tree: c.Tree): String = tree match {
        case Ident(n) => n.decodedName.toString
        case Select(_, n) => n.decodedName.toString
        case Function(_, body) => extract(body)
        case Block(_, expr) => extract(expr)
        case Apply(func, _) => extract(func)
        case TypeApply(func, _) => extract(func)
        case _ =>
          c.abort(c.enclosingPosition, s"Unsupported expression: ${expr}")
      }

      val name = expr match {
        case Literal(Constant(_)) => c.abort(c.enclosingPosition, "Cannot provide name to static constant!")
        case _ => extract(expr)
      }
            
      q"""(${c.prefix}.keyValue($name, fb.ToValue[$tpeA]($expr)))"""
    }
  }

}
