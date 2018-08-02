name := """codacy-flawfinder"""

version := "1.0.0-SNAPSHOT"

val languageVersion = "2.12.6"

scalaVersion := languageVersion

resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases",
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "com.codacy" %% "codacy-engine-scala-seed" % "3.0.183",
  "com.github.pathikrit" %% "better-files" % "3.5.0"
)

mainClass in Compile := Some("codacy.Engine")

enablePlugins(JavaAppPackaging)

enablePlugins(DockerPlugin)
