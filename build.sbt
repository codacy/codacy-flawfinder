import com.typesafe.sbt.packager.docker.Cmd

name := "codacy-flawfinder"

scalaVersion := "2.13.8"

libraryDependencies ++= Seq(
  "com.codacy" %% "codacy-engine-scala-seed" % "5.0.1",
  "com.github.pathikrit" %% "better-files" % "3.8.0"
)

Compile / mainClass := Some("codacy.Engine")

enablePlugins(JavaAppPackaging)

enablePlugins(DockerPlugin)

Universal / mappings ++= {
  (Compile / resourceDirectory) map { resourceDir: File =>
    val src = resourceDir / "docs"
    val dest = "/docs"

    for {
      path <- src.allPaths.get if !path.isDirectory
    } yield path -> path.toString.replaceFirst(src.toString, dest)
  }
}.value

val dockerUser = "docker"
val dockerGroup = "docker"

Docker / daemonUser := dockerUser
Docker / daemonGroup := dockerGroup

dockerBaseImage := "codacy-flawfinder-base"
dockerEntrypoint := Seq(s"/opt/docker/bin/${name.value}")

dockerCommands := dockerCommands.value.flatMap {
  case cmd @ Cmd("WORKDIR", _) => Seq(Cmd("WORKDIR", "/opt/docker"))
  case cmd @ Cmd("ADD", _) =>
    Seq(Cmd("RUN", s"adduser -u 2004 -D $dockerUser"), cmd, Cmd("RUN", "mv /opt/docker/docs /docs"))
  case other => List(other)
}
