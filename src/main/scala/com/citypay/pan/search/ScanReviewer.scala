package com.citypay.pan.search

import java.nio.ByteBuffer

import com.citypay.pan.search.io.IndexedByteBuffer
import com.citypay.pan.search.nio.NioFileSystemScanner.Stats._
import com.citypay.pan.search.source.ScanListener
import com.citypay.pan.search.util.{LuhnCheck, Tracer}

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal


/**
  * Trait which reviews data for a match
  */
trait ScanReviewer {

  def system: String

  def sec: ScanExecutionContext

  def ctx: SearchContext = sec.sc

  def scanListener: ScanListener

  protected def report: ScanReport = sec.report

  protected lazy val analysisBuffer: ByteBuffer = ByteBuffer.allocate(ctx.config.analysisBufferSz)

  protected lazy val inspectionBuffer = new IndexedByteBuffer(sec.sc.maximumLength * 2)


  /**
    * @param inspectionBuffer the inspection buffer to analyse, collated by review
    * @param previous         previous reviews that have been collated through the review process and may match, these
    *                         are returned if not enough data has been established at the time of review and the [[PanSpec]]
    *                         has a `length` greater than the value of `limit`
    * @param offset           an offset from the buffer
    * @param limit            the limit to which the review should hold, this is generally to where we
    *                         have yet gathered data to
    * @return
    */
  def review(inspectionBuffer: IndexedByteBuffer,
             previous: List[PotentialMatch],
             location: String,
             offset: Int,
             limit: Int): List[MatchResult] = {

    // level 1 review
    val result = reviewBufferLevel1(inspectionBuffer, location, previous, offset, limit)
    result.collect {
      // a level 1 match helps to speed up searches of probable matches i.e 20 ranges vs 1000 ranges to check
      // now we have a possible match, filter by level2 matches
      case m: Match => level42Match(m)
      case r: MatchResult => r
    }

  }


  /**
    * Reviews the underlying inspection buffer and returns any matches if found
    *
    * @param inspectionBuffer the inspection buffer to analyse, collated by review
    * @param offset           an offset from the buffer
    * @param limit            the limit to which the review should hold, this is generally to where we
    *                         have yet gathered data to
    * @return
    */
  def reviewBufferLevel1(inspectionBuffer: IndexedByteBuffer,
                         location: String,
                         previous: List[PotentialMatch],
                         offset: Int,
                         limit: Int): List[MatchResult] = {


    def findInSpec(specs: List[PanSpec], accum: List[MatchResult]): List[MatchResult] = {
      specs.headOption.fold(accum) { spec =>
        InspectionScanner("level1", spec, inspectionBuffer, offset, limit) match {
          case i: InspectedPotential =>
            findInSpec(specs.tail, accum :+ PotentialMatch(spec, i.offset, i.from, i.to))
          case i: InspectedProposedFind =>
            accum :+ Match(spec, s"Found ${spec.name} in $location at position ${i.from}", system, i.from, i.to, i.offset, location, i.value.getBytes)
          case _ =>
            findInSpec(specs.tail, accum)
        }
      }
    }

    def findInPrev(specs: List[PotentialMatch], accum: List[MatchResult]): List[MatchResult] = {
      specs.headOption.fold(accum) { prev =>
        InspectionScanner("previo", prev.spec, inspectionBuffer, prev.offset, limit) match {
          case i: InspectedPotential =>
            findInPrev(specs.tail, accum :+ prev)
          case i: InspectedProposedFind =>
            accum :+ Match(prev.spec, s"Found ${prev.spec.name} in $location at position ${i.from}", system, i.from, i.to, i.offset, location, i.value.getBytes)
          case _ =>
            findInPrev(specs.tail, accum)
        }
      }
    }

    findInSpec(ctx.level1, List()) ++ findInPrev(previous, List())

  }

  def checkFalsePositiveRegistry(str: String): Boolean = {
    !ctx.falsePositives.contains(str)
  }


  /**
    * Runs a level 2 match (with a bad reference to Mark King to show my age!)
    * against a level 1 match, this aids in reducing false-positives
    *
    * @param matched the matched value
    * @return an updated match value if found to exist or None otherwise
    */
  def level42Match(matched: Match): MatchResult = {

    val _tracer = new Tracer("analysisBufferL2")
    import _tracer._

    traceEnd(s"Reviewing match $matched")

    @tailrec
    def rec(spec: PanSpec, i: Int): Boolean = {

      trace(s"InspectionScanner: level2, spec=$spec, i=$i")

      val str = new String(matched.expectedValue)

      // if we are equal or over the min length of the spec, run a luhn check on the whole value as a shortcut,
      // return only if a valid match
      if (i >= spec.length && LuhnCheck(str)) {
        traceEnd(", match, luhncheck")

        // check for false positive matches
        checkFalsePositiveRegistry(str)
      }

      // use the expected length to restrict the search, return false if we have exceeded or matched it
      else if (i >= matched.expectedLen) {
        traceEnd(", exhausted")
        false
      }

      // check the leading digits
      else if (i < spec.leadingLen && spec.leadingBytes(i) != matched.expectedValue(i)) {
        traceEnd(", no match")
        false
      }

      // iterate recursively as apt
      else {
        traceEnd(", match...")
        rec(spec, i + 1)
      }

    }


    // only attempt level 2 checks against schemes with the same id and return the first one found
    val declaredMatch = ctx.level2.filter(spec => spec.id == matched.searchSpec.id).find(spec => rec(spec, 0))

    declaredMatch.fold[MatchResult](NoMatch())(d => matched.copy(searchSpec = d))

  }

  private def falsePos(tracer: Tracer) = {

    import tracer._
    trace(", inspection buffer > 2 (reset)")

    // do not add it doesn't look like a card number based on sequence of entered digits
    trace(", reset-buffer")
    inspectionBuffer.reset()
    0
  }

  private def traceReset(i: Int, offset: Int, location: String, reviewPos: Int, tracer: Tracer) = {
    if (i > offset && i % 16 == 0) {
      tracer.traceEnd()
    }
    tracer.trace("\n%s, analysis-buffer: i=%06d, offset=%06d, pos=%06d", location, i, offset + i, reviewPos)
  }

  def reviewAnalysisBuffer(offset: Int,
                           read: Int,
                           location: String,
                           accumulativeResult: ReviewResult
                          ): ReviewResult = {

    var potentialMatch = List[PotentialMatch]()
    var matches = ListBuffer[Match]()
    var reviewPos = 0
    var lineNo = accumulativeResult.lineNo
    var colNo = accumulativeResult.colNo

    val _tracer = new Tracer("analysisBuffer")
    import _tracer._

    try {

      // iterate an index from 0 to read
      for (i <- 0 until read) {
        colNo = colNo + 1
        traceReset(i, offset, location, reviewPos, _tracer)

        val byte = analysisBuffer.get(offset + i) // review the incoming bytes and push to the inspection buffer if numeric
        trace(", byte=0x%02X", byte)
        // restrict further analysis based on an int value
        if (byte >= 0x30 && byte <= 0x39) {
          // we can say that if the last recorded value's index > 2 (i.e. allow 4000-000)
          // then it is immediately a false positive result
          if (inspectionBuffer.position() > 0 && (offset + i - inspectionBuffer.lastEnteredIndex) > 2) {
            reviewPos = falsePos(_tracer)
            // if this is the first inspected numeric then add
            // but only add if it matches possible leading pan digits
          } else if (inspectionBuffer.position() == 0 && ctx.leadingPanDigits.contains(byte)) {
            val j = inspectionBuffer.put(byte, offset + i, lineNo, colNo)
            trace(", L, put(idx=%s, i=%02d)", offset + i, j)
          } else if (inspectionBuffer.position() == 0) {
            trace(", not leadingPanDigit, ignoring")
            // otherwise it seems to be middle numerics that may make up a pan, add and continue
          } else {

            val j = inspectionBuffer.put(byte, offset + i, lineNo, colNo)
            trace(", d, put(idx=%s, i=%02d)", offset + i, j)
            trace(s", ins-pos=${inspectionBuffer.position()}")

            // check inspection buffer length to see if it is in a position for
            // inspection which we can say is the minimum length digits or more
            if (inspectionBuffer.position() >= ctx.minimumLength) {
              traceEnd(s" ---> Inspect (ins-pos=${inspectionBuffer.position()}, rev-pos=$reviewPos) <--- ${potentialMatch.mkString("\n", ",\n", "")}")
              val result = review(inspectionBuffer, potentialMatch, location, reviewPos, inspectionBuffer.position())
              val matched = result.collect { case m: Match => m }
              matched.foreach(m => sec.report.incStat(Matched))
              if (ctx.stopOnFirstMatch && matched.nonEmpty) {
                return ReviewResult(matched, lineNo, colNo)
              }
              matches ++= matched
              trace("Matched=%d", matched.size)
              potentialMatch = result.collect { case pm: PotentialMatch => pm }
              trace(", Potential=%d", potentialMatch.size)

              val noMatches = result.collect { case n: NoMatch => n }
              traceEnd(", Nomatch=%d".format(noMatches.size))

              // if we have no potential matches, we can reset the inspection buffer
              if (potentialMatch.isEmpty) {
                inspectionBuffer.shiftLeft(1)
                trace(s"shift-buffer (${inspectionBuffer.position()})")
                reviewPos = 0
              } else {
                reviewPos = reviewPos + 1
              }


            }


          }

        } else {
          trace(", out-of-range")

          // increment new line
          if (byte == 0x0A) {
            lineNo = lineNo + 1
            colNo = 0
          }

          // offset added to 3 so we can consider windows end of line patterns
          if ((offset + i - inspectionBuffer.lastEnteredIndex) > 3) {
            trace(", inspection buffer > 2 (reset)")

            // do not add it doesn't look like a card number based on sequence of entered digits
            trace(", reset-buffer")
            inspectionBuffer.reset()
            reviewPos = 0
          } else {


            // check for grouping characters (space, -, new line or carriage return) as these are allowed
            if (byte == 0x20 || byte == 0x2D || byte == 0x0A || byte == 0x0D) {
              trace(", acceptable delimiter"); // continue
            } else {
              trace(", reset-buffer")
              inspectionBuffer.reset()
              reviewPos = 0
            }


          }


        }
      }

      ReviewResult(matches.toList, lineNo, colNo)

    } catch {
      case NonFatal(e) => e.printStackTrace()
        throw e
    } finally {
      traceEnd()
    }

  }

}
