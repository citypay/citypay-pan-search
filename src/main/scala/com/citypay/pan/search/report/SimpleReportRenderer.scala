package com.citypay.pan.search.report

import java.io.PrintStream

import com.citypay.pan.search.ScanReport
import com.typesafe.config.Config

trait ReportRenderer {
  def render(report: ScanReport): Unit
}

/**
  * Created by gary on 12/06/2017.
  */
class SimpleReportRenderer(out: PrintStream) extends ReportRenderer {


  override def render(report: ScanReport): Unit = {

    out.println("=" * 80)
    out.println(s"| Scan Report ${report.name}" )
    out.println("=" * 80)
    out.println()


    out.println(s" Matches ${report.matchCount}")
    out.println("-" * 80)

    for ((m, i) <- report.matches().zipWithIndex) {
      out.println(" Match %02d %24s %-24s %-50s (%5s to %5s, len:%d) %-10s, %20s".format(i + 1,
        m.system, m.location, m.result, m.from, m.to, m.expectedLen,  m.searchSpec, m.value))
    }

    out.flush()


  }
}
