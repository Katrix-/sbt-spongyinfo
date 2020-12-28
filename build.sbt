name := "sbt-spongyinfo"
organization := "net.katsstuff"
version := "1.5"

description := "Easier Sponge plugins for SBT"

sbtPlugin := true

resolvers += "SpongePowered" at "http://repo.spongepowered.org/maven"
libraryDependencies += "org.spongepowered" % "plugin-meta" % "0.4.1"
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")
addSbtPlugin("org.portable-scala" % "sbt-crossproject" % "0.5.0")

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

publishMavenStyle := false
bintrayRepository := "sbt-plugins"
bintrayOrganization in bintray := None

bintrayReleaseOnPublish in ThisBuild := false
bintrayVcsUrl := Some("git@github.com:Katrix/sbt-spongyinfo.git")
