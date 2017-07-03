// MIT License
//
// Copyright (c) 2017 CityPay
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package com.citypay.pan.search.db

import java.sql.{Connection, ResultSet, Statement, Types}

import com.citypay.pan.search._
import com.citypay.pan.search.source.{ScanListener, ScanSource}
import com.citypay.pan.search.util.{Loggable, Timeable, Tracer}
import com.jolbox.bonecp.BoneCP

import scala.annotation.tailrec
import scala.util.Try
import scala.util.control.NonFatal

object ColReviewer {
  val MaxFieldSize = 16384
}

/**
  * @param ds       a data source which enables connection to the database
  * @param tableDef the table definition to run
  * @param sec      a scan context to execute against
  */
class ColReviewer(ds: BoneCP,
                  tableDef: TableDef,
                  val source: ScanSource,
                  val scanListener: ScanListener,
                  val sec: ScanExecutionContext) extends ScanExecutor with ScanReviewer with Loggable with Timeable {

  import ColReviewer._

  override def system: String = tableDef.toString

  override val report = new ScanReport(s"$tableDef scan")

  private val _tracer = new Tracer("db")

  private val fetchSize = 500

  import _tracer._

  private var conn: Connection = _
  private var st: Statement = _
  private var rs: ResultSet = _

  def sqlEscape(str: String): String = s"`$str`"

  def sql(): String =
    s"""
       |SELECT ${tableDef.lookupColNames.map(sqlEscape).mkString(",")}
       |FROM `${tableDef.name}`
       |${if (tableDef.primaryKeysCols.nonEmpty) tableDef.primaryKeysCols.map(sqlEscape).mkString("ORDER BY ", ",", "") else ""}
     """.stripMargin.trim


  override def scan(): ScanReport = {
    debug(s"Starting scan $tableDef, ${tableDef.lookupColNames}")
    traceEnd(s"scan $tableDef")

    // ensure that we have some columns to scan
    if (tableDef.lookupColNames.nonEmpty) {

      conn = ds.getConnection
      st = conn.createStatement(ResultSet.FETCH_FORWARD, ResultSet.CONCUR_READ_ONLY)
      st.setFetchSize(fetchSize)
      st.setMaxFieldSize(MaxFieldSize)

      try {

        rs = st.executeQuery(sql())
        val result = reviewRows(1, rs)(ReviewResult())
        traceEnd(s"Review provided ${result.matches.size} results")
        result.matches.foreach(m => {
          scanListener.matchFound(source, m.location)
          report.addMatch(m)
        })

      } catch {
        case NonFatal(e) =>
          report.addError(e.toString, system, tableDef.toString)
          debug(sql())
      }


    }

    report

  }


  private def reviewColString(col: Col, pk: String): ReviewResult = {
    try {
      Option(rs.getString(col.name)).fold(ReviewResult())(reviewString(col, pk, _))
    } catch {
      case NonFatal(e) => e.printStackTrace()
        throw e
    }
  }

  private def reviewColLong(col: Col, pk: String) = reviewString(col, pk,
    rs.getLong(col.name).toString
  )

  private def reviewColBlob(col: Col): ReviewResult = {
    ??? // not currently supported
  }

  private def reviewString(col: Col, pk: String, str: String): ReviewResult = {
    if (str.length < sec.sc.minimumLength) {
      ReviewResult()
    } else {

      val array = str.getBytes("UTF-8")
      analysisBuffer.rewind()
      analysisBuffer.put(array)
      analysisBuffer.rewind()

      // todo overflow of addition to the analysis buffer, initial sizes shouldn't block us currently
      val result = reviewAnalysisBuffer(0, array.length, s"$pk ${col.name}", ReviewResult())
      inspectionBuffer.reset()
      result
    }
  }

  @tailrec
  private def reviewRows(i: Int, rs: ResultSet)(implicit accumulativeResult: ReviewResult): ReviewResult = {

    if (ctx.stopOnFirstMatch && accumulativeResult.matches.nonEmpty) {
      traceEnd("stopOnFirstMatch, returning found result")
      report.incStat("RowsScanned", i - 1)
      accumulativeResult
    } else {

      if (rs.next()) {

        scanListener.scanItem(source, s"Row $i")

        val pk = tableDef.primaryKeysCols.map(s => s"$s=${rs.getString(s)}").mkString(",")

        val reviewed = tableDef.cols.foldLeft(accumulativeResult)((r, col) => {

          report.incStat("Columns")
          val colReview = reviewCol(col, pk)
          trace(s", ${col.name}=${colReview.matches.size}")
          r concat colReview
        })


        traceEnd()

        // recursive review of each row, accumulating as we progress
        reviewRows(i + 1, rs)(reviewed)
      } else {
        //      debug("No further rows")

        report.incStat("RowsScanned", i)
        accumulativeResult
      }

    }
  }


  private def reviewCol(c: Col, pk: String)(implicit accumulativeResult: ReviewResult): ReviewResult = {
    accumulativeResult concat (c.dataType match {
      case Types.VARCHAR => reviewColString(c, pk)
      case Types.NVARCHAR => reviewColString(c, pk)
      case Types.CHAR => reviewColString(c, pk)
      case Types.NCHAR => reviewColString(c, pk)
      case Types.BLOB => reviewColBlob(c)
      case Types.BIGINT => reviewColLong(c, pk)
      case _ => accumulativeResult // ignore
    })
  }


  override def onComplete: Unit = {
    Try(rs.close())
    Try(st.close())
    Try(conn.close())
  }

}
