enablePlugins(ScalaJSPlugin)

name := "scalajs-redux"

organization := "uk.ac.ncl.openlab.intake24"

description := "Scala.js Redux interface"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.4"

scalacOptions ++= Seq("-P:scalajs:sjsDefinedByDefault", "-language:experimental.macros")

libraryDependencies ++= Seq(
  "io.circe" %%% "circe-core" % "0.8.0",
  "io.circe" %%% "circe-generic" % "0.8.0",
  "io.circe" %%% "circe-parser" % "0.8.0",
  "org.scala-lang" % "scala-reflect" % scalaVersion.value)

scalaJSLinkerConfig ~= {
  _.withModuleKind(ModuleKind.CommonJSModule)
}
