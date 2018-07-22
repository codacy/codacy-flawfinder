package codacy.flawfinder

import play.api.libs.json.{JsArray, Json}
import better.files._
import better.files

object DocGenerator {

  case class Ruleset(patternId: String,
                     level: String,
                     title: String,
                     description: String)

  def main(args: Array[String]): Unit = {

    val version: String = getVersion
    val fileName = args(0)
    val rules = getRules(fileName)
    createPatternsAndDescriptionFile(version, rules)
  }

  private def generatePatterns(rules: Seq[Ruleset]): JsArray = {
    val codacyPatterns = rules.map { rule =>
      val category: String = "Security"

      val level: String =
        rule.level match {
          case "1" => "Warning"
          case _ => "Info"
        }

      Json.obj(
        "patternId" -> rule.patternId,
        "level" -> level,
        "category" -> category
      )

    }
    Json.parse(Json.toJson(codacyPatterns).toString).as[JsArray]
  }

  private def generateDescriptions(rules: Seq[Ruleset]): JsArray = {
    val codacyPatternsDescs = rules.map { rule =>
      Json.obj(
        "patternId" -> rule.patternId,
        "title" -> Json.toJsFieldJsValueWrapper(s"Finds potential security problems in ${rule.patternId} calls"),
        "description" -> Json.toJsFieldJsValueWrapper(
          truncateText(rule.description, 495)),
        "timeToFix" -> 5
      )
    }

    Json.parse(Json.toJson(codacyPatternsDescs).toString).as[JsArray]
  }

  private def getVersion: String = {
    val repoRoot: files.File = File(".flawfinder-version")
    repoRoot.lines.mkString("")
  }

  private def getRules(fileName: String): Seq[Ruleset] = {
    File(fileName).lines.map(_.split('\t')).map { line =>
      Ruleset(line(0), line(1), line(2), line(2))
    }.toSeq
  }

  private def createPatternsAndDescriptionFile(
                                                version: String,
                                                rules: Seq[DocGenerator.Ruleset]): Unit = {
    val repoRoot: files.File = File(".")
    val docsRoot: files.File = File(repoRoot, "src/main/resources/docs")
    val patternsFile: files.File = File(docsRoot, "patterns.json")
    val descriptionsRoot: files.File = File(docsRoot, "description")
    val descriptionsFile: files.File =
      File(descriptionsRoot, "description.json")

    val patterns: String = getPatterns(version, rules)
    val descriptions: String = getDescriptions(rules)

    patternsFile.write(patterns)
    descriptionsFile.write(descriptions)
  }

  private def getPatterns(version: String,
                          rules: Seq[DocGenerator.Ruleset]): String = {
    Json.prettyPrint(
      Json.obj("name" -> "flawfinder",
        "version" -> version,
        "patterns" -> Json
          .parse(Json.toJson(generatePatterns(rules)).toString)
          .as[JsArray]))
  }

  private def getDescriptions(rules: Seq[DocGenerator.Ruleset]): String = {
    Json.prettyPrint(
      Json
        .parse(Json.toJson(generateDescriptions(rules)).toString)
        .as[JsArray])
  }

  private def truncateText(description: String, maxCharacters: Int): String = {
    if (description.length > maxCharacters) {
      description
        .take(maxCharacters)
        .split("\\.")
        .dropRight(1)
        .mkString(".") + "."
    } else {
      description
    }
  }
}
