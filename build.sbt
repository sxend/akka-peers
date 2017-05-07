
organization := "arimitsu.sf"

name := "akka-peers"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.2"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  Resolver.bintrayRepo("sxend", "releases"),
  Resolver.bintrayRepo("sxend", "snapshots"),
  Resolver.bintrayRepo("akka", "maven"),
  "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
)

libraryDependencies ++= {
  val akkaVersion = "2.5.1"
  val spec2Version = "3.8.9"
  val log4jVersion = "2.7"
  Seq(
    "com.typesafe.akka" %% "akka-remote" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion % "test",
    "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion % "test",
    "org.apache.logging.log4j" % "log4j-core" % log4jVersion % "test",
    "org.apache.logging.log4j" % "log4j-api" % log4jVersion % "test",
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion % "test",
    "org.specs2" %% "specs2-html" % spec2Version % "test",
    "org.specs2" %% "specs2-junit" % spec2Version % "test",
    "org.specs2" %% "specs2-core" % spec2Version % "test"
  )
}

publishMavenStyle := false

bintrayRepository := {
  if (version.value.matches("^[0-9]+\\.[0-9]+\\.[0-9]+$")) "releases" else "snapshots"
}

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

javacOptions ++= Seq("-source", "1.8")

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:reflectiveCalls",
  "-language:postfixOps"
)

testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "junitxml", "html", "console")

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value / "protobuf"
)

libraryDependencies += "com.trueaccord.scalapb" %% "scalapb-runtime" % com.trueaccord.scalapb.compiler.Version.scalapbVersion % "protobuf"