enablePlugins(JavaAppPackaging)

import sbt.Keys.libraryDependencies

lazy val sevenzipjbindingVersion = "9.20-2.00beta"

lazy val root = (project in file("."))
  .settings(
    name := "citypay-pan-search",
    version := "0.1.0",
    scalaVersion := "2.12.2",
    libraryDependencies ++= Seq(

      "net.sf.sevenzipjbinding" % "sevenzipjbinding" % sevenzipjbindingVersion,
      "net.sf.sevenzipjbinding" % "sevenzipjbinding-all-platforms" % sevenzipjbindingVersion,
      "com.typesafe" % "config" % "1.3.1",
      "org.slf4j" % "slf4j-api" % "1.7.25",
      "com.jolbox" % "bonecp" % "0.8.0.RELEASE",
      "org.json4s" % "json4s-native_2.12" % "3.5.2",

      // testing dependencies
      "org.scalatest" %% "scalatest" % "3.0.1" % Test,
      "org.slf4j" % "slf4j-simple" % "1.7.25"
    )
  )


mainClass in Compile := Some("com.citypay.pan.search.Scanner")

mappings in Universal <+= (packageBin in Compile, sourceDirectory ) map {(_, src) =>
      src / "main" / "resources" / "chd.conf" -> "conf/chd.conf"
}
mappings in Universal <+= (packageBin in Compile, sourceDirectory ) map {(_, src) =>
  src / "main" / "resources" / "scanner.conf" -> "conf/scanner.conf"
}
mappings in Universal <+= (packageBin in Compile, sourceDirectory ) map {(_, src) =>
  src / "main" / "resources" / "search.conf" -> "conf/search.conf"
}


