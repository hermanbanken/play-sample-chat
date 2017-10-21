
name := """chat"""

version := "2.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(ehcache, ws, specs2 % Test, guice)

unmanagedResourceDirectories in Test <+= baseDirectory(
  _ / "target/web/public/test")