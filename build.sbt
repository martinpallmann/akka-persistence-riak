
organization := "com.github.martinpallmann"
name := "akka-persistence-riak"
version := "0.0.0"
scalaVersion := "2.11.7"
crossScalaVersions := Seq("2.10.4", "2.11.7")

val akkaVersion = "2.4.1"
val riakVersion = "2.0.2"
val slf4jVersion = "1.7.13"

libraryDependencies ++= {
  def akka(artifactId: String) = { "com.typesafe.akka" %% s"akka-$artifactId" % akkaVersion }
  def riak(artifactId: String) = { "com.basho.riak"    %  s"riak-$artifactId" % riakVersion }
  def test(moduleId: ModuleID) = { moduleId % "test" }
  Seq(
    akka("actor"),
    akka("persistence"),
    riak("client")
  ) ++ (Seq(
    akka("testkit"),
    akka("persistence-tck"),
    "org.slf4j" % "slf4j-nop" % slf4jVersion
  ) map test)
}
scalacOptions := Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")
