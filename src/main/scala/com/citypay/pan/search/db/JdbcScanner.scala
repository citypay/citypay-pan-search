package com.citypay.pan.search.db

import java.sql._

import com.citypay.pan.search._
import com.citypay.pan.search.db.DbUtils._
import com.citypay.pan.search.source.{NoOpScanListener, ScanSource, ScanListener}
import com.citypay.pan.search.util.{CredentialsCallback, Loggable, Timeable, Tracer}
import com.jolbox.bonecp.{BoneCP, BoneCPConfig}


/**
  * The jdbc scanner is a scan source that connects to a database and scans tables within
  * a database schema that match a regular expression pattern. By default all tables are scanned
  *
  * @param driver         The driver used to instantiate a connection, will be loaded by this class
  * @param url            the url to connect
  * @param catalog        a catalog name; must match the catalog name as it is stored in the
  *                       database; "" retrieves those without a catalog; None means that the catalog
  *                       name should not be used to narrow the search. Defaults to None
  * @param schemaPattern  a schema name pattern; must match the schema name as it is stored in
  *                       the database; "" retrieves those without a schema; None means that the
  *                       schema name should not be used to narrow the search. Defaults to None
  * @param tableNameRegex a table name pattern used to select only certain tables from the scan.
  *
  */
class JdbcScanner(driver: String,
                  url: String,
                  credentials: CredentialsCallback,
                  catalog: Option[String] = None,
                  schemaPattern: Option[String] = None,
                  tableNameRegex: Option[String] = None,
                  colNameRegex: Option[String] = None
                 ) extends ScanSource with Loggable with Timeable {

  private val self = this
  override def name: String = "JDBC"

  Class.forName(driver)

  private val cpConfig = new BoneCPConfig()
  cpConfig.setJdbcUrl(url)
  cpConfig.setUsername(credentials.getUsername)
  cpConfig.setPassword(credentials.getPassword)

  private val ds = new BoneCP(cpConfig)

  override def submitScan(context: SearchContext): List[ScanReport] = {

    _listener.started(this)
    try {

      val sec = new ScanExecutionContext(context, name, ScanReport(s"Database scan ${catalog.orElse(schemaPattern).getOrElse("")}"))
      val secs = loadTableMetaData(context).map(tblDef => {

        sec.queue(self, _listener, new ScanExecutor {

          val c = new ColReviewer(ds, tblDef, self, _listener, sec)
          var started = 0L

          override def onComplete: Unit = {
            c.onComplete
            c.report.incStat("MsTaken", (System.currentTimeMillis() - started).toInt)
          }

          override def scan(): ScanReport = {
            started = System.currentTimeMillis()
            val report = c.scan()
            debug(">>>>" + report)
            report
          }

        })


      })
      sec.end()
      //    sec.await()
      secs.flatMap(s => s.map(_.get()))

    } finally {
      _listener.complete(self)
    }

  }

  override def close(): Unit = {
  }

  //noinspection ScalaStyle
  private def loadTableMetaData(context: SearchContext): List[TableDef] = {

    withConn(ds) { conn =>

      val md = conn.getMetaData

      val validTableNames = md.getTables(catalog.orNull, schemaPattern.orNull, null, scala.Array("TABLE"))
        .toList(rs => (rs.getString("TABLE_CAT"), rs.getString("TABLE_SCHEM"), rs.getString("TABLE_NAME")))
        .filter(s => tableNameRegex.fold(true)(regex => s._3.matches(regex)))

      validTableNames.map(n => {

        val tableName = n._3
        val pks = md.getPrimaryKeys(catalog.orNull, schemaPattern.orNull, tableName).toList(_.getString("COLUMN_NAME"))
        val cols = md.getColumns(catalog.orNull, schemaPattern.orNull, tableName, null).toList { row =>
          Col(
            row.getString("COLUMN_NAME"),
            row.getInt("DATA_TYPE"),
            row.getInt("COLUMN_SIZE")
          )
        }
        val dbName = Option(n._1).getOrElse(n._2)
        TableDef(dbName, tableName, pks, cols.filter(shouldScanCol(context, _)))
      })

    }

  }

  def shouldScanCol(context: SearchContext, col: Col): Boolean = {

    debug(s"col: $col")

    val rg = colNameRegex.fold(true)(regex => col.name.matches(regex))
    val dt = (col.dataType, col.sz) match {
      case (Types.VARCHAR, i) => i > context.minimumLength
      case (Types.NVARCHAR, i) => i > context.minimumLength
      case (Types.CHAR, i) => i > context.minimumLength
      case (Types.NCHAR, i) => i > context.minimumLength
      case (Types.BLOB, i) => i > context.minimumLength
      case (Types.BIGINT, _) => true
      case _ => false
    }

    rg && dt
  }

}

case class TableDef(db: String, name: String, primaryKeysCols: List[String], cols: List[Col]) {

  /**
    * provides al columns including primary keys to be used
    */
  val lookupColNames: List[String] = (primaryKeysCols ++ cols.map(_.name)).distinct

  override def toString: String = s"$db.$name"
}

case class Col(name: String, dataType: Int, sz: Int)
