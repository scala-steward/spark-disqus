import sbt._

object Dependencies {
  val currentScalaVersion = "2.13.8"

  val scalaLanguage = "org.scala-lang" % "scala-library" % currentScalaVersion
  val scalaCompiler = "org.scala-lang" % "scala-compiler" % currentScalaVersion
  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.6" % "test"

  val coreDependencies = Seq(
    "org.scala-lang" % "scala-library" % currentScalaVersion,
    "org.scala-lang" % "scala-compiler" % currentScalaVersion,
    "org.scalatest" %% "scalatest" % "3.3.0-SNAP3" % "test",
    "com.typesafe" % "config" % "1.4.1",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
    "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.17.1",
    "org.slf4j" % "jcl-over-slf4j" % "1.7.33",
    "com.typesafe.akka" %% "akka-actor" % "2.6.18",
    "com.typesafe.akka" %% "akka-stream" % "2.6.18",
    "com.typesafe.akka" %% "akka-http" % "10.2.7",
    ("org.apache.hadoop" % "hadoop-common" % "3.3.1")
      .excludeAll(
        ExclusionRule("commons-logging"),
        ExclusionRule("com.sun.jersey"),
        ExclusionRule("javax.activation"),
        ExclusionRule("javax.servlet"),
        ExclusionRule("io.netty"),
        ExclusionRule("org.apache.curator"),
        ExclusionRule("log4j", "log4j"),
        ExclusionRule("org.slf4j", "slf4j-log4j12")
      ),
    ("org.apache.hadoop" % "hadoop-mapreduce-client-core" % "3.3.1")
      .exclude("aopalliance", "aopalliance")
      .exclude("javax.inject", "javax.inject")
      .exclude("org.apache.hadoop", "hadoop-yarn-common")
      .exclude("org.slf4j", "slf4j-log4j12")
      .exclude("log4j", "log4j")
      .exclude("com.squareup.okio", "okio")
      .exclude("org.apache.hadoop", "hadoop-yarn-client")
      .excludeAll(
        ExclusionRule("io.netty")
      ),
    ("org.apache.spark" %% "spark-streaming" % "3.2.0")
      .excludeAll(
        ExclusionRule("org.apache.hadoop"),
        ExclusionRule("jakarta.xml.bind"),
        ExclusionRule("com.sun.jersey"),
        ExclusionRule("io.netty", "netty-common"),
        ExclusionRule("io.netty", "netty-resolver"),
        ExclusionRule("io.netty", "netty-handler"),
        ExclusionRule("io.netty", "netty-codec"),
        ExclusionRule("io.netty", "netty-transport"),
        ExclusionRule("io.netty", "netty-buffer"),
        ExclusionRule("io.netty", "netty-transport-native-unix-common"),
        ExclusionRule("io.netty", "netty-transport-native-epoll"),
        ExclusionRule("javax.activation"),
        ExclusionRule("org.slf4j", "slf4j-log4j12")
      ),
    "com.sksamuel.elastic4s" % "elastic4s-core_2.12" % "7.16.3",
    ("com.sksamuel.elastic4s" % "elastic4s-client-esjava_2.12" % "7.16.3")
      .excludeAll(
        ExclusionRule("commons-logging")
      ),
    "com.softwaremill.retry" %% "retry" % "0.3.3"
  )

}
