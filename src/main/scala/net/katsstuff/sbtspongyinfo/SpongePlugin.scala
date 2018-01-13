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

import org.spongepowered.plugin.meta.McModInfo

import sbt.Keys._
import sbt._

object SpongePlugin extends AutoPlugin {

	override def requires = plugins.JvmPlugin
	val autoImport: SpongeSbtImports.type = SpongeSbtImports
	import autoImport._

	lazy val baseSettings: Seq[Setting[_]] = Seq[Setting[_]](
		spongeMetaCreate := true,
		spongeApiVersion := "7.0.0",
		spongePluginInfo := PluginInfo(
			id = thisProject.value.id,
			name = Some(name.value),
			version = Some(version.value),
			description = Some(description.value),
			url = homepage.value.map(_.toString)
		),
		spongeMetaFile := generateMcModInfo((resourceManaged in Compile).value / "mcmod.info", spongePluginInfo.value),

		resourceGenerators in Compile += Def.task {
			if(spongeMetaCreate.value) {
				Seq(generateMcModInfo((resourceManaged in Compile).value / "mcmod.info", spongePluginInfo.value))
			} else Seq()
		}.taskValue,

		resolvers += SpongeRepo,
		libraryDependencies += "org.spongepowered" % "spongeapi" % spongeApiVersion.value % Provided
	)

	override def projectSettings: Seq[Setting[_]] = baseSettings

	def generateMcModInfo(file: File, plugin: PluginInfo): File = {
		file.getParentFile.mkdirs()
		McModInfo.DEFAULT.write(file.toPath, plugin.toSponge)
		file
	}
}
