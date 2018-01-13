name := "sbt-spongyinfo"
organization := "net.katsstuff"
version := "1.1-SNAPSHOT"

sbtPlugin := true

resolvers += "SpongePowered" at "http://repo.spongepowered.org/maven"
libraryDependencies += "org.spongepowered" % "plugin-meta" % "0.4.1"
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

bintrayReleaseOnPublish in ThisBuild := false
bintrayVcsUrl := Some("git@github.com:Katrix-/sbt-spongyinfo.git")
