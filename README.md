# sbt-spongyinfo

sbt-spongyinfo is a sbt plugin that makes making plugins for Sponge easier and less tedious, saving you time wondering why your plugin isn't being detected correctly.

To add sbt-spongyinfo to your project, add this to your `plugins.sbt`

```scala
resolvers += "SpongePowered" at "https://repo-new.spongepowered.org/repository/maven-public"
addSbtPlugin("net.katsstuff" % "sbt-spongyinfo" % "2.0.0-SNAPSHOT")
```

## Features
* Creates a dependency on on SpongeAPI automatically
* Allows you to easily change the API version for a project
* Automatically generates the `mcmod.info` file for you, based on information like the project id, name, version and so on.
* Deploy your plugin to Ore from SBT
* Cross sponge versions

## Usage

To just roll with the default settings, simply enable the plugin for the project, like so `enablePlugins(SpongePlugin)`.

To set the Sponge version, use the key named `spongeApiVersion`. For example, to set the Sponge to 7.1.0, use `spongeApiVersion := "7.0.0"`. The default is currently `7.0.0`.

sbt-spongyinfo will try to use information it can find about the project to generate a sensible `mcmod.info` or `plugins.json` file. Given this project here:

```scala
lazy val mymod = (project in file(".")).enablePlugins(SpongePlugin).settings(
  scalaVersion := "2.13.3",
  name := "MyPlugin",
  version := "1.0",
  description := "An example project to show of sbt-spongyinfo",
  homepage := Some(url("http://mywebsite.net"))
)
```

it will generate this `mcmod.info` file:

```json
[
    {
        "modid": "mymod",
        "name": "MyMod",
        "version": "1.0",
        "description": "An example project to show of sbt-spongyinfo",
        "url": "http://mywebsite.net"
    }
]

```

To set the information used to create the `mcmod.info`, use the key named `spongeV7.pluginInfo` or `spongeV8.pluginInfo`. Example:
```scala
spongeV7.PluginInfo := spongeV7.PluginInfo(
  id               = "myplugin",
  name             = Some("MyPlugin"),
  version          = Some("9.9.999"),
  description      = Some("My special plugin"),
  url              = Some("mywebsite.net"),
  authors          = Seq("Katrix"),
  dependencies     = Set(
    spongeV7.Dependency(
      loadOrder = spongeV7.LoadOrder.None, 
      id = "myotherplugin", 
      version = Some("1.1.0"), 
      optional = false
    )
  )
)
```
All fields besides `id` are optional.

If you don't want a `mcmod.info` file created for a project, for example if it is a shared project, you can easily specify this like this `spongeV7.metaCreate := false`. This setting defaults to true for the Sponge API version your project is currently using.

If you want to manually create a `mcmod.info` file, run `spongeV7.metaFile`.
