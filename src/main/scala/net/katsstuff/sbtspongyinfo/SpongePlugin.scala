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

import java.io.IOException

import scala.sys.process.Process
import scala.util.{Failure, Success}

import org.spongepowered.plugin.meta.McModInfo

import com.typesafe.sbt.SbtPgp

import okhttp3.{HttpUrl, MultipartBody, OkHttpClient, Request, RequestBody, Response}
import sbt.Keys._
import sbt.{Def, _}
import sbtassembly.AssemblyPlugin
import sjsonnew.shaded.scalajson.ast.unsafe.{JArray, JField, JObject, JString}

object SpongePlugin extends AutoPlugin {

  private val client = new OkHttpClient()

  override def requires = plugins.JvmPlugin && SbtPgp && AssemblyPlugin
  val autoImport: SpongeSbtImports.type = SpongeSbtImports
  import autoImport._

  lazy val spongeSettings: Seq[Setting[_]] = Seq(
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
    resourceGenerators in Compile += Def.taskDyn {
      if (spongeMetaCreate.value) Def.task(Seq(spongeMetaFile.value)) else Def.task(Seq.empty[File])
    }.taskValue,
    resolvers += SpongeRepo,
    libraryDependencies += "org.spongepowered" % "spongeapi" % spongeApiVersion.value % Provided,
    spongeVanillaRunInfo := None,
    spongeForgeRunInfo := None
  )

  lazy val oreSettings: Seq[Setting[_]] = Seq(
    oreUrl := "https://ore.spongepowered.org",
    oreRecommended := true,
    oreChannel := "Release",
    oreCreateForumPost := None,
    oreChangelog := None,
    oreDeploymentKey := None,
    oreDeployFile := AssemblyPlugin.autoImport.assembly.value,
    oreDeploy := Some(signJar.value),
  )

  override def projectConfigurations: Seq[Configuration] = Seq(SpongeVanilla, SpongeForge, ForgeInstall, VanillaInstall)

  lazy val spongeVanillaSettings: Seq[Setting[_]] = Seq(
    fork in (SpongeVanilla, spongeRun) := true,
    baseDirectory in SpongeVanilla := file("runVanilla"),
    connectInput in SpongeVanilla := true,
    outputStrategy in SpongeVanilla := Some(OutputStrategy.StdoutOutput),
    spongeGenerateRun in SpongeVanilla := Def.task {
      val optVanillaVersion = (spongeVanillaRunInfo in SpongeVanilla).value.flatMap(_.spongeVanillaVersion)
      val runDir            = (baseDirectory in SpongeVanilla).value
      val vanillaClasspath  = (managedClasspath in VanillaInstall).value.files
      val logger            = (streams in SpongeVanilla).value.log

      if (optVanillaVersion.isEmpty) {
        logger.info("No SpongeVanilla version defined. spongeGenerateRun will do nothing")
      }

      optVanillaVersion.fold(Seq.empty[File].classpath) { version =>
        val vanillaJar = s"spongevanilla-$version.jar"

        if (!(runDir / vanillaJar).exists()) {
          runDir.mkdirs()
          vanillaClasspath.foreach(file => IO.copyFile(file, runDir / file.getName))
        }

        Seq(runDir / vanillaJar).classpath
      }
    }.value
  ) ++ inConfig(SpongeVanilla)(Defaults.configSettings) ++ fullRunInputTask(
    spongeRun in SpongeVanilla,
    SpongeVanilla,
    "org.spongepowered.server.launch.VersionCheckingMain",
    "--scan-classpath"
  ) ++ Seq(
    (spongeRun in SpongeVanilla) := (spongeRun in SpongeVanilla)
      .dependsOn(spongeGenerateRun in SpongeVanilla)
      .evaluated,
    unmanagedClasspath in SpongeVanilla := Classpaths
      .concat(unmanagedClasspath in SpongeVanilla, spongeGenerateRun in SpongeVanilla)
      .value,
    fullClasspath in SpongeVanilla := {
      val old                          = (fullClasspath in SpongeVanilla).value
      val (classesDirs, nonClassesDir) = old.partition(_.data.name.endsWith("classes"))
      nonClassesDir ++ classesDirs
    }
  )

  lazy val spongeForgeSettings: Seq[Setting[_]] = Seq(
    fork in (SpongeForge, spongeRun) := true,
    baseDirectory in SpongeForge := file("runForge"),
    connectInput in SpongeForge := true,
    outputStrategy in SpongeForge := Some(OutputStrategy.StdoutOutput),
    libraryDependencies ++= (spongeForgeRunInfo in SpongeForge).value
      .flatMap(_.spongeForgeVersion)
      .map(version => "org.spongepowered" % "spongeforge" % version % SpongeForge intransitive ()),
    spongeGenerateRun in SpongeForge := Def.task {
      val optForgeInfo   = (spongeForgeRunInfo in SpongeForge).value
      val runDir         = (baseDirectory in SpongeForge).value
      val forgeJavaOpts  = (javaOptions in ForgeInstall).value
      val forgeClasspath = (managedClasspath in ForgeInstall).value.files
      val logger         = (streams in SpongeForge).value.log

      if (optForgeInfo.isEmpty) {
        logger.info("No forge info defined. spongeGenerateRun will do nothing")
      }

      optForgeInfo.fold(Seq.empty[File].classpath) { info =>
        val forgeJar = s"forge-${info.minecraftVersion}-${info.forgeVersion}-universal.jar"

        if (!(runDir / forgeJar).exists()) {
          runDir.mkdirs()
          runForgeInstaller(runDir, forgeJavaOpts, forgeClasspath, logger)
        }

        Seq(runDir / forgeJar).classpath
      }
    }.value,
    javaOptions in SpongeForge += "-Dfml.coreMods.load=org.spongepowered.mod.SpongeCoremod"
  ) ++ inConfig(SpongeForge)(Defaults.configSettings) ++ fullRunInputTask(
    spongeRun in SpongeForge,
    SpongeForge,
    "net.minecraftforge.fml.relauncher.ServerLaunchWrapper",
    "nogui"
  ) ++ Seq(
    (spongeRun in SpongeForge) := (spongeRun in SpongeForge).dependsOn(spongeGenerateRun in SpongeForge).evaluated,
    unmanagedClasspath in SpongeForge := Classpaths
      .concat(unmanagedClasspath in SpongeForge, spongeGenerateRun in SpongeForge)
      .value
  )

  lazy val forgeInstallSettings: Seq[Setting[_]] = Seq(
    resolvers += "Forge" at "https://files.minecraftforge.net/maven",
    libraryDependencies ++= {
      (spongeForgeRunInfo in SpongeForge).value.map { info =>
        val minecraftVersion = info.minecraftVersion
        val forgeVersion     = info.forgeVersion

        "net.minecraftforge" % "forge" % s"$minecraftVersion-$forgeVersion" % ForgeInstall classifier "installer"
      }.toList
    },
    managedClasspath in ForgeInstall := Classpaths
      .managedJars(ForgeInstall, (classpathTypes in ForgeInstall).value, update.value)
  )

  lazy val vanillaInstallSettings: Seq[Setting[_]] = Seq(
    libraryDependencies ++= (spongeVanillaRunInfo in SpongeVanilla).value
      .flatMap(_.spongeVanillaVersion)
      .map(version => "org.spongepowered" % "spongevanilla" % version % VanillaInstall intransitive ()),
    managedClasspath in VanillaInstall := Classpaths
      .managedJars(VanillaInstall, (classpathTypes in VanillaInstall).value, update.value)
  )

  override def projectSettings: Seq[Setting[_]] =
    spongeSettings ++ oreSettings ++ spongeVanillaSettings ++ spongeForgeSettings ++ forgeInstallSettings ++ vanillaInstallSettings

  def generateMcModInfo(file: File, plugin: PluginInfo): File = {
    file.getParentFile.mkdirs()
    McModInfo.DEFAULT.write(file.toPath, plugin.toSponge)
    file
  }

  val signJar = Def.taskDyn {
    val deployFile   = oreDeployFile.value
    val signer       = SbtPgp.autoImport.PgpKeys.pgpSigner.value
    val logger       = streams.value
    val gpgExtension = com.typesafe.sbt.pgp.gpgExtension
    val signature    = signer.sign(deployFile, file(deployFile.getAbsolutePath + gpgExtension), logger)
    deploy(deployFile, signature, logger)
  }

  def deploy(jar: File, signature: File, logger: TaskStreams): Def.Initialize[Task[(sbt.File, sbt.File)]] =
    Def.task {
      require(oreDeploymentKey.value.isDefined, "Ore API key needs to be set")
      val pluginInfo = spongePluginInfo.value
      require(pluginInfo.version.isDefined, "Plugin version can't be empty")

      val oreUrlValue = oreUrl.value
      val usedUrl     = if (oreUrlValue.endsWith("/")) oreUrlValue.dropRight(1) else oreUrlValue
      val projectUrl  = s"$usedUrl/api/projects/${pluginInfo.id}/versions/${pluginInfo.version.get}"

      val body = {
        val builder = new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("apiKey", oreDeploymentKey.value.get)
          .addFormDataPart("channel", oreChannel.value)
          .addFormDataPart("recommended", oreRecommended.value.toString)
          .addFormDataPart("pluginFile", jar.name, RequestBody.create(null, jar))
          .addFormDataPart("pluginSig", signature.name, RequestBody.create(null, signature))
        oreCreateForumPost.value.foreach(forumPost => builder.addFormDataPart("forumPost", forumPost.toString))
        oreChangelog.value.foreach(changelog => builder.addFormDataPart("changelog", changelog))

        builder.build()
      }

      val request = new Request.Builder().url(projectUrl).post(body).build()

      var response: Response = null
      try {
        logger.log.info(s"Deploying ${jar.name} to $projectUrl")
        response = client.newCall(request).execute()
        val status  = response.code()
        val created = status == 201
        if (!created) {
          val body = response.body().string()
          throw new IOException(s"$status Could not deploy plugin. ${response.message()}\n$body")
        }
        logger.log.debug(response.body().string())
        logger.log.info(s"Successfully deployed ${jar.name} to Ore")
      } finally {
        if (response != null) {
          response.close()
        }
      }

      (jar, signature)
    } tag (Tags.Publish, Tags.Network)

  private def removeSnapshot(version: String): String =
    if (version.endsWith("-SNAPSHOT")) version.substring(0, version.length - 9) else version

  def runForgeInstaller(installDir: File, javaOptions: Seq[String], classpath: Seq[File], log: Logger): Unit = {
    require(classpath.nonEmpty, "Can't run the Forge installer with an empty classpath")

    val options = Seq(
      "java",
      "-cp",
      Path.makeString(classpath),
      "net.minecraftforge.installer.SimpleInstaller",
      "-installServer"
    ) ++ javaOptions
    log.info(options.mkString(" "))

    val exitCode = Process(options, installDir) ! log
    if (exitCode != 0) sys.error("Error while generating forge installation")
  }

  //Some day these will work

  def downloadApiUrl(platform: String): HttpUrl.Builder =
    HttpUrl
      .parse(s"https://dl-api.spongepowered.org/v1/org.spongepowered/$platform/downloads")
      .newBuilder()
      .addQueryParameter("limit", "1")

  def currentSpongeVanillaVersion(spongeApiVersion: String, minecraftVersion: String): String = {
    val spongeVersion = removeSnapshot(spongeApiVersion)
    val bleeding      = spongeApiVersion != spongeVersion

    val url = downloadApiUrl("spongevanilla")
      .addQueryParameter("type", if (bleeding) "bleeding" else "stable")
      .addQueryParameter("spongeapi", spongeVersion)
      .addQueryParameter("minecraft", minecraftVersion)
      .build()

    findVersion(url)
  }

  def currentSpongeForgeVersion(spongeApiVersion: String, fullForgeVersion: String): String = {
    val spongeVersion = removeSnapshot(spongeApiVersion)
    val bleeding      = spongeApiVersion != spongeVersion

    val url = downloadApiUrl("spongeforge")
      .addQueryParameter("type", if (bleeding) "bleeding" else "stable")
      .addQueryParameter("spongeapi", spongeVersion)
      .addQueryParameter("forge", fullForgeVersion)
      .build()

    findVersion(url)
  }

  def findVersion(url: HttpUrl): String = {
    var response: Response = null
    try {
      response = client.newCall(new Request.Builder().url(url).build()).execute()
      val body = response.body().string()
      if (response.isSuccessful) {
        sjsonnew.support.scalajson.unsafe.Parser.parseFromString(body) match {
          case Success(JArray(Array(JObject(fields)))) =>
            fields
              .collectFirst {
                case JField("version", JString(v)) => v
              }
              .getOrElse {
                throw new IOException("Could not find Sponge version that matched the specified criteria")
              }
          case Success(value) =>
            throw new IOException(s"Unexpected response from Sponge download API.\n${value.toStandard}")
          case Failure(e) =>
            throw new IOException("Failed to find sponge platform dependency", e)
        }
      } else
        sys.error(s"Unsuccessful call to Sponge download API: ${response.code()} ${response.message()}\n$body")
    } finally {
      if (response != null) {
        response.close()
      }
    }
  }
}
