package com.citypay.pan.search

case class ReviewResult(matches: List[Match] = Nil,
                        lineNo: Int = 1,
                        colNo: Int = 1) {

  def ++(result: ReviewResult): ReviewResult =
    ReviewResult(matches ++ result.matches, result.lineNo, result.colNo)

}

