package uk.ac.ncl.openlab.intake24.redux

import scala.reflect.macros.whitebox

object macros {

  implicit def deriveActionEncoder[T]: ActionEncoder[T] = macro actionEncoderImpl[T]

  implicit def deriveActionDecoder[T]: ActionDecoder[T] = macro actionDecoderImpl[T]

  def actionEncoderImpl[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._

    val rootClass = c.weakTypeOf[T].typeSymbol

    if (!rootClass.isClass || !rootClass.asClass.isSealed)
      c.abort(c.enclosingPosition, "Sealed trait or abstract class type expected")
    else {

      val imports = List(q"import scala.scalajs.js",
        q"import io.circe._",
        q"import io.circe.syntax._",
        q"import io.circe.generic.auto._",
        q"import io.circe.scalajs._")

      val subclasses = rootClass.asClass.knownDirectSubclasses.toList

      val matchClauses: List[c.Tree] = subclasses.map {
        cls =>
          /*

          This check is disabled because it triggers a probable bug in Scala/Scala.js compiler causing
          recompilation to break. 'clean' fixes the issue which is why I assume it is a bug but I have no
          idea how to debug something like this.

          This will trigger a compile error anyway if an attempt to match on a non-case class is made.

          if (!cls.asClass.isCaseClass)
          c.abort(c.enclosingPosition, "Not a case class: " + cls.asClass.fullName + ". Only case classes are supported.")
          */
          if (cls.isModuleClass)
            cq"x: $cls => (JsonObject.empty, ${cls.fullName})"
          else
            cq"x: $cls => (x.asJsonObject, ${cls.fullName})"
      }

      q"""new uk.ac.ncl.openlab.intake24.redux.ActionEncoder[$rootClass] {
          ..$imports

             def encode(action: $rootClass) = {
               val (payload, typeName) = action match {
                 case ..$matchClauses
             }

             convertJsonToJs(Json.fromJsonObject(payload.add("type", Json.fromString(typeName))))
           }
         }
       """
    }
  }

  def actionDecoderImpl[T: c.WeakTypeTag](c: whitebox.Context): c.Tree = {
    import c.universe._

    val rootClass = c.weakTypeOf[T].typeSymbol

    if (!rootClass.isClass || !rootClass.asClass.isSealed)
      c.abort(c.enclosingPosition, "Sealed trait or abstract class type expected")
    else {

      val imports = List(q"import scala.scalajs.js",
        q"import io.circe._",
        q"import io.circe.syntax._",
        q"import io.circe.generic.auto._",
        q"import io.circe.scalajs._")

      val subclasses = rootClass.asClass.knownDirectSubclasses.toList

      val parseSubclasses = subclasses.filterNot(_.isAbstract).foldLeft(q"None": c.Tree) {
        case (cont, cls) =>

          // if (!cls.asClass.isCaseClass)
          // c.abort(c.enclosingPosition, "Not a case class: " + cls.asClass.fullName + ". Only case classes are supported.")

          val classNameLiteral = cls.asClass.fullName
          q"""if (actionType == $classNameLiteral) parsePayload[$cls](action) else $cont"""
      }

      q"""new uk.ac.ncl.openlab.intake24.redux.ActionDecoder[$rootClass] {
        ..$imports;

            def decode(action: js.Dynamic): Option[$rootClass] = {
              val actionType = action.`type`.toString

              $parseSubclasses
            }
         }
      """
    }
  }
}