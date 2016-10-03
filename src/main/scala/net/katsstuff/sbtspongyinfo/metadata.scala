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

import scala.collection.JavaConverters._

import org.spongepowered.plugin.meta.PluginMetadata
import org.spongepowered.plugin.meta.PluginMetadata.{Dependency => SpongeDependency}

case class PluginInfo(
		id: String,
		name: Option[String] = None,
		version: Option[String] = None,
		description: Option[String] = None,
		url: Option[String] = None,
		minecraftVersion: Option[String] = None,
		authors: Seq[String] = Nil,
		dependencies: Set[DependencyInfo] = Set(),
		loadBefore: Set[DependencyInfo] = Set(),
		loadAfter: Set[DependencyInfo] = Set()) {

	def toSponge: PluginMetadata = {
		val metadata = new PluginMetadata(id)
		metadata.setName(name.orNull)
		metadata.setVersion(version.orNull)
		metadata.setDescription(description.orNull)
		metadata.setUrl(url.orNull)
		metadata.setMinecraftVersion(minecraftVersion.orNull)
		authors.foreach(metadata.addAuthor)
		dependencies.foreach(d => metadata.addRequiredDependency(d.toSponge))
		loadBefore.foreach(d => metadata.loadBefore(d.toSponge))
		loadAfter.foreach(d => metadata.loadAfter(d.toSponge))
		metadata
	}
}
object PluginInfo {

	def apply(spongeMeta: PluginMetadata): PluginInfo = {
		new PluginInfo(
			id = spongeMeta.getId,
			name = Option(spongeMeta.getName),
			version = Option(spongeMeta.getVersion),
			description = Option(spongeMeta.getDescription),
			url = Option(spongeMeta.getUrl),
			minecraftVersion = Option(spongeMeta.getMinecraftVersion),
			authors = spongeMeta.getAuthors.asScala,
			dependencies = Set(spongeMeta.getRequiredDependencies.asScala.map(DependencyInfo(_)).toSeq: _*),
			loadBefore = Set(spongeMeta.getLoadBefore.asScala.map(DependencyInfo(_)).toSeq: _*),
			loadAfter = Set(spongeMeta.getLoadAfter.asScala.map(DependencyInfo(_)).toSeq: _*)
		)
	}
}

case class DependencyInfo(id: String, version: Option[String] = None) {

	def toSponge: SpongeDependency = new SpongeDependency(id, version.orNull)
}

object DependencyInfo {

	def apply(spongeDependency: SpongeDependency): DependencyInfo = {
		new DependencyInfo(spongeDependency.getId, Option(spongeDependency.getVersion))
	}
}