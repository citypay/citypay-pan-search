package com.citypay.pan.search.source

import com.citypay.pan.search.db.JdbcScanner
import com.citypay.pan.search.nio.NioFileSystemScanner
import com.citypay.pan.search.util.ConfigExt._
import com.citypay.pan.search.util.{CredentialsCallback, NoOpCredentials}
import com.typesafe.config.Config

import scala.collection.JavaConverters._

/**
  * Factory which loads a [[ScanSource]] bsaed on configuration
  */
object ScanSourceConfigFactory {

  def apply(config: Config): List[ScanSource] = {
    config.getObjectList("search.source").asScala.toList
      .flatMap(c => adapt(c.toConfig))
  }

  def expandRoots(list: List[String]): List[String] = {
    list.collect {
      case "user.home" => System.getProperty("user.home")
      case s:String => s
    }
  }

  def adapt(c: Config): Option[ScanSource] = {
    Option(c.getString("type").toLowerCase match {

      case "file" =>

        NioFileSystemScanner(
          expandRoots(c.getStringList("root").asScala.toList),
          c.string("pattern", "**"), // default glob
          c.stringOpt("exclude"),
          c.boolean("includeHiddenFiles", default = false),
          c.boolean("recursive", default = true),
          c.int("maxDepth", default = -1)
        )

      case "db" =>

        val credentials = c.stringOpt("credentials").fold[CredentialsCallback](new NoOpCredentials)(s =>
          Class.forName(s).newInstance().asInstanceOf[CredentialsCallback]
        )

        new JdbcScanner(
          c.getString("driver"),
          c.getString("url"),
          credentials,
          c.stringOpt("catalog"),
          c.stringOpt("schema"),
          c.stringOpt("tableNameRegex"),
          c.stringOpt("colNameRegex")
        )


      case _ => null

    })
  }


}

