name := "typeracer-server"

version := "0.1"

scalaVersion := "2.13.1"

enablePlugins(AkkaGrpcPlugin)

libraryDependencies += "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
