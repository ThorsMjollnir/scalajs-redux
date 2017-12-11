package uk.ac.ncl.openlab.intake24.redux

import io.circe.Encoder

import scala.language.experimental.macros
import scala.reflect.macros.{blackbox, whitebox}
import scala.scalajs.js

object Macros {
  def actionFromJs[T](action: js.Dynamic): Option[T] = macro actionFromJsImpl[T]

  def actionToJs[T](action: T): js.Any = macro actionToJsImpl[T]

  def actionToJsImpl[T: c.WeakTypeTag](c: whitebox.Context)(action: c.Tree): c.Tree = {
    import c.universe._

    val symbol = c.weakTypeOf[T].typeSymbol

    if (!symbol.isClass || !symbol.asClass.isSealed)
      c.abort(c.enclosingPosition, "Sealed trait or abstract class type expected")
    else {

      val circeImports = List(q"import io.circe._", q"import io.circe.syntax._", q"import io.circe.generic.auto._", q"import io.circe.scalajs.convertJsonToJs")

      val subclasses = symbol.asClass.knownDirectSubclasses.toList

      val matchClauses: List[c.Tree] = subclasses.map {
        cls =>
          if (!cls.asClass.isCaseClass)
            c.abort(c.enclosingPosition, "Not a case class: " + cls.asClass.fullName + ". Only case classes are supported.")
          else if (cls.isModuleClass)
            cq"x: $cls => (JsonObject.empty, ${cls.fullName})"
          else
            cq"x: $cls => (x.asJsonObject, ${cls.fullName})"
      }

      q"""
          ..$circeImports

         val (payload, typeName) = $action match {
            case ..$matchClauses
         }

         convertJsonToJs(Json.fromJsonObject(payload.add("type", Json.fromString(typeName))))
       """
    }
  }

  def actionFromJsImpl[T: c.WeakTypeTag](c: blackbox.Context)(action: c.Tree): c.Tree = {
    import c.universe._

    val symbol = c.weakTypeOf[T].typeSymbol

    if (!symbol.isClass || !symbol.asClass.isSealed)
      c.abort(c.enclosingPosition, "Sealed trait or abstract class type expected")
    else {

      val circeImports = List(q"import io.circe._", q"import io.circe.syntax._", q"import io.circe.generic.auto._", q"import io.circe.scalajs.convertJsToJson")

      val subclasses = symbol.asClass.knownDirectSubclasses.toList


      val parsePayload =
        q"""def parsePayload[T](action: js.Dynamic)(implicit decoder: Decoder[T]): Option[T] =
                       (for (json <- convertJsToJson(action);
                              parsed <- json.as[T])
                          yield parsed) match {
                          case Left(err) =>
                            js.Dynamic.global.console.error("Could not parse Redux action: " + err.getMessage)
                            None
                          case Right(res) =>
                            Some(res)
                        }"""

      val actionType = q"""val actionType = $action.`type`.toString"""

      val parseSubclasses = subclasses.filterNot(_.isAbstract).foldLeft(q"None": c.Tree) {
        case (cont, cls) =>

          if (!cls.asClass.isCaseClass)
            c.abort(c.enclosingPosition, "Not a case class: " + cls.asClass.fullName + ". Only case classes are supported.")

          val classNameLiteral = cls.asClass.fullName
          q"""if (actionType == $classNameLiteral) parsePayload[$cls]($action) else $cont"""
      }

      q"..$circeImports;$parsePayload;$actionType;$parseSubclasses"
    }
  }
}