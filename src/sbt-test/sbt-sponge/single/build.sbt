import org.spongepowered.plugin.meta.McModInfo

import scala.collection.JavaConverters._

name := "sbt-sponge Single Test"
organization := "katrix"
version := "0.1"

scalaVersion := "2.11.8"

spongePluginInfo := PluginInfo(
	id = "bestPlugin",
	name = Some("The best plugin"),
	version = Some("9.9.999"),
	description = Some("The best plugin ever")
)

libraryDependencies += "org.spongepowered" % "plugin-meta" % "0.2"

enablePlugins(SpongePlugin)

TaskKey[Unit]("check") := {
	val file = (resourceManaged in Compile).value / "mcmod.info"
	val read = McModInfo.DEFAULT.read(file.toPath).asScala.head
	assert(spongePluginInfo.value == PluginInfo(read))
}