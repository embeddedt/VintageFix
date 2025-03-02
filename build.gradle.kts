import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar
import org.jetbrains.gradle.ext.Application
import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.RunConfigurationContainer

plugins {
  id("java-library")
  id("maven-publish")
  id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.7"
  id("com.gtnewhorizons.retrofuturagradle") version "1.4.1"
  id("com.github.johnrengelman.shadow") version "8.1.0"
  id("eclipse")
  id("me.modmuss50.mod-publish-plugin") version "0.7.3"
}

// Project properties
group = "org.embeddedt.vintagefix"

if (project.hasProperty("build.release")) {
  version = project.properties["mod_version"].toString()
} else if(project.hasProperty("build.ci")) {
  version = project.properties["mod_version"].toString() + "-ci." + project.properties["build.ci"].toString()
} else {
  version = project.properties["mod_version"].toString() + "-local"
}

// Set the toolchain version to decouple the Java we run Gradle with from the Java used to compile and run the mod
java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
    // Azul covers the most platforms for Java 8 toolchains, crucially including MacOS arm64
    vendor.set(org.gradle.jvm.toolchain.JvmVendorSpec.AZUL)
  }
  // Generate sources and javadocs jars when building and publishing
  // withSourcesJar()
}

// Most RFG configuration lives here, see the JavaDoc for com.gtnewhorizons.retrofuturagradle.MinecraftExtension
minecraft {
  mcVersion.set("1.12.2")

  // Username for client run configurations
  username.set("Developer")

  // Generate a field named VERSION with the mod version in the injected Tags class
  injectedTags.put("VERSION", project.version)

  // If you need the old replaceIn mechanism, prefer the injectTags task because it doesn't inject a javac plugin.
  // tagReplacementFiles.add("RfgExampleMod.java")

  // Enable assertions in the mod's package when running the client or server
  extraRunJvmArguments.add("-ea:${project.group}")
  extraRunJvmArguments.addAll("-Xmx1024m", "-Xms1024m")

  // If needed, add extra tweaker classes like for mixins.
  // extraTweakClasses.add("org.spongepowered.asm.launch.MixinTweaker")

  // Exclude some Maven dependency groups from being automatically included in the reobfuscated runs
  groupsToExcludeFromAutoReobfMapping.addAll("com.diffplug", "com.diffplug.durian", "net.industrial-craft")
}

// Generates a class named rfg.examplemod.Tags with the mod version in it, you can find it at
tasks.injectTags.configure {
  outputClassName.set("${project.group}.Tags")
}

// Put the version from gradle into mcmod.info
tasks.processResources.configure {
  inputs.property("version", version)

  filesMatching("mcmod.info") {
    expand(mapOf("modVersion" to inputs.properties["version"]))
  }
}

// Create a new dependency type for runtime-only dependencies that don't get included in the maven publication
val runtimeOnlyNonPublishable: Configuration by configurations.creating {
  description = "Runtime only dependencies that are not published alongside the jar"
  isCanBeConsumed = false
  isCanBeResolved = false
}
listOf(configurations.runtimeClasspath, configurations.testRuntimeClasspath).forEach {
  it.configure {
    extendsFrom(
      runtimeOnlyNonPublishable
    )
  }
}

val shadow = configurations.getByName("shadow")

listOf(configurations.implementation).forEach {
  it.configure {
    extendsFrom(shadow)
  }
}

// Add an access tranformer
// tasks.deobfuscateMergedJarToSrg.configure {accessTransformerFiles.from("src/main/resources/META-INF/mymod_at.cfg")}

// Dependencies
repositories {
  maven { url = uri("https://repo.spongepowered.org/repository/maven-public") }
  maven {
    url = uri("https://maven.cleanroommc.com")
  }
  maven {
    url = uri("https://modmaven.dev/")
  }
  maven {
    url = uri("https://cursemaven.com")
  }
  maven {
    url = uri("https://maven.tterrag.com/")
  }
}

dependencies {
  // Adds NotEnoughItems and its dependencies (CCL&CCC) to runClient/runServer
  // runtimeOnlyNonPublishable("com.github.GTNewHorizons:NotEnoughItems:2.3.39-GTNH:dev")
  // Example: grab the ic2 jar from curse maven and deobfuscate
  // api(rfg.deobf("curse.maven:ic2-242638:2353971"))
  // Example: grab the ic2 jar from libs/ in the workspace and deobfuscate
  // api(rfg.deobf(project.files("libs/ic2.jar")))
  implementation("zone.rong:mixinbooter:10.5")
  annotationProcessor("org.ow2.asm:asm-debug-all:5.2")
  annotationProcessor("com.google.guava:guava:24.1.1-jre")
  annotationProcessor("com.google.code.gson:gson:2.8.6")
  annotationProcessor("zone.rong:mixinbooter:10.5") {isTransitive = false}

  compileOnly(rfg.deobf("slimeknights.mantle:Mantle:1.12-1.3.3.56"))
  compileOnly(rfg.deobf("slimeknights:TConstruct:1.12.2-2.13.0.180"))
  compileOnly(rfg.deobf("curse.maven:applied-energistics-2-223794:2747063"))
  compileOnly(rfg.deobf("curse.maven:immersive-engineering-231951:2974106"))
  compileOnly(rfg.deobf("curse.maven:mysticallib-277064:3483816"))
  compileOnly(rfg.deobf("curse.maven:blockcraftery-278882:2716712"))
  compileOnly(rfg.deobf("team.chisel.ctm:CTM:MC1.12.2-1.0.2.31"))
  compileOnly(rfg.deobf("curse.maven:base-246996:3440963"))
  compileOnly(rfg.deobf("curse.maven:rloader-226447:2477566"))
  // server build, put client build in run/mods
  compileOnly(rfg.deobf("curse.maven:betweenlands-243363:4479692"))
  //implementation(rfg.deobf("team.chisel:Chisel:MC1.12.2-1.0.1.44"))
  //implementation(rfg.deobf("curse.maven:codechicken-lib-1-8-242818:2779848"))
  //implementation(rfg.deobf("curse.maven:avaritia-261348:3143349"))
  compileOnly("org.reflections:reflections:0.9.10")
  compileOnly(rfg.deobf("curse.maven:unlimited-chisel-works-278493:3319307"))
  compileOnly(rfg.deobf("curse.maven:hammercore-247401:3611193"))
  compileOnly(rfg.deobf("curse.maven:refinedstorage-243076:2940914"))
  compileOnly(rfg.deobf("curse.maven:opencomputers-223008:4566834"))
  compileOnly(rfg.deobf("curse.maven:extrautils-225561:2678374"))
  compileOnly(rfg.deobf("curse.maven:chiselsandbits-231095:2720655"))
  compileOnly(rfg.deobf("curse.maven:projectredexpansion-229048:2745550"))
  shadow("com.esotericsoftware:kryo:5.1.1")
  val mixinExtras = "io.github.llamalad7:mixinextras-common:0.3.6"
  shadow(mixinExtras)
  annotationProcessor(mixinExtras)
}

val main by sourceSets.getting // created by ForgeGradle
sourceSets.register("googleaccess") {
  compileClasspath += main.compileClasspath
}
val googleaccess by sourceSets.getting // created by ForgeGradle
sourceSets.register("googleimpl") {
  compileClasspath += googleaccess.output
  compileClasspath += main.output
  compileClasspath += main.compileClasspath
}
val googleimpl by sourceSets.getting
main.runtimeClasspath += googleimpl.output

val mixinConfigRefMap = "mixins.vintagefix.refmap.json"
val mixinTmpDir = buildDir.path + File.separator + "tmp" + File.separator + "mixins"
val refMap = mixinTmpDir + File.separator + mixinConfigRefMap
val mixinSrg = mixinTmpDir + File.separator + "mixins.srg"

tasks.withType<ShadowJar> {
  configurations = listOf(shadow)
  relocate("com.llamalad7.mixinextras", "org.embeddedt.vintagefix.mixinextras")
  mergeServiceFiles() // Very important!
  archiveClassifier = "deobf"
}

tasks.named<ReobfuscatedJar>("reobfJar").configure {
  extraSrgFiles.from(mixinSrg)
  inputJar.set(tasks.named<ShadowJar>("shadowJar").map { it.archiveFile }.get())
}

tasks.named<JavaCompile>("compileJava").configure {
  inputs.property("reobfSrgFile", tasks.reobfJar.get().srg.get().asFile)
  inputs.property("mixinTmpDir", mixinTmpDir)
  doFirst {
    File(inputs.properties["mixinTmpDir"].toString()).mkdirs()
  }
  options.compilerArgs.addAll(listOf(
    "-AreobfSrgFile=${inputs.properties["reobfSrgFile"]}",
    "-AoutSrgFile=${mixinSrg}",
    "-AoutRefMapFile=${refMap}",
  ))
}

tasks.processResources {
  from(refMap)
  dependsOn(tasks.compileJava)
}

tasks.deobfuscateMergedJarToSrg.configure {accessTransformerFiles.from("src/main/resources/META-INF/vintagefix_at.cfg")}

tasks.named<Jar>("jar") {
  manifest {
    attributes(
      "FMLCorePlugin" to ("org.embeddedt.vintagefix.core.VintageFixCore"),
      "FMLCorePluginContainsFMLMod" to "true",
      "FMLAT" to "vintagefix_at.cfg",
      "ForceLoadAsMod" to "true"
    )
  }
}

listOf("jar", "shadowJar").forEach {
  tasks.named<Jar>(it) {
    into("googleaccess") {
      from(googleaccess.output)
      rename { filename ->
        // Add suffix to stop parts of the toolchain from moving these classes to the "correct" package
        filename + "_manual"
      }
    }
    from(googleimpl.output)
  }
}

val copyJarToBin = tasks.register<Copy>("copyJarToBin") {
  from(tasks.reobfJar)
  into(rootProject.file("out"))
  rename { name -> "vintagefix.jar" }
}

tasks.build {
  dependsOn(copyJarToBin)
}


// Publishing to a Maven repository
publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])
    }
  }
  repositories {
    // Example: publishing to the GTNH Maven repository
    maven {
      url = uri("http://jenkins.usrv.eu:8081/nexus/content/repositories/releases")
      isAllowInsecureProtocol = true
      credentials {
        username = System.getenv("MAVEN_USER") ?: "NONE"
        password = System.getenv("MAVEN_PASSWORD") ?: "NONE"
      }
    }
  }
}

// IDE Settings
eclipse {
  classpath {
    isDownloadSources = true
    isDownloadJavadoc = true
  }
}

idea {
  module {
    isDownloadJavadoc = true
    isDownloadSources = true
    inheritOutputDirs = true // Fix resources in IJ-Native runs
  }
  project {
    this.withGroovyBuilder {
      "settings" {
        "runConfigurations" {
          val self = this.delegate as RunConfigurationContainer
          self.add(Gradle("1. Run Client").apply {
            setProperty("taskNames", listOf("runClient"))
          })
          self.add(Gradle("2. Run Server").apply {
            setProperty("taskNames", listOf("runServer"))
          })
          self.add(Gradle("3. Run Obfuscated Client").apply {
            setProperty("taskNames", listOf("runObfClient"))
          })
          self.add(Gradle("4. Run Obfuscated Server").apply {
            setProperty("taskNames", listOf("runObfServer"))
          })
        }
      }
    }
  }
}

tasks.processIdeaSettings.configure {
  dependsOn(tasks.injectTags)
}

publishMods {
  file = tasks.reobfJar.get().archiveFile
  displayName = "VintageFix ${project.version}"
  type = STABLE
  modLoaders.add("forge")
  changelog = file("CHANGELOG.md").readText()

  curseforge {
    projectId = "871198"
    projectSlug = "vintagefix" // Required for discord webhook
    accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
    minecraftVersions.add("1.12.2")
    requires("mixin-booter")
  }
  modrinth {
    projectId = "e6vNsbAm"
    accessToken = providers.environmentVariable("MODRINTH_TOKEN")
    minecraftVersions.add("1.12.2")
  }
}
