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

import org.spongepowered.plugin.meta.{PluginDependency, PluginMetadata}

case class PluginInfoV7(
    id: String,
    name: Option[String] = None,
    version: Option[String] = None,
    description: Option[String] = None,
    url: Option[String] = None,
    authors: Seq[String] = Nil,
    dependencies: Set[DependencyInfoV7] = Set(),
    extra: Map[String, _] = Map()
) {

  def toSponge: PluginMetadata = {
    val metadata = new PluginMetadata(id)
    metadata.setName(name.orNull)
    metadata.setVersion(version.orNull)
    metadata.setDescription(description.orNull)
    metadata.setUrl(url.orNull)
    authors.foreach(metadata.addAuthor)
    dependencies.foreach(d => metadata.addDependency(d.toSponge))
    extra.foreach(tuple => metadata.setExtension(tuple._1, tuple._2))
    metadata
  }
}
object PluginInfoV7 {

  def apply(spongeMeta: PluginMetadata): PluginInfoV7 = {
    new PluginInfoV7(
      id = spongeMeta.getId,
      name = Option(spongeMeta.getName),
      version = Option(spongeMeta.getVersion),
      description = Option(spongeMeta.getDescription),
      url = Option(spongeMeta.getUrl),
      authors = spongeMeta.getAuthors.asScala,
      dependencies = spongeMeta.getDependencies.asScala.map(DependencyInfoV7.apply).toSet,
      extra = Map(spongeMeta.getExtensions.asScala.toSeq: _*)
    )
  }
}

sealed trait LoadOrderV7
object LoadOrderV7 {
  case object None   extends LoadOrderV7
  case object Before extends LoadOrderV7
  case object After  extends LoadOrderV7

  def toSponge(loadOrder: LoadOrderV7): PluginDependency.LoadOrder = loadOrder match {
    case None   => PluginDependency.LoadOrder.NONE
    case Before => PluginDependency.LoadOrder.BEFORE
    case After  => PluginDependency.LoadOrder.AFTER
  }

  def fromSponge(loadOrder: PluginDependency.LoadOrder): LoadOrderV7 = loadOrder match {
    case PluginDependency.LoadOrder.NONE   => None
    case PluginDependency.LoadOrder.BEFORE => Before
    case PluginDependency.LoadOrder.AFTER  => After
  }
}
case class DependencyInfoV7(loadOrder: LoadOrderV7, id: String, version: Option[String] = None, optional: Boolean) {

  def toSponge: PluginDependency = new PluginDependency(LoadOrderV7.toSponge(loadOrder), id, version.orNull, optional)
}

object DependencyInfoV7 {

  def apply(spongeDependency: PluginDependency): DependencyInfoV7 =
    new DependencyInfoV7(
      LoadOrderV7.fromSponge(spongeDependency.getLoadOrder),
      spongeDependency.getId,
      Option(spongeDependency.getVersion),
      spongeDependency.isOptional
    )
}
