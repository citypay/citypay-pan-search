package com.citypay.pan.search.util

import java.io.{BufferedReader, InputStreamReader}
import java.lang.management.ManagementFactory
import java.nio.ByteBuffer
import javax.management.MBeanServerFactory

import com.sun.management.UnixOperatingSystemMXBean

import scala.annotation.tailrec
import scala.util.Try
import scala.util.control.NonFatal


/**
  * Util classes used by the scanner
  * Created by gary on 30/05/2017.
  */
object Util {

  // todo unit test
  def DigitsInNumber(i: Int): Int = (Math.floor(Math.log10(i)) + 1).toInt

  // todo unit test
  def FirstDigit(i: Int): Int = {
    @tailrec
    def calc(x: Int, j: Int): Int = {
      if (j == 0) x
      else calc(j % 10, j / 10)
    }

    calc(i, i)
  }

  def hexDump(src: ByteBuffer): String = {
    val width = 16
    val sb = new StringBuilder

    val position = src.position()

    @tailrec
    def row(rowOffset: Int): Unit = {
      if (rowOffset < position) {
        sb.append("%04d:".format(rowOffset))
        val asciiWidth = Math.min(width, position - rowOffset)
        sb.append(" | ")
        colHex(rowOffset, 0, width)
        sb.append(" | ")
        colAscii(rowOffset, 0, width)
        sb.append(System.lineSeparator())
        row(rowOffset + width)
      }
    }

    @tailrec
    def colHex(rowOffset: Int, i: Int, widthRemaining: Int): Unit = {
      if (rowOffset + i < position && i < width) {
        sb.append("%02x ".format(src.get(rowOffset + i)))
        colHex(rowOffset, i + 1, widthRemaining - 1)
      } else {
        sb.append("   " * widthRemaining)
      }
    }

    @tailrec
    def colAscii(rowOffset: Int, i: Int, widthRemaining: Int): Unit = {
      if (rowOffset + i < position && i < width) {
        sb.append(src.get(rowOffset + i).asInstanceOf[Char] match {
          case c if Character.isLetterOrDigit(c) => c
          case _ => "."
        })
        colAscii(rowOffset, i + 1, widthRemaining - 1)
      } else {
        sb.append("   " * widthRemaining)
      }
    }

    row(0)
    src.position(position)
    sb.toString
  }

  def use[A <: {def close() : Unit}, B](resource: A)(code: A ⇒ B): B = {
    try
      code(resource)
    finally
      resource.close
  }

  def getPID: Int = Try(ManagementFactory.getRuntimeMXBean.getName.split("@")(0).toInt).getOrElse(0)

  def stringFromExec(proc: Process): String = {
    use(new BufferedReader(new InputStreamReader(proc.getInputStream))) { in =>
      Stream.continually(in.readLine()).takeWhile(_ != null).mkString("\n")
    }
  }

  def stringFromExec(cmd: String*): String = stringFromExec(Runtime.getRuntime.exec(cmd.toArray))

  def OpenFileDescriptorCount: Long = {
    val m = ManagementFactory.getRuntimeMXBean
    m match {
      case u: UnixOperatingSystemMXBean => u.getOpenFileDescriptorCount
      case _ => {

        Try {
          val p = new ProcessBuilder("/bin/sh", "-c", s"lsof -p $getPID | grep txt | wc -l").redirectErrorStream(true).start()
          stringFromExec(p).trim.toLong
        } getOrElse(0L)

      }
    }

  }

}
