package com.tersesystems.echopraxia.plusscala.api

import com.tersesystems.echopraxia.spi.FieldConstants

import scala.annotation.implicitNotFound

/**
 * Add this trait to get access to the ToName type class.
 */
trait NameTypeClass {
  // this needs to be a dependent type because implicit type resolution only works on a
  // field builder if it's dependent to the type itself.

  @implicitNotFound("Could not find an implicit ToName[${T}]")
  trait ToName[-T] {
    def toName(t: Option[T]): String
  }

  object ToName {
    implicit def throwableToName[T <: Throwable]: ToName[T] = _ => FieldConstants.EXCEPTION

    implicit val sourceCodeToName: ToName[SourceCode] = _ => SourceCode.SourceCode

    implicit def optNameToName[T: ToName]: ToName[Option[T]] = (t: Option[Option[T]]) => implicitly[ToName[T]].toName(t.flatten)

    def apply[T: ToName](t: T): String = implicitly[ToName[T]].toName(Option(t))
  }
}

trait StringToNameImplicits extends NameTypeClass {
  implicit val stringToName: ToName[String] = _.orNull
}
