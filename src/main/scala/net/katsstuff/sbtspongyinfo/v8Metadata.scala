package net.katsstuff.sbtspongyinfo

import org.spongepowered.plugin.metadata.{PluginContributor, PluginDependency, PluginLinks, PluginMetadata}

import java.net.URL
import scala.collection.JavaConverters._

case class PluginInfoV8(
    loader: String,
    id: String,
    name: Option[String] = None,
    version: String,
    mainClass: String,
    description: Option[String] = None,
    links: PluginLinksV8 = PluginLinksV8(),
    contributors: Seq[ContributorV8] = Nil,
    dependencies: Seq[DependencyInfoV8] = Nil,
    extra: Map[String, AnyRef] = Map()
) {

  def toSponge: PluginMetadata = {
    PluginMetadata
      .builder()
      .setLoader(loader)
      .setId(id)
      .setName(name.orNull)
      .setVersion(version)
      .setMainClass(mainClass)
      .setDescription(description.orNull)
      .setLinks(links.toSponge)
      .setContributors(contributors.map(_.toSponge).asJava)
      .setDependencies(dependencies.map(_.toSponge).asJava)
      .setExtraMetadata(extra.asJava)
      .build()
  }
}
object PluginInfoV8 {

  def fromSponge(spongeMeta: PluginMetadata): PluginInfoV8 = {
    def optionalToOption[A](opt: java.util.Optional[A]): Option[A] =
      if (opt.isPresent) Some(opt.get) else None

    new PluginInfoV8(
      loader = spongeMeta.getLoader,
      id = spongeMeta.getId,
      name = optionalToOption(spongeMeta.getName),
      version = spongeMeta.getVersion,
      mainClass = spongeMeta.getMainClass,
      description = optionalToOption(spongeMeta.getDescription),
      links = PluginLinksV8.fromSponge(spongeMeta.getLinks),
      contributors = spongeMeta.getContributors.asScala.map(ContributorV8.fromSponge),
      dependencies = spongeMeta.getDependencies.asScala.map(DependencyInfoV8.fromSponge),
      extra = Map(spongeMeta.getExtraMetadata.asScala.toSeq: _*)
    )
  }
}

case class PluginLinksV8(homepage: Option[URL] = None, source: Option[URL] = None, issues: Option[URL] = None) {

  def toSponge: PluginLinks =
    PluginLinks
      .builder()
      .setHomepage(homepage.orNull)
      .setSource(source.orNull)
      .setIssues(issues.orNull)
      .build()
}
object PluginLinksV8 {

  def fromSponge(spongeLinks: PluginLinks): PluginLinksV8 = {
    def optionalToOption[A](opt: java.util.Optional[A]): Option[A] =
      if (opt.isPresent) Some(opt.get) else None

    new PluginLinksV8(
      optionalToOption(spongeLinks.getHomepage),
      optionalToOption(spongeLinks.getSource),
      optionalToOption(spongeLinks.getIssues)
    )
  }
}

case class ContributorV8(name: String, description: Option[String] = None) {

  def toSponge: PluginContributor =
    PluginContributor
      .builder()
      .setName(name)
      .setDescription(description.orNull)
      .build()
}
object ContributorV8 {

  def fromSponge(spongeContributor: PluginContributor): ContributorV8 = {
    def optionalToOption[A](opt: java.util.Optional[A]): Option[A] =
      if (opt.isPresent) Some(opt.get) else None

    ContributorV8(spongeContributor.getName, optionalToOption(spongeContributor.getDescription))
  }
}

sealed trait LoadOrderV8
object LoadOrderV8 {
  case object Undefined extends LoadOrderV8
  case object After     extends LoadOrderV8

  def toSponge(loadOrder: LoadOrderV8): PluginDependency.LoadOrder = loadOrder match {
    case Undefined => PluginDependency.LoadOrder.UNDEFINED
    case After     => PluginDependency.LoadOrder.AFTER
  }

  def fromSponge(loadOrder: PluginDependency.LoadOrder): LoadOrderV8 = loadOrder match {
    case PluginDependency.LoadOrder.UNDEFINED => Undefined
    case PluginDependency.LoadOrder.AFTER     => After
  }
}
case class DependencyInfoV8(loadOrder: LoadOrderV8, id: String, version: String, optional: Boolean) {

  def toSponge: PluginDependency = {
    PluginDependency
      .builder()
      .setId(id)
      .setVersion(version)
      .setLoadOrder(LoadOrderV8.toSponge(loadOrder))
      .setOptional(optional)
      .build()
  }
}

object DependencyInfoV8 {

  def fromSponge(spongeDependency: PluginDependency): DependencyInfoV8 =
    new DependencyInfoV8(
      LoadOrderV8.fromSponge(spongeDependency.getLoadOrder),
      spongeDependency.getId,
      spongeDependency.getVersion,
      spongeDependency.isOptional
    )
}
