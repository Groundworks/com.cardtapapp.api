resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Twitter" at "http://maven.twttr.com/"

libraryDependencies += "com.twitter" % "querulous" % "2.7.0"

libraryDependencies += "com.google.protobuf" % "protobuf-java" % "2.4.1"

libraryDependencies += "com.googlecode.protobuf-java-format" % "protobuf-java-format" % "1.2"

libraryDependencies += "postgresql" % "postgresql" % "9.1-901.jdbc4"

libraryDependencies += "junit" % "junit" % "4.8.1" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.8" % "test"

libraryDependencies += "net.databinder.dispatch" %% "core" % "0.9.0" % "test"
