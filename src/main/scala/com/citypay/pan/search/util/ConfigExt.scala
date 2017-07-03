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
package com.citypay.pan.search.util

import com.typesafe.config.Config

import scala.util.control.NonFatal

/**
  * Created by gary on 08/06/2017.
  */
object ConfigExt {

  implicit class ConfigExt(config: Config) {

    def boolean(path: String, default: Boolean): Boolean = defaultValue(_.getBoolean(path), default)

    def int(path: String, default: Int): Int = defaultValue(_.getInt(path), default)

    def long(path: String, default: Long): Long = defaultValue(_.getLong(path), default)

    def double(path: String, default: Double): Double = defaultValue(_.getDouble(path), default)

    def string(path: String, default: String): String = defaultValue(_.getString(path), default)

    def stringOpt(path: String): Option[String] = opt(_.getString(path))

    def defaultValue[T](fn: Config => T, default: T): T = {
      try {
        Option(fn(config)).getOrElse(default)
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
