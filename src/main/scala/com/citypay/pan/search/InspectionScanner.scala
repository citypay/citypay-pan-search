package com.citypay.pan.search

import com.citypay.pan.search.io.IndexedByteBuffer
import com.citypay.pan.search.util.{Location, LuhnCheck, Tracer}

/**
  * Directly identifies the progress of inspection
  */
trait InspectionResult {}

final case class InspectedProposedFind(value: String, offset: Int, from: Location, to: Location) extends InspectionResult

final case class InspectedPotential(offset: Int, from: Location, to: Location) extends InspectionResult

object InspectionNoMatch extends InspectionResult {
  override def toString: String = "InspectionNoMatch"
}

/**
  * Search for a pan spec by scanning the inspection buffer provided from a reviewer
  */
object InspectionScanner {

  def isPrefixDelimiter(b: Byte): Boolean = b < '0' || b > '9' && b < 'A' || b > 'Z' && b < 'a' || b > 'z'


  /**
    * Runs an inspection of collated data
    *
    * @param label            a label for the inspection, primarily for tracing
    * @param spec             the spec to search against
    * @param inspectionBuffer the inspection buffer which contains proposed data
    * @param offset           an offset of the bs array to inspect
    * @param limit            the limit to which the review should hold, this is generally to where we
    *                         have yet gathered data to
    * @return an either value of
    *         left, Boolean -> true if the inspection should continue, i.e. its a potental match and requires more data
    *         left, Boolean -> false this is definately not a match
    *         right, String -> the pan which is looked to have matched
    */
  def apply(label: String,
            spec: PanSpec,
            inspectionBuffer: IndexedByteBuffer,
            offset: Int,
            limit: Int
           ): InspectionResult = {

    val _tracer = new Tracer("inspectionScanner")
    import _tracer._


    def scan(): InspectionResult = {

      var startLine: Int = 0
      var startCol: Int = 0

      for (i <- offset until limit) {

        val b = inspectionBuffer.get(i)
        trace("InspectionScanner: %s, spec=%s, i=%06d, offset=%06d, limit=%06d, b=%s (0x%02X)", label, spec, i, offset, limit, b.toChar, b)


        // breakout if non numeric
        if (b < 0x30 || b > 0x39) {
          //noinspection ScalaStyle
          return InspectionNoMatch
        }

        // search using a linear approach O(nm), any search methods such as KMP/BM are inappropriate for this context
        if (i - offset < spec.leadingLen) {

          trace(", leading")

          // if we are in the leading range, the value must be a direct match, continue or breakout as false
          if (spec.leadingBytes(i - offset) != b) {
            trace(", mismatch (%02X != %02X)", spec.leadingBytes(i - offset), b)
            //noinspection ScalaStyle
            return InspectionNoMatch
          }

          if (startLine == 0) {
            startLine = inspectionBuffer.channelIndexLineNo(i)
            startCol = inspectionBuffer.channelIndexColNo(i)
          }

          // if we are at the length, then run a luhn check to verify
        } else if (i >= spec.length - 1) {
          trace(s"proposed(offset=$offset, len=${i + 1}, arrlen=(${inspectionBuffer.length}))")
          val proposed = inspectionBuffer.toString(offset, i + 1)
          if (tracer(", luhn")(LuhnCheck(proposed))) {

            // the proposed value has passed a luhn check
            // to prevent false positives, ensure that the value before the offset is a non alpha numeric value
            // a lot of false positives can be found in id strings such as session ids or UIDs
            if (offset > 0) {

              val prefix = inspectionBuffer.get(offset - 1)
              if (isPrefixDelimiter(prefix)) {
                //noinspection ScalaStyle
                return InspectedProposedFind(proposed, offset, Location(startLine, startCol),
                  Location(inspectionBuffer.channelIndexLineNo(i), inspectionBuffer.channelIndexColNo(i))
                )
              } else {
                // False positive
                return InspectionNoMatch
              }



            } else {
              //noinspection ScalaStyle
              return InspectedProposedFind(proposed, offset, Location(startLine, startCol),
                Location(inspectionBuffer.channelIndexLineNo(i), inspectionBuffer.channelIndexColNo(i))
              )
            }



          } else if (i >= spec.maxLength - 1) {
            traceEnd(", len-exhausted")
            //noinspection ScalaStyle
            return InspectionNoMatch
          }
        }

        traceEnd(", matching...")

      }
      // if nothing yet found, return as potential
      InspectedPotential(offset, Location(startLine, startCol),
        Location(inspectionBuffer.channelIndexLineNo(limit), inspectionBuffer.channelIndexColNo(limit))
      )
    }

    try {

      val result = scan()
      trace(s" returning Inspection Result -> $result")
      result

    } finally {
      traceEnd()
    }

  }

}
