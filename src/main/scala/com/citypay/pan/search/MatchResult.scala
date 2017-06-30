package com.citypay.pan.search

import com.citypay.pan.search.util.Location


/**
  * A result type from an inspection process when attempting to find a matched pan
  */
trait MatchResult {}

/**
  * Result when no match is found for a pan spec
  */
case class NoMatch() extends MatchResult

/**
  * Result when a match has potential however not enough information is yet available
  *
  * @param spec   the spec that's matching
  * @param offset the previous offset to use
  */
case class PotentialMatch(spec: PanSpec,
                          offset: Int,
                          from: Location,
                          to: Location
                         ) extends MatchResult

