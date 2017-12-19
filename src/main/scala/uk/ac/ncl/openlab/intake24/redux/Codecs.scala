package uk.ac.ncl.openlab.intake24.redux

import io.circe.Decoder

import scala.scalajs.js
import io.circe.scalajs._

trait ActionEncoder[T] {
  def encode(action: T): js.Any
}

abstract class ActionDecoder[T] {

  protected def parsePayload[T](action: js.Dynamic)(implicit decoder: Decoder[T]): Option[T] =
    decodeJs[T](action) match {
      case Left(err) =>
        js.Dynamic.global.console.error("Could not parse Redux action: " + err.getMessage)
        None
      case Right(res) =>
        Some(res)
    }

  def decode(action: js.Dynamic): Option[T]
}
