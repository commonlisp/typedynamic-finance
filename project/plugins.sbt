// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers ++= Seq(
	"Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
	"Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.1")
