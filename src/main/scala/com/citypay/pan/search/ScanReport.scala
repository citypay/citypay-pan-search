package com.citypay.pan.search

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import com.citypay.pan.search.source.ScanSourceError
import com.citypay.pan.search.util.Location

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.TimeUnit


object ScanReport {

  object StandardStats {
    val Error = "Error"
  }

  def flatten(name: String, reports: List[ScanReport]): ScanReport = {
    val dst = ScanReport(name)
    reports.foreach(dst.add)
    dst
  }

}


/**
  * A report which is collated by scanning a source
  */
case class ScanReport(name: String) {

  @volatile private var _taken = 0L
  protected var stats: mutable.Map[String, AtomicInteger] = mutable.Map()
  protected val _matches = new ListBuffer[Match]()
  protected val _errors = new ListBuffer[ScanSourceError]()
  protected var _subReports: ListBuffer[ScanReport] = ListBuffer[ScanReport]()
  protected var _meta: mutable.Map[String, String] = mutable.Map()

  def subReports: List[ScanReport] = _subReports.toList

  def addMeta(key: String, value: String): Unit = {
    _meta.put(key, value)
  }

  def meta: Map[String, String] = _meta.toMap

  def add(report: ScanReport): ScanReport = {
    _subReports += report
    report.stats.foreach(s => {
      incStat(s._1, s._2.get())
    })
    _errors ++= report._errors
    this
  }

  def markComplete(taken: Long): ScanReport = {
    _taken = taken
    this
  }

  def taken(unit: TimeUnit): Long = unit.convert(_taken, TimeUnit.MILLISECONDS)

  def incStat(str: String, i: Int): Unit = {
    stats.get(str) match {
      case Some(a) => a.set(a.get() + i)
      case None => stats += (str -> new AtomicInteger(i))
    }
  }

  def incStat(str: String): Unit = {
    incStat(str, 1)
  }

  def statNames: List[String] = stats.keys.toList

  def getStat(str: String): Int = stats.get(str).fold(0)(_.get())

  def addMatch(searchSpec: PanSpec,
               result: String,
               system: String,
               location: String,
               from: Location,
               to: Location,
               offset: Int,
               expectedValue: Array[Byte]): Unit = {
    addMatch(Match(searchSpec, result, system, from, to, offset, location, expectedValue))
  }

  def addMatch(matched: Match): Unit = {
    _matches += matched
  }

  def matches(): List[Match] = _matches.toList

  def matchCount: Int = _matches.size

  def addError(message: String, system: String, location: String): ScanReport = {
    addError(ScanSourceError(message, system, location))
    this
  }

  def addError(error: ScanSourceError): ScanReport = {
    _errors += error
    this
  }

  def errors(): List[ScanSourceError] = _errors.toList

  override def toString: String = s"ScanReport($name, taken=${_taken / 1000d}s stats=[${stats.mkString(",")}], matches=[${_matches.size}], subs=${subReports.size})"
}



