lazy val playmatcher = (project in file(".")).enablePlugins(PlayScala)

name := "PlayMatcher"

version := "1.0"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",
  "org.webjars" % "requirejs" % "2.1.14-1",
  "org.webjars" % "jquery" % "2.1.1",
  "org.webjars" % "angularjs" % "1.3.0",
  "com.typesafe.play" %% "play-slick" % "0.8.0-RC3",
  ws,
  "org.virtuslab" %% "unicorn-play" % "0.6.0",
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc4",
  "xalan" % "serializer" % "2.7.2" // No javadoc or sources available for 2.7.1
)

//pipelineStages := Seq(rjs, digest)

net.virtualvoid.sbt.graph.Plugin.graphSettings

