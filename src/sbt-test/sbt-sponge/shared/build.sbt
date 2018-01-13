import org.spongepowered.plugin.meta.McModInfo
import scala.collection.JavaConverters._

lazy val check = taskKey[Unit]("check")

lazy val commonSettings = Seq(organization := "katrix", version := "0.1", scalaVersion := "2.12.4")

lazy val implSettings = Seq(name := s"sbt-sponge Double Test-${spongeApiVersion.value}", check := {
  val file = (resourceManaged in Compile).value / "mcmod.info"
  val read = McModInfo.DEFAULT.read(file.toPath).asScala.head
  assert(spongePluginInfo.value == PluginInfo(read))
})

lazy val shared = project
  .in(file("shared"))
  .enablePlugins(SpongePlugin)
  .settings(commonSettings: _*)
  .settings(name := "sbt-sponge Double Test-shared", spongeMetaCreate := false)

lazy val v500 = project
  .in(file("5.0.0"))
  .dependsOn(shared)
  .enablePlugins(SpongePlugin)
  .settings(commonSettings: _*)
  .settings(implSettings: _*)
  .settings(spongeApiVersion := "5.0.0")

lazy val v410 = project
  .in(file("4.1.0"))
  .dependsOn(shared)
  .enablePlugins(SpongePlugin)
  .settings(commonSettings: _*)
  .settings(implSettings: _*)
  .settings(spongeApiVersion := "4.1.0")
