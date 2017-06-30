package com.citypay.pan.search.db

import java.sql.{Connection, ResultSet}
import javax.sql.DataSource

import com.jolbox.bonecp.BoneCP

import scala.collection.mutable.ListBuffer

object DbUtils {

  def withConn[T](ds: BoneCP)(fn: Connection => T): T = {
    val conn = ds.getConnection
    try {
      fn(conn)
    } finally {
      conn.close()
    }
  }

  implicit class RsExtender(rs: ResultSet) {

    def toList[T](row: ResultSet => T): List[T] = {
      var buffer = ListBuffer[T]()
      try {
        while (rs.next()) {
          buffer += row(rs)
        }
      } finally {
        rs.close()
      }
      buffer.toList
    }

  }


}
