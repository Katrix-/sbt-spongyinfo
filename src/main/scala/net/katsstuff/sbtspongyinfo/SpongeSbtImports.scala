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

object SpongeSbtImports {

  final val SpongeRepo = "SpongePowered" at "https://repo.spongepowered.org/maven"

  final val SpongePlugin = sbtspongyinfo.SpongePlugin

  final val PluginInfo = sbtspongyinfo.PluginInfo
  type PluginInfo = sbtspongyinfo.PluginInfo

  final val DependencyInfo = sbtspongyinfo.DependencyInfo
  type DependencyInfo = sbtspongyinfo.DependencyInfo

  final val LoadOrder = sbtspongyinfo.LoadOrder
  type LoadOrder = sbtspongyinfo.LoadOrder

  lazy val spongeApiVersion = settingKey[String]("The version of sponge to use")
  lazy val spongePluginInfo = settingKey[PluginInfo]("What info to include in the mcmod.info file")
  lazy val spongeMetaCreate = settingKey[Boolean]("If the meta mcmod.info file should be created")
  lazy val spongeMetaFile   = taskKey[File]("Creates a mcmod.info file")

  lazy val oreUrl           = settingKey[String]("The url to use for Ore")
  lazy val oreRecommended   =
    settingKey[Boolean]("If the plugin should be set as the recommended plugin when uploaded to Ore")
  lazy val oreChannel       = settingKey[String]("The channel to upload to when uploading a plugin to Ore")
  lazy val oreDeploymentKey = settingKey[Option[String]]("An API key used to export a project to Ore")
  lazy val oreDeploy        = taskKey[Option[(File, File)]]("Uploads a plugin to Ore")
}
