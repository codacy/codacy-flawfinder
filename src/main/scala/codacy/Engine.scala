package codacy

import codacy.flawfinder.Flawfinder
import com.codacy.tools.scala.seed.DockerEngine

object Engine extends DockerEngine(Flawfinder)()
