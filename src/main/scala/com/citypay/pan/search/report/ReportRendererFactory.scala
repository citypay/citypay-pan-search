package com.citypay.pan.search.report

import java.io.PrintStream

import com.typesafe.config.Config

/**
  * Created by gary on 22/06/2017.
  */
object ReportRendererFactory {

  import com.citypay.pan.search.util.ConfigExt._

  def apply(ps: PrintStream, config: Config): ReportRenderer = {

    config.string("renderer", "json").toLowerCase match {
      case "json" => new JsonRenderer(ps, config)
      case "simple" => new SimpleReportRenderer(ps)
      case s: String => throw new UnsupportedOperationException(s"Renderer: '$s' not supported")
    }




  }

}
