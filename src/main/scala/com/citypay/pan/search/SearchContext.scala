package com.citypay.pan.search

import java.util.concurrent._

import com.citypay.pan.search.source.ScanSource
import com.citypay.pan.search.util.NamedThreadFactory

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

/**
  * Defines the context for a search
  */
trait SearchContext {

  /**
    * @return a specification of searches used for level 1 searching. Level 1 searches are used against the core
    *         data in the file to attempt to locate a match. Once a match has been found its potential is realised
    *         by further review against a level 2 test which is expected to be higher detail of a bin range
    */
  def level1: List[PanSpec]

  /**
    * @return a level 2 list of specs, see [[level1]]
    */
  def level2: List[PanSpec] = level1

  /**
    * @return a list of strings which are false positive matches and should be ignored
    */
  def falsePositives: List[String] = Nil

  /**
    * @return a list of scan sources
    */
  def sources: List[ScanSource]

  /**
    * @return true if the search should stop when a single PAN is found, this may speed up searches to black list
    *         files for further analysis
    */
  def stopOnFirstMatch: Boolean


  /**
    * All possible distinct leading single byte values
    *
    */
  val leadingPanDigits: List[Byte] = level1.map(_.firstDigit.toString.getBytes.head).distinct

  /**
    * The calculated mimumum length of any pan based on the configured specs
    */
  val minimumLength: Int = level1.foldLeft(16)((i, s) => if (s.length < i) s.length else i)

  /**
    * The calculated maximum length of any pan based on the configured specs
    */
  val maximumLength: Int = level1.foldLeft(16)((i, s) => if (s.maxLength > i) s.maxLength else i)

  def config: ScannerConfig

  // run some sanity checks against the search context
  assume(level1.nonEmpty, "No level1 specs, cannot search")
  assume(level2.nonEmpty, "No level2 specs, cannot search")

}


