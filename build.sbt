name := """nbot"""

version := "1.0"

scalaVersion := "2.10.1"

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2-M3",
  "com.typesafe.akka" %% "akka-agent" % "2.2-M3",
  "com.typesafe.akka" %% "akka-testkit" % "2.2-M3",
  "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
  "commons-lang" % "commons-lang" % "2.6"
)
  //"junit" % "junit" % "4.11" % "test",
  //"com.novocode" % "junit-interface" % "0.7" % "test->default"

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")


// Note: These settings are defaults for activator, and reorganize your source directories.
Seq(
  scalaSource in Compile <<= baseDirectory / "src",
  sourceDirectory in Compile <<= baseDirectory / "src",
  scalaSource in Test <<= baseDirectory / "test",
  sourceDirectory in Test <<= baseDirectory / "test",
  resourceDirectory in Compile <<= baseDirectory / "src/main/resources"
)
