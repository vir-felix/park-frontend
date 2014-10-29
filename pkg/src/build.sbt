import sbt._
import Process._
import Keys._

import AssemblyKeys._

assemblySettings

net.virtualvoid.sbt.graph.Plugin.graphSettings

name := "sparkr"

version := "0.1"

organization := "edu.berkeley.cs.amplab"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.2",
  "org.slf4j" % "slf4j-log4j12" % "1.7.2"
)

{
  val excludeCglib = ExclusionRule(organization = "org.sonatype.sisu.inject")
  val excludeJackson = ExclusionRule(organization = "org.codehaus.jackson")
  val excludeNetty = ExclusionRule(organization = "org.jboss.netty")
  val excludeAsm = ExclusionRule(organization = "asm")
  val excludeSnappy = ExclusionRule(organization = "org.xerial.snappy")
  val sbtYarnFlag = scala.util.Properties.envOrElse("SBT_YARN_FLAG", "")
  val defaultHadoopVersion = "1.0.4"
  val defaultSparkVersion = "1.1.0"
  val hadoopVersion = scala.util.Properties.envOrElse("SPARK_HADOOP_VERSION", defaultHadoopVersion)
  val sparkVersion = scala.util.Properties.envOrElse("SPARK_VERSION", defaultSparkVersion)
  libraryDependencies ++= Seq(
    "org.apache.hadoop" % "hadoop-client" % hadoopVersion excludeAll(excludeJackson, excludeNetty, excludeAsm, excludeCglib),
    "org.apache.spark" % "spark-core_2.10" % sparkVersion
  ) ++ (if (sbtYarnFlag == "yarn") {
          val defaultYarnVersion = "2.4.0"
          val yarnVersion = scala.util.Properties.envOrElse("SPARK_YARN_VERSION", defaultYarnVersion)
          Seq(
            "org.apache.hadoop" % "hadoop-yarn-api" % yarnVersion excludeAll(excludeJackson, excludeNetty, excludeAsm, excludeCglib),
            "org.apache.hadoop" % "hadoop-yarn-common" % yarnVersion excludeAll(excludeJackson, excludeNetty, excludeAsm, excludeCglib),
            "org.apache.hadoop" % "hadoop-yarn-server-web-proxy" % yarnVersion excludeAll(excludeJackson, excludeNetty, excludeAsm, excludeCglib),
            "org.apache.hadoop" % "hadoop-yarn-client" % yarnVersion excludeAll(excludeJackson, excludeNetty, excludeAsm, excludeCglib),
            "org.apache.spark" % "spark-yarn_2.10" % sparkVersion
          )
        } else {
          None.toSeq
        }
      )
}

resolvers ++= Seq(
  "Typesafe" at "http://repo.typesafe.com/typesafe/releases",
  "Scala Tools Snapshots" at "http://scala-tools.org/repo-snapshots/",
  "Cloudera Repository"  at "https://repository.cloudera.com/artifactory/cloudera-repos/",
  "Spray" at "http://repo.spray.cc"
)

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList("javax", "servlet", xs @ _*)              => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".html"      => MergeStrategy.first
    case "application.conf"                                 => MergeStrategy.concat
    case "reference.conf"                                   => MergeStrategy.concat
    case "log4j.properties"                                 => MergeStrategy.first
    case m if m.toLowerCase.endsWith("manifest.mf")         => MergeStrategy.discard
    case m if m.toLowerCase.matches("meta-inf/services.*$") => MergeStrategy.concat
    case m if m.toLowerCase.matches("meta-inf.*\\.sf$")     => MergeStrategy.discard
    case _ => MergeStrategy.first
  }
}
