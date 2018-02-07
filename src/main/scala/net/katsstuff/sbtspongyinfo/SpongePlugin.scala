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

  override def projectConfigurations: Seq[Configuration] = Seq(SpongeVanilla, SpongeForge, ForgeInstall)

  lazy val spongeVanillaSettings: Seq[Setting[_]] = inConfig(SpongeVanilla) {
    Seq(
      mainClass := Some("org.spongepowered.server.launch.VersionCheckingMain"),
      fork in run := true,
      baseDirectory in run := file("runVanilla"),
      spongeFullVersion := currentSpongeVanillaVersion(spongeApiVersion.value, spongeMinecraftVersion.value),
      libraryDependencies += "org.spongepowered" % "spongevanilla" % spongeFullVersion.value classifier "dev"
    )
  }

  lazy val spongeForgeSettings: Seq[Setting[_]] = inConfig(SpongeForge) {
    Seq(
      unmanagedClasspath := Classpaths.concat(unmanagedClasspath, spongeGenerateForgeRun).value,
      mainClass := Some("net.minecraftforge.fml.relauncher.ServerLaunchWrapper"),
      fork in run := true,
      spongeGenerateForgeRun := Def.task {
        val runDir   = (baseDirectory in run).value
        val forgeJar = s"forge-${spongeMinecraftVersion.value}-${spongeForgeVersion.value}-universal.jar"

        if (!(runDir / forgeJar).exists()) {
          runForgeInstaller(
            runDir,
            (javaOptions in ForgeInstall).value,
            (managedClasspath in ForgeInstall).value.map(_.data),
            streams.value.log
          )
        }

        Seq(runDir / forgeJar).classpath
      }.value,
      baseDirectory in run := file("runForge"),
      spongeFullVersion := currentSpongeForgeVersion(spongeApiVersion.value, spongeForgeVersion.value),
      libraryDependencies += "org.spongepowered" % "spongeforge" % spongeFullVersion.value classifier "dev",
      resolvers += "Forge" at "https://files.minecraftforge.net/maven",
      libraryDependencies += {
        val minecraftVersion = (spongeMinecraftVersion in SpongeForge).value
        val fullForgeVersion = (spongeForgeVersion in SpongeForge).value

        "net.minecraftforge" % "forge" % s"$minecraftVersion-$fullForgeVersion" classifier "installer"
      }
    )
  }

  lazy val forgeInstallSettings: Seq[Setting[_]] = inConfig(ForgeInstall) {
    Seq(managedClasspath := Classpaths.managedJars(ForgeInstall, (classpathTypes in ForgeInstall).value, update.value))
  }

  override def projectSettings: Seq[Setting[_]] = spongeSettings ++ oreSettings ++ spongeVanillaSettings ++ spongeForgeSettings ++ forgeInstallSettings

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
      .addQueryParameter("version", spongeVersion)
      .addQueryParameter("minecraft", minecraftVersion)
      .build()

    findVersion(url)
  }

  def currentSpongeForgeVersion(spongeApiVersion: String, fullForgeVersion: String): String = {
    val spongeVersion = removeSnapshot(spongeApiVersion)
    val bleeding      = spongeApiVersion != spongeVersion

    val url = downloadApiUrl("spongeforge")
      .addQueryParameter("type", if (bleeding) "bleeding" else "stable")
      .addQueryParameter("version", spongeVersion)
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
          case Success(_) => throw new IOException("Unexpected response from Sponge download API")
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

  def runForgeInstaller(installDir: File, javaOptions: Seq[String], classpath: Seq[File], log: Logger): Unit = {
    require(classpath.nonEmpty, "Can't run the Forge installer with an empty classpath")

    val options = javaOptions ++ Seq(
      "java",
      "-cp",
      Path.makeString(classpath),
      "net.minecraftforge.installer.SimpleInstaller",
      "-installServer"
    )
    log.info(options.mkString(" "))

    val exitCode = Process(options, installDir) ! log
    if (exitCode != 0) sys.error("Error while generating forge installation")
  }
}
