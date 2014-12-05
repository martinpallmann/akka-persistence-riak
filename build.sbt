import scalariform.formatter.preferences._

organization := "com.github.martinpallmann"

name := "akka-persistence-riak"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.4"

crossScalaVersions := Seq("2.10.4", "2.11.4")

libraryDependencies ++= {
  def akka(artifactId: String) = { "com.typesafe.akka" %% artifactId % "2.3.7" }
  def riak(artifactId: String) = { "com.basho.riak"    %  artifactId % "2.0.0" }
  def test(moduleId: ModuleID) = { moduleId % "test" }
  (Seq("akka-actor", "akka-persistence-experimental") map akka) ++
  (Seq("riak-client") map riak) ++
  (Seq("akka-testkit", "akka-persistence-tck-experimental") map akka map test) ++
  (Seq("org.slf4j" % "slf4j-nop" % "1.7.7") map test)
}

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

publishMavenStyle := true

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PreserveDanglingCloseParenthesis, true)

