
organization := "com.github.martinpallmann"
name := "akka-persistence-riak"
version := "0.0.0"
scalaVersion := "2.11.7"

val akkaVersion = "2.4.1"
val riakVersion = "2.0.2"
val slf4jVersion = "1.7.13"

libraryDependencies ++= {
  def akka(artifactId: String) = { "com.typesafe.akka" %% s"akka-$artifactId" % akkaVersion }
  def riak(artifactId: String) = { "com.basho.riak"    %  s"riak-$artifactId" % riakVersion }
  def slf4j(artifactId: String) = { "org.slf4j"        %  s"slf4j-$artifactId" % slf4jVersion }
  def testDeps(modules: ModuleID*): Seq[ModuleID] = modules.map(_ % "test")
  Seq(
    akka("actor"),
    akka("persistence"),
    riak("client")
  ) ++ testDeps(
    akka("testkit"),
    akka("persistence-tck"),
    slf4j("nop")
  )
}
scalacOptions := Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")
