package com.tersesystems.echopraxia.plusscala.api

import com.tersesystems.echopraxia.api.{Attributes, Field, Value}
import com.tersesystems.echopraxia.plusscala.api.LoggingBase._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.format.{DateTimeFormatter, FormatStyle}
import java.time.{Instant, LocalDateTime, ZoneOffset}

// The tests here compile in 2.13 but do not compile in 2.12
class OptionSpec extends AnyWordSpec with Matchers with LoggingBase {
  implicit val instantToValue: ToValue[Instant] = instant => ToValue(instant.toString)

  implicit def optionValueFormat[TV: ToValueAttribute]: ToValueAttribute[Option[TV]] = new ToValueAttribute[Option[TV]] {
    override def toValue(v: Option[TV]): Value[_] = v match {
      case Some(tv) =>
        val ev = implicitly[ToValueAttribute[TV]]
        ev.toValue(tv)
      case None => Value.nullValue()
    }

    override def toAttributes(value: Value[_]): Attributes = implicitly[ToValueAttribute[TV]].toAttributes(value)
  }

  // Show a human readable toString
  trait ToStringFormat[T] extends ToValueAttribute[T] {
    override def toAttributes(value: Value[_]): Attributes = withAttributes(withStringFormat(value))
  }

  "option" should {

    "work with primitives" in {
      val field: Field = "test" -> Option(1)
      field.toString must be("test=1")
    }

    "work with Some" in {
      val field: Field = "test" -> Some(1)
      field.toString must be("test=1")
    }

    "work with None" in {
      // XXX works in 2.13, does not work in 2.12
      // val option: Option[Nothing] = None
      // val option: None.type = None

      // Using a straight Option[Int] with None works in 2.12
      val option: Option[Int] = None
      val field: Field        = ("test" -> option)
      field.toString must be("test=null")
    }

    "work with objects" in {
      val field: Field = "test" -> Option(Instant.ofEpochMilli(0))
      field.toString must be("test=1970-01-01T00:00:00Z")
    }

    "work with custom attributes" in {
      implicit val readableInstant: ToStringFormat[Instant] = (v: Instant) => {
        val datetime  = LocalDateTime.ofInstant(v, ZoneOffset.UTC)
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        ToValue(formatter.format(datetime))
      }
      val field: Field = "test" -> Option(Instant.ofEpochMilli(0))
      field.toString must be("test=1/1/70, 12:00 AM")
    }
  }
}
