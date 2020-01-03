name := "typeracer-server"

version := "0.1"

scalaVersion := "2.13.1"

enablePlugins(AkkaGrpcPlugin)

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.26" % Test,
  "org.scalatest" %% "scalatest" % "3.1.0" % Test
)
