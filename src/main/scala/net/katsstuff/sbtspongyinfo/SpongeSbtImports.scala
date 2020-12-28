/*
 * This file is part of sbt-spongyinfo, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 Katrix
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package net.katsstuff.sbtspongyinfo

import net.katsstuff.sbtspongyinfo
import sbt._
import sbt.Keys._
import sbtcrossproject.CrossProject

object SpongeSbtImports {

  final val SpongeRepo         = "SpongePowered" at "https://repo-new.spongepowered.org/repository/maven-releases"
  final val SpongeSnapshotRepo = "SpongePowered-snapshots" at "https://repo-new.spongepowered.org/repository/maven-snapshots"

  final val SpongePlugin = sbtspongyinfo.SpongePlugin

  object spongeV7 {
    final val PluginInfo = sbtspongyinfo.PluginInfoV7
    type PluginInfo = sbtspongyinfo.PluginInfoV7

    final val Dependency = sbtspongyinfo.DependencyInfoV7
    type DependencyInfo = sbtspongyinfo.DependencyInfoV7

    final val LoadOrder = sbtspongyinfo.LoadOrderV7
    type LoadOrder = sbtspongyinfo.LoadOrderV7

    lazy val pluginInfo = SettingKey[PluginInfo]("spongePluginInfoV7", "What info to include in the mcmod.info file")
    lazy val metaCreate = SettingKey[Boolean]("spongeMetaCreateV7", "If the mcmod.info file should be created")
    lazy val metaFile   = TaskKey[File]("spongeMetaFileV8", "Creates the mcmod.info file")
  }

  object spongeV8 {
    final val PluginInfo = sbtspongyinfo.PluginInfoV8
    type PluginInfo = sbtspongyinfo.PluginInfoV8

    final val Links = sbtspongyinfo.PluginLinksV8
    type Links = sbtspongyinfo.PluginLinksV8

    final val Contributor = sbtspongyinfo.ContributorV8
    type Contributor = sbtspongyinfo.ContributorV8

    final val Dependency = sbtspongyinfo.DependencyInfoV8
    type DependencyInfo = sbtspongyinfo.DependencyInfoV8

    final val LoadOrder = sbtspongyinfo.LoadOrderV8
    type LoadOrder = sbtspongyinfo.LoadOrderV8

    lazy val pluginLoader = SettingKey[String](
      "spongeDefaultPluginLoaderV8",
      "The loader to use. Use this setting to easily apply one loader to all plugins in the project"
    )
    lazy val pluginInfo =
      SettingKey[Seq[PluginInfo]]("spongePluginInfoV8", "What info to include in the plugins.json file")
    lazy val metaCreate = SettingKey[Boolean]("spongeMetaCreateV8", "If the plugins.json file should be created")
    lazy val metaFile   = TaskKey[File]("spongeMetaFileV8", "Creates the plugins.json file")
  }

  final val SpongePlatform = sbtspongyinfo.SpongePlatform
  type SpongePlatform = sbtspongyinfo.SpongePlatform.type

  //TODO: Only approved plugins
  def oreDependency(id: String, version: String): ModuleID =
    "ore" % id % version from s"https://ore.spongepowered.org/api/projects/$id/versions/$version/download"

  lazy val SpongeVanilla  = config("spongeVanilla").extend(Runtime)
  lazy val SpongeForge    = config("spongeForge").extend(Runtime)
  lazy val ForgeInstall   = config("forgeinstall").hide
  lazy val VanillaInstall = config("vanillainstall").hide

  lazy val spongeApiVersion = settingKey[String]("The version of sponge to use")

  lazy val oreUrl = settingKey[String]("The url to use for Ore")
  lazy val oreApiV1Id =
    settingKey[String]("The Ore APIv1 identifier for the project. For older project this is your plugin id")
  lazy val oreVersion = settingKey[String]("Version to deploy the project to")
  @deprecated("Ore no longer respects this", since = "1.5")
  lazy val oreRecommended =
    settingKey[Boolean]("If the plugin should be set as the recommended plugin when uploaded to Ore")
  @deprecated("Ore will soon stop respecting this", since = "1.5")
  lazy val oreChannel   = settingKey[String]("The channel to upload to when uploading a plugin to Ore")
  lazy val oreChangelog = taskKey[Option[String]]("The changelog to send when publishing a plugin to Ore")
  lazy val oreCreateForumPost =
    taskKey[Option[Boolean]]("If a forum post should be created when deploying the new version")
  lazy val oreDeploymentKey = settingKey[Option[String]]("An API key used to export a project to Ore")
  lazy val oreDeployFile    = taskKey[File]("The file which should be deployed to Ore")
  lazy val oreDeploy        = taskKey[Option[(File, File)]]("Uploads a plugin to Ore")

  implicit class SpongeCrossProjectOps(private val project: CrossProject) extends AnyVal {
    def spongeProject(version: String): Project = project.projects(SpongePlatform(version))

    def spongeSettings(version: String)(ss: Def.SettingsDefinition*): CrossProject =
      project.configurePlatform(SpongePlatform(version))(_.settings(ss: _*))
  }

  final val SpongeForgeRunInfo = sbtspongyinfo.SpongeForgeRunInfo
  type SpongeForgeRunInfo = sbtspongyinfo.SpongeForgeRunInfo

  final val SpongeVanillaRunInfo = sbtspongyinfo.SpongeVanillaRunInfo
  type SpongeVanillaRunInfo = sbtspongyinfo.SpongeVanillaRunInfo

  lazy val spongeForgeRunInfo = settingKey[Option[SpongeForgeRunInfo]]("All the information SpongeForge needs to run")
  lazy val spongeVanillaRunInfo =
    settingKey[Option[SpongeVanillaRunInfo]]("All the information SpongeVanilla needs to run")

  lazy val spongeGenerateRun = taskKey[Classpath]("Generates the needed files for forge to run")
  lazy val spongeRun         = inputKey[Unit]("Run Sponge")
}
