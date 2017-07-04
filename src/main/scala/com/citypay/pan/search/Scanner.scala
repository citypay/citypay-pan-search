package com.citypay.pan.search

import java.io.{File, FileOutputStream, PrintStream}
import java.net.InetAddress
import java.time.LocalDate
import java.util.concurrent.{CountDownLatch, Executors}

import com.citypay.pan.search.report.ReportRendererFactory
import com.citypay.pan.search.source.{CommandLineScanListener, ScanSource, ScanSourceConfigFactory}
import com.citypay.pan.search.util.Util.use
import com.citypay.pan.search.util.{Loggable, NamedThreadFactory, Timeable, Util}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal


/**
  * A scanner singleton instance used to run a scan based on configuration
  * Created by gary on 08/06/2017.
  */
object Scanner extends Loggable with Timeable {

  implicit private val ex = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool(NamedThreadFactory("scanner-svc")))

  import java.util

  private def localName = {
    val env = System.getenv
    val names = "HOSTNAME,HOST,COMPUTERNAME".split(",")
    names.foldLeft[Option[String]](None)((prev, s2) =>
      prev.orElse(Option(env.get(s2)))
    ).getOrElse(InetAddress.getLocalHost.getHostName).toLowerCase
  }

  def runScan(setupConfig: String = "scanner",
              searchConfig: String = "search",
              chdConfig: String = "chd"): Unit = {

    val setupConf = ConfigFactory.load(setupConfig)
    val searchConf = ConfigFactory.load(searchConfig)
    val chdConf = ConfigFactory.load(chdConfig)


    implicit val ctx = new SearchContext {

      override def sources = ScanSourceConfigFactory(searchConf)

      override def config = ScannerConfig(setupConf)

      override def level1: List[PanSpec] = PanSpec.load(chdConf, 1)

      override def level2: List[PanSpec] = PanSpec.load(chdConf, 2)

      override def stopOnFirstMatch: Boolean = config.stopOnFirstMatch

    }

    val file = new File(s"$localName-report-${LocalDate.now().toString}.json")


    log(s"Loaded ${ctx.level1.size} PanSpec(s) level1")
    log(s"Loaded ${ctx.level2.size} PanSpec(s) level2")
    log(s"Loaded ${ctx.sources.size} Source(s)")
    log(s"Current file count: ${Util.OpenFileDescriptorCount}")

    val scanTypeLatch = new CountDownLatch(ctx.sources.length)


    Timed("Scan") {

      val futures = ctx.sources.map(s => {

        s.attachListener(CommandLineScanListener)

        log(s"Scheduling $s")
        val scanType = s.getClass.getSimpleName

        Future {
          try {
            ScanReport.flatten(s.name, s.submitScan(ctx))
          } catch {
            case NonFatal(e) =>
              log(s"Current file count: ${Util.OpenFileDescriptorCount}")
              error(s"Error running $scanType", e)
              val sr = ScanReport(s.name)
              sr.addError(e.getMessage, "", "")

          } finally {
            s.close()
            scanTypeLatch.countDown()
          }
        }
      })


      scanTypeLatch.await()
      log(s"Completed, ${futures.size}")
      Thread.sleep(200)


      use(new FileOutputStream(file)) { fos =>
        use(new PrintStream(fos)) { ps =>
          val renderer = ReportRendererFactory(ps, setupConf)
          val sr = new ScanReport(
            name = s"Pan Search Scan"
          )
          val k = System.getProperties.keys()
          while (k.hasMoreElements) {
            val key = k.nextElement().asInstanceOf[String]
            sr.addMeta(key, System.getProperty(key))
          }
          futures.foreach(f => {
            for {
              s <- f.value
              t <- s.toOption
            } yield {
              sr.add(t)
            }
          })
          renderer.render(sr)
          log("Exit")
        }


      }
    }
  }

  def main(args: Array[String]): Unit = {
    runScan()
  }

}



