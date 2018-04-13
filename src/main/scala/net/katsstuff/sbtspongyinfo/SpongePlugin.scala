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

import org.spongepowered.plugin.meta.McModInfo

import com.typesafe.sbt.SbtPgp

import okhttp3.{MultipartBody, OkHttpClient, Request, RequestBody, Response}
import sbt.Keys._
import sbt.{Def, _}
import sbtassembly.AssemblyPlugin

object SpongePlugin extends AutoPlugin {

  override def requires = plugins.JvmPlugin && SbtPgp && AssemblyPlugin
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
      if (spongeMetaCreate.value) {
        Seq(generateMcModInfo((resourceManaged in Compile).value / "mcmod.info", spongePluginInfo.value))
      } else Seq()
    }.taskValue,
    resolvers += SpongeRepo,
    libraryDependencies += "org.spongepowered" % "spongeapi" % spongeApiVersion.value % Provided,
    oreUrl := "https://ore.spongepowered.org",
    oreRecommended := true,
    oreChannel := "Release",
    oreCreateForumPost := None,
    oreChangelog := None,
    oreDeploymentKey := None,
    oreDeploy := Some(signFatjar.value),
  )

  override def projectSettings: Seq[Setting[_]] = baseSettings

  def generateMcModInfo(file: File, plugin: PluginInfo): File = {
    file.getParentFile.mkdirs()
    McModInfo.DEFAULT.write(file.toPath, plugin.toSponge)
    file
  }

  val signFatjar = Def.taskDyn {
    val fatjar       = AssemblyPlugin.autoImport.assembly.value
    val r            = SbtPgp.autoImport.PgpKeys.pgpSigner.value
    val s            = streams.value
    val gpgExtension = com.typesafe.sbt.pgp.gpgExtension
    val signature    = r.sign(fatjar, new File(fatjar.getAbsolutePath + gpgExtension), s)
    deploy(fatjar, signature, s)
  }

  def deploy(jar: File, signature: File, s: TaskStreams): Def.Initialize[Task[(sbt.File, sbt.File)]] =
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

      val client = new OkHttpClient()
      var response: Response = null
      try {
        s.log.info(s"Deploying ${jar.name} to $projectUrl")
        response = client.newCall(request).execute()
        val status  = response.code()
        val created = status == 201
        if (!created) {
          val body = response.body().string()
          throw new IOException(s"$status Could not deploy plugin. ${response.message()}\n$body")
        }
        s.log.debug(response.body().string())
        s.log.info(s"Successfully deployed ${jar.name} to Ore")
      } finally {
        if (response != null) {
          response.close()
        }
      }

      (jar, signature)
    } tag (Tags.Publish, Tags.Network)
}
