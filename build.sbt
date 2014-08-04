lazy val playmatcher = (project in file(".")).enablePlugins(PlayScala)

name := "PlayMatcher"

version := "1.0"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",
  "org.webjars" % "handlebars" % "2.0.0-alpha.2",
  "org.webjars" % "requirejs" % "2.1.14-1",
  "org.webjars" % "jquery" % "2.1.1"
)

sourceGenerators in Assets <+= handlebars

//pipelineStages := Seq(rjs, digest)

net.virtualvoid.sbt.graph.Plugin.graphSettings

