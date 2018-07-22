package codacy.flawfinder

import com.codacy.plugins.api.results.Result.Issue
import com.codacy.plugins.api.results.{Pattern, Result, Tool}
import com.codacy.plugins.api.{Options, Source}
import com.codacy.tools.scala.seed.utils.CommandRunner

import scala.util.Try

object Flawfinder extends Tool {

  override def apply(
                      source: Source.Directory,
                      conf: Option[List[Pattern.Definition]],
                      files: Option[Set[Source.File]],
                      options: Map[Options.Key, Options.Value]
                    )(implicit specification: Tool.Specification): Try[List[Result]] = {
    Try {
      val filesToLint: List[String] = files.fold(List(source.path)) { paths =>
        paths.map(_.toString).toList
      }

      val command = List("flawfinder", "-Q", "-D", "-S", "-F") ++
        filesToLint

      CommandRunner.exec(command) match {
        case Right(resultFromTool) =>
          val output = resultFromTool.stdout ++ resultFromTool.stderr
          parseToolResult(output, checkPattern(conf))
        case Left(failure) =>
          throw failure
      }
    }
  }

  private val LineRegex = """(.+?):(\d+?):.*?\) (.+?):(.+)""".r

  private def parseToolResult(resultFromTool: List[String], wasRequested: String => Boolean): List[Result] = {
    resultFromTool.flatMap {
      case LineRegex(filename, lineNumber, patternId, message) if wasRequested(patternId) =>
        Option(Result.Issue(Source.File(filename), Result.Message(message), Pattern.Id(patternId), Source.Line(lineNumber.toInt)))
      case _ =>
        Option.empty[Result]
    }
  }

  private def checkPattern(conf: Option[List[Pattern.Definition]])(patternId: String): Boolean = {
    conf.forall(_.exists(_.patternId.value.toLowerCase == patternId.toLowerCase))
  }

}
