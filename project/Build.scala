import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "typedynamic"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      anorm,
      jdbc,
      "org.jsoup" % "jsoup" % "1.6.3",
      "org.xerial" % "sqlite-jdbc" % "3.7.2",
      "org.mongodb" %% "casbah" % "2.6.3",
      "com.typesafe" % "config" % "0.5.0",
      "org.scalanlp" % "breeze_2.10" % "0.5.2"
      // Add your project dependencies here,
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      // Add your own project settings here      
    )

}
