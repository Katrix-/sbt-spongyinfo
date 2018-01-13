import org.spongepowered.plugin.meta.McModInfo

import scala.collection.JavaConverters._

lazy val check = taskKey[Unit]("check")

lazy val commonSettings = Seq(
  name := s"sbt-sponge Double Test-${spongeApiVersion.value}",
  organization := "katrix",
  version := "0.1",
  scalaVersion := "2.12.4",
  check := {
    val file = (resourceManaged in Compile).value / "mcmod.info"
    val read = McModInfo.DEFAULT.read(file.toPath).asScala.head
    assert(spongePluginInfo.value == PluginInfo(read))
  }
)

lazy val v500 = project
  .in(file("5.0.0"))
  .enablePlugins(SpongePlugin)
  .settings(commonSettings: _*)
  .settings(spongeApiVersion := "5.0.0")

lazy val v410 = project
  .in(file("4.1.0"))
  .enablePlugins(SpongePlugin)
  .settings(commonSettings: _*)
  .settings(spongeApiVersion := "4.1.0")
