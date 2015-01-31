import scalariform.formatter.preferences._

organization := "com.github.martinpallmann"
name := "akka-persistence-riak"
version := "0.0.0"
scalaVersion := "2.11.5"
crossScalaVersions := Seq("2.10.4", "2.11.5")
val akkaVersion = "2.3.9"
val riakVersion = "2.0.0"
val slf4jVersion = "1.7.7"

libraryDependencies ++= {
  def akka(artifactId: String) = { "com.typesafe.akka" %% s"akka-$artifactId" % akkaVersion }
  def riak(artifactId: String) = { "com.basho.riak"    %  s"riak-$artifactId" % riakVersion }
  def test(moduleId: ModuleID) = { moduleId % "test" }
  Seq(
    akka("actor"),
    akka("persistence-experimental"),
    riak("client")
  ) ++ (Seq(
    akka("testkit"),
    akka("persistence-tck-experimental"),
    "org.slf4j" % "slf4j-nop" % slf4jVersion
  ) map test)
}
scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")
publishMavenStyle := true
scalariformSettings
ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PreserveDanglingCloseParenthesis, true)

