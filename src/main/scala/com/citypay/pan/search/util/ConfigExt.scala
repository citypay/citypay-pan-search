package com.citypay.pan.search.util

import com.typesafe.config.Config

import scala.util.control.NonFatal

/**
  * Created by gary on 08/06/2017.
  */
object ConfigExt {

  implicit class ConfigExt(config: Config) {

    def boolean(path: String, default: Boolean): Boolean = $(_.getBoolean(path), default)

    def int(path: String, default: Int): Int = $(_.getInt(path), default)

    def long(path: String, default: Long): Long = $(_.getLong(path), default)

    def double(path: String, default: Double): Double = $(_.getDouble(path), default)

    def string(path: String, default: String): String = $(_.getString(path), default)

    def stringOpt(path: String): Option[String] = opt(_.getString(path))

    def $[T](fn: Config => T, default: T): T = {
      try {
        val f = fn(config)
        if (f == null) default else f
      } catch {
        case NonFatal(e) => default
      }
    }

    def opt[T](fn: Config => T): Option[T] = {
      try {
        Option(fn(config))
      } catch {
        case NonFatal(e) => None
      }
    }

  }

}
