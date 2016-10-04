name := "sbt-spongyinfo"
organization := "net.katsstuff"
version := "1.0"

sbtPlugin := true

resolvers += "SpongePowered" at "http://repo.spongepowered.org/maven"
libraryDependencies += "org.spongepowered" % "plugin-meta" % "0.2"
libraryDependencies += "com.google.code.findbugs" % "jsr305" % "1.3.+"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

bintrayReleaseOnPublish in ThisBuild := false
bintrayVcsUrl := Some("git@github.com:Katrix-/sbt-spongyinfo.git")