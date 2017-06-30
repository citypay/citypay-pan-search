package com.citypay.pan.search.report

import java.io.PrintStream

import com.citypay.pan.search.{PanSpec, ScanReport}
import com.citypay.pan.search.util.Location
import com.typesafe.config.Config
import org.json4s.DefaultFormats
import org.json4s.native.Serialization

case class JsonReport(name: String, stats: Map[String, Int], meta: Map[String, String], matches: Any, subReports: List[JsonReport])

/**
  * Created by gary on 16/06/2017.
  */
class JsonRenderer(ps: PrintStream, config: Config) extends ReportRenderer {

  import com.citypay.pan.search.util.ConfigExt._

  implicit private val formats = DefaultFormats

  private val prettyPrint = config.boolean("prettify", default = false)

  override def render(report: ScanReport): Unit = {

    val rpt = _render(report, 1)

    val str = Serialization.writePretty(rpt)

    println(str)
    ps.write(str.getBytes("UTF-8"))
    ps.write("\n\n".getBytes("UTF-8"))
    ps.flush()

  }

  private def _render(report: ScanReport, level: Int): JsonReport = {
    JsonReport(
      report.name, stats(report), report.meta, matches(report), report.subReports.map(rpt => _render(rpt, level + 1))
    )
  }

  private def stats(report: ScanReport) = {
    val m = report.statNames.map(n => (n, report.getStat(n)))
    m.toMap
  }

  private def matches(report: ScanReport) = {
    report.matches().map(m => {

      Map(
        "system" -> m.system,
        "location" -> m.location,
        "result" -> m.result,
        "from" -> loc(m.from),
        "to" -> loc(m.to),
        "offset" -> m.offset,
        "maskedValue" -> m.maskedValue,
        "value" -> m.value,
        "spec" -> spec(m.searchSpec)
      )


    })
  }

  private def obj(indent: Int, args: String*): String = args.mkString(s"${ind(indent)}{", ",", s"$newLine${ind(indent)}}")

  private def spec(spec: PanSpec) = {
    Map(
      "id" -> spec.id,
      "leading" -> spec.leading,
      "name" -> spec.name,
      "logo" -> spec.logo
    )
  }

  private def loc(location: Location) = {
    Map(
      "colNo" -> location.colNo,
      "lineNo" -> location.lineNo
    )
  }

  private def ind(i: Int) = if (prettyPrint) "   " * i else ""

  private def newLine = if (prettyPrint) "\n" else ""

  def tlv(key: String, value: Any, indent: Int = 1, prerendered: Boolean = false): String = {

    def jsonValue(a: Any): String = a match {
      case i: Byte => i.toString
      case i: Int => i.toString
      case i: Long => i.toString
      case i: Double => i.toString
      case i: Float => i.toString
      case i: Boolean => i.toString
      case arr: Array[_] => arr.map(jsonValue).mkString("[", ",", "]")
      case list: List[_] => list.map(jsonValue).mkString("[", ",", "]")
      case m: Map[_, _] => m.map(t => tlv(String.valueOf(t._1), t._2, indent + 1)).mkString("{", ",", s"$newLine${ind(indent)}}")
      case s: String if prerendered => s
      case s: String => s""""$s""""
      case _ => String.valueOf(value)
    }

    val j = jsonValue(value)
    if (j.length > 0 && j != "[]")
      s"""$newLine${ind(indent)}"$key":$j"""
    else
      ""
  }

}
