package com.tersesystems.echopraxia.plusscala.trace

import com.tersesystems.echopraxia.api.{FieldBuilderResult, Level => JLevel}
import com.tersesystems.echopraxia.spi.CoreLogger
import com.tersesystems.echopraxia.plusscala.api.Condition
import com.tersesystems.echopraxia.plusscala.spi.DefaultMethodsSupport
import sourcecode._

import java.util.function.Function
import scala.compat.java8.FunctionConverters._
import scala.util.{Failure, Success, Try}

trait DefaultTraceLoggerMethods[FB <: TraceFieldBuilder] extends DefaultMethodsSupport[FB] with TraceLoggerMethods[FB] {

  def trace[B: ToValue](attempt: => B)(implicit line: Line, file: File, enc: Enclosing, args: Args): B = {
    handle(JLevel.TRACE, attempt)
  }

  def trace[B: ToValue](condition: Condition)(attempt: => B)(implicit line: Line, file: File, enc: Enclosing, args: Args): B = {
    handleCondition(JLevel.TRACE, condition, attempt)
  }

  def debug[B: ToValue](attempt: => B)(implicit line: Line, file: File, enc: Enclosing, args: Args): B = {
    handle(JLevel.DEBUG, attempt)
  }

  def debug[B: ToValue](condition: Condition)(attempt: => B)(implicit line: Line, file: File, enc: Enclosing, args: Args): B = {
    handleCondition(JLevel.DEBUG, condition, attempt)
  }

  def info[B: ToValue](attempt: => B)(implicit line: Line, file: File, enc: Enclosing, args: Args): B = {
    handle(JLevel.INFO, attempt)
  }

  def info[B: ToValue](condition: Condition)(attempt: => B)(implicit line: Line, file: File, enc: Enclosing, args: Args): B = {
    handleCondition(JLevel.INFO, condition, attempt)
  }

  def warn[B: ToValue](attempt: => B)(implicit line: Line, file: File, enc: Enclosing, args: Args): B = {
    handle(JLevel.WARN, attempt)
  }

  def warn[B: ToValue](condition: Condition)(attempt: => B)(implicit line: Line, file: File, enc: Enclosing, args: Args): B = {
    handleCondition(JLevel.WARN, condition, attempt)
  }

  def error[B: ToValue](attempt: => B)(implicit line: Line, file: File, enc: Enclosing, args: Args): B = {
    handle(JLevel.ERROR, attempt)
  }

  def error[B: ToValue](condition: Condition)(
      attempt: => B
  )(implicit line: Line, file: File, enc: Enclosing, args: Args): B = {
    handleCondition(JLevel.ERROR, condition, attempt)
  }

  @inline
  private def entering(sourceFields: SourceFields): Function[FB, FieldBuilderResult] = { fb: FB =>
    fb.entering(sourceFields)
  }.asJava

  @inline
  private def exiting[B: ToValue](sourceFields: SourceFields, ret: B): Function[FB, FieldBuilderResult] = { fb: FB =>
    fb.exiting(sourceFields, implicitly[ToValue[B]].toValue(ret))
  }.asJava

  @inline
  private def throwing(sourceFields: SourceFields, ex: Throwable): Function[FB, FieldBuilderResult] = { fb: FB =>
    fb.throwing(sourceFields, ex)
  }.asJava

  @inline
  private def handle[B: ToValue](
      level: JLevel,
      attempt: => B
  )(implicit line: Line, file: File, enc: Enclosing, args: Args): B = {
    val sourceFields = fb.sourceFields
    val extraFields  = (() => fb.list(sourceFields.loggerFields).fields()).asJava
    if (core.isEnabled(level, extraFields)) {
      execute(core, level, sourceFields, attempt)
    } else {
      attempt
    }
  }

  @inline
  private def handleCondition[B: ToValue](
      level: JLevel,
      condition: Condition,
      attempt: => B
  )(implicit line: Line, file: File, enc: Enclosing, args: Args): B = {
    val sourceFields = fb.sourceFields
    val extraFields  = (() => fb.list(sourceFields.loggerFields).fields()).asJava
    if (core.isEnabled(level, condition.asJava, extraFields)) {
      execute(core, level, sourceFields, attempt)
    } else {
      attempt
    }
  }

  @inline
  private def execute[B: ToValue](core: CoreLogger, level: JLevel, sourceFields: SourceFields, attempt: => B): B = {
    val handle = core.logHandle(level, fb)
    handle.log(fb.enteringTemplate, entering(sourceFields))
    val result = Try(attempt)
    result match {
      case Success(ret) =>
        handle.log(fb.exitingTemplate, exiting(sourceFields, ret))
      case Failure(ex) =>
        handle.log(fb.throwingTemplate, throwing(sourceFields, ex))
    }
    result.get // rethrow the exception
  }

}
