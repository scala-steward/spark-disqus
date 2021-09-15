import Dependencies._
import sbt.Keys.test
import sbt.Tests.{Group, SubProcess}
import sbtassembly.MergeStrategy
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.{versionFormatError, Version}

scalaVersion := "2.12.15"

Global / onChangedBuildSource := ReloadOnSourceChanges

Project.inConfig(Test)(baseAssemblySettings)

lazy val shadeRules = Seq()

lazy val mergeStrategy: PartialFunction[String, MergeStrategy] = {
  case x if x.endsWith("module-info.class")             => MergeStrategy.rename
  case x if x.endsWith("actor.conf")                    => MergeStrategy.first
  case x if x.endsWith("scala-java8-compat.properties") => MergeStrategy.rename
  case x if x.endsWith("io.netty.versions.properties")  => MergeStrategy.rename
  case x if x.endsWith("native-image.properties")       => MergeStrategy.rename
  case x if x.endsWith("reflection-config.json")        => MergeStrategy.rename
  case x if x.endsWith("UnusedStubClass.class")         => MergeStrategy.discard
}

lazy val commonSettings = Seq(
  organizationName := "SZTAKI",
  organization := "hu.sztaki.spark.squs",
  scalaVersion := "2.12.15",
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision,
  addCompilerPlugin(scalafixSemanticdb),
  scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0",
  scalacOptions ++= List(
    "-Yrangepos",
    "-Ywarn-unused-import",
    "-encoding",
    "UTF-8",
    "-target:jvm-1.8",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-language:implicitConversions",
    "-language:postfixOps"
  ),
  releaseVersionBump := sbtrelease.Version.Bump.Next,
  releaseIgnoreUntrackedFiles := true,
  releaseVersion := {
    ver =>
      Version(ver).map(_.withoutQualifier.string)
        .getOrElse(versionFormatError(ver))
  },
  releaseNextVersion := {
    ver =>
      Version(ver).map(_.bump(releaseVersionBump.value).withoutQualifier.string)
        .getOrElse(versionFormatError(ver))
  },
  releaseProcess := Seq[ReleaseStep](
    inquireVersions,
    setReleaseVersion,
    commitReleaseVersion,
    setNextVersion,
    commitNextVersion,
    pushChanges
  ),
  /**
    * Required so that on Bamboo Agent environment, the tests can run code using the
    * Scala interpreter. If disabled, you get an NPE somewhere in Scala, missing the
    * compiler mirror.
    */
  fork := true,
  fork in (IntegrationTest, test) := true,
  Test / testForkedParallel := true,
  IntegrationTest / testForkedParallel := true,
  concurrentRestrictions in Global := Seq(Tags.limitAll(14)),
  parallelExecution in Test := true,
  testGrouping in Test := (testGrouping in Test).value.flatMap {
    group =>
      group.tests.map(
        test => Group(test.name, Seq(test), SubProcess(ForkOptions()))
      )
  },
  concurrentRestrictions := Seq(Tags.limit(Tags.ForkedTestGroup, 14)),
  logLevel in test := Level.Debug,
  test in assembly := {},
  test in assemblyPackageDependency := {},
  /**
    * Do not pack sources in compile tasks.
    */
  sources in (Compile, doc) := Seq.empty,
  /**
    * Disabling Scala and Java documentation in publishing tasks.
    */
  publishArtifact in (Compile, packageDoc) := false,
  publishArtifact in (Test, packageDoc) := false,
  publishArtifact in (Test, packageBin) := true,
  publishArtifact in (Test, packageSrc) := true,
  assemblyShadeRules in assembly := Seq(),
  /**
    * The partial function's fallback match can not be applied outside ot settings key, due
    * to accessing `value`. If you place this fallback to the `mergeStrategyBaby`, it will
    * throw an error.
    */
  assemblyMergeStrategy in assembly := mergeStrategy.orElse {
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials.sztaki.dev"),
  publishTo := Some(
    "Artifactory Realm".at("https://artifactory.sztaki.dev/artifactory/sbt-release/")
  ),
  resolvers ++= Seq(
    "Maven Central".at("https://repo1.maven.org/maven2/")
  )
)

lazy val core = (project in file("core")).settings(commonSettings: _*).settings(
  name := "core",
  description :=
    "Core, implementation and technology-dependent project used by all other " +
      "components of this project.",
  libraryDependencies ++= coreDependencies
)

lazy val `sztaki-spark-disqus` =
  (project in file(".")).settings(commonSettings: _*).aggregate(core).dependsOn(
    core % "test->test;compile->compile"
  )
