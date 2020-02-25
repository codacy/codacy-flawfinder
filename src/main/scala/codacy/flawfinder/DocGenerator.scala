package codacy.flawfinder

import play.api.libs.json.{JsArray, Json}
import better.files._
import better.files
import com.codacy.plugins.api.results.{Pattern, Result, Tool}

object DocGenerator {

  case class Ruleset(patternId: String, level: String, title: String, description: String)

  def main(args: Array[String]): Unit = {

    val version: String = getVersion
    val fileName = args(0)
    val rules = getRules(fileName)
    createPatternsAndDescriptionFile(version, rules)
  }

  private def getSubCategory(ruleset: Ruleset) = {
    // if we have more than one sub-category, only consider first
    val cwePatternRegex = """.*\(CWE-([0-9]+).*""".r

    // https://manpages.debian.org/jessie/flawfinder/flawfinder.1.en.html#COMMON_WEAKNESS_ENUMERATION_(CWE)
    ruleset.description match {
      case cwePatternRegex("20") => Pattern.Subcategory.InputValidation
      case cwePatternRegex("22") => Pattern.Subcategory.FileAccess
      case cwePatternRegex("78") => Pattern.Subcategory.InsecureModulesLibraries
      case cwePatternRegex("119") => Pattern.Subcategory.CommandInjection
      case cwePatternRegex("120") => Pattern.Subcategory.CommandInjection
      case cwePatternRegex("126") => Pattern.Subcategory.InputValidation
      case cwePatternRegex("134") => Pattern.Subcategory.CommandInjection
      case cwePatternRegex("190") => Pattern.Subcategory.UnexpectedBehaviour
      case cwePatternRegex("250") => Pattern.Subcategory.CommandInjection
      case cwePatternRegex("327") => Pattern.Subcategory.Cryptography
      case cwePatternRegex("362") => Pattern.Subcategory.DoS
      case cwePatternRegex("377") => Pattern.Subcategory.InsecureStorage
      case cwePatternRegex("676") => Pattern.Subcategory.InsecureModulesLibraries
      case cwePatternRegex("732") => Pattern.Subcategory.Visibility
      case cwePatternRegex("785") => Pattern.Subcategory.InputValidation
      case cwePatternRegex("807") => Pattern.Subcategory.UnexpectedBehaviour
      case cwePatternRegex("829") => Pattern.Subcategory.MaliciousCode
      case _ => Pattern.Subcategory.Other
    }
  }

  private def generateDescriptions(rules: Seq[Ruleset]): JsArray = {
    val codacyPatternsDescs = rules.map { rule =>
      Json.obj(
        "patternId" -> rule.patternId,
        "title" -> Json.toJsFieldJsValueWrapper(s"Finds potential security problems in ${rule.patternId} calls"),
        "description" -> Json.toJsFieldJsValueWrapper(truncateText(rule.description, 495)),
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
    File(fileName).lines
      .map(_.split('\t'))
      .map { line =>
        Ruleset(line(0), line(1), line(2), line(2))
      }
      .toSeq
  }

  private def createPatternsAndDescriptionFile(version: String, rules: Seq[DocGenerator.Ruleset]): Unit = {
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

  private def getLevel(level: String) = level match {
    case "3" => Result.Level.Warn
    case "4" | "5" => Result.Level.Err
    case _ => Result.Level.Info
  }

  private def getPatterns(version: String, rules: Seq[DocGenerator.Ruleset]): String = {
    val specifications =
      rules.map { rule =>
        Pattern.Specification(
          Pattern.Id(rule.patternId),
          getLevel(rule.level),
          Pattern.Category.Security,
          Option(getSubCategory(rule)),
          None,
          None
        )
      }.toSet

    val spec = Tool.Specification(Tool.Name("flawfinder"), Some(Tool.Version(version)), specifications)
    Json.prettyPrint(Json.toJson(spec))
  }

  private def getDescriptions(rules: Seq[DocGenerator.Ruleset]): String = {
    Json.prettyPrint(
      Json
        .parse(Json.toJson(generateDescriptions(rules)).toString)
        .as[JsArray]
    )
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
