name := "sbt-spongyinfo"
organization := "net.katsstuff"
version := "0.1-SNAPSHOT"

sbtPlugin := true

resolvers += "SpongePowered" at "http://repo.spongepowered.org/maven"
libraryDependencies += "org.spongepowered" % "plugin-meta" % "0.2"
libraryDependencies += "com.google.code.findbugs" % "jsr305" % "1.3.+"