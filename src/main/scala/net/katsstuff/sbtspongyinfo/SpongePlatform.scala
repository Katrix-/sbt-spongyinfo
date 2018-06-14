package net.katsstuff.sbtspongyinfo

import sbt._
import sbtcrossproject.Platform
import SpongeSbtImports._

case class SpongePlatform(version: String) extends Platform {
  override def identifier: String = s"sponge$version"
  override def sbtSuffix:  String = s"Sponge${version.replace('.', '-')}"
  override def enable(project: Project) =
    project.enablePlugins(SpongePlugin).settings(spongeApiVersion := version)
}
