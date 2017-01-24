name := "My Project"

version := "1.0"

scalaVersion := "2.11.6"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= { Seq (
	"com.typesafe.akka" %% "akka-actor" % "2.4.16",
	"com.typesafe.akka" %% "akka-testkit" % "2.4.16",
	"com.typesafe.akka" %% "akka-stream" % "2.4.16",
	"com.typesafe.akka" %% "akka-http" % "10.0.1",
	"com.typesafe.akka" %% "akka-http-spray-json" % "10.0.1",
	"org.scala-lang.modules" % "scala-xml_2.11" % "1.0.5",
	"mysql" % "mysql-connector-java" % "5.1.24",
	 )
	}
