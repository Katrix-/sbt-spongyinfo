import org.spongepowered.plugin.meta.McModInfo
import scala.collection.JavaConverters._

lazy val check = taskKey[Unit]("check")

lazy val myProject = crossProject(SpongePlatform("5.0.0"), SpongePlatform("6.0.0")).settings(
  name := s"sbt-sponge Cross Test-${spongeApiVersion.value}",
  organization := "katrix",
  version := "0.1",
  scalaVersion := "2.12.4",
  check := {
    val file = (resourceManaged in Compile).value / "mcmod.info"
    val read = McModInfo.DEFAULT.read(file.toPath).asScala.head
    assert(spongePluginInfo.value == PluginInfo(read))
  }
)
lazy val myProjectV500 = myProject.spongeProject("5.0.0")
lazy val myProjectV600 = myProject.spongeProject("6.0.0")

TaskKey[Unit]("checkGlobal") := {
  assert((spongeApiVersion in myProjectV500).value == "5.0.0")
  assert((spongeApiVersion in myProjectV600).value == "6.0.0")
}
