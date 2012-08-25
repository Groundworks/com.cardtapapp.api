// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % "2.0.3")

resolvers += "gseitz@github" at "http://gseitz.github.com/maven/"

addSbtPlugin("com.github.gseitz" % "sbt-protobuf" % "0.2.2")

