import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.security.MessageDigest
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.room)
}


fun Project.propertyOrEmpty(name: String): String {
    return (findProperty(name) as? String) ?: ""
}

// Defined in gradle.properties, shared with androidApp. Override with -PriplayVersion=<x.y.z>.
val riplayVersion: String by project
val generatedSrcDir = layout.buildDirectory.dir("generated/kotlin/config").get()

val generateEnvironmentConfig by tasks.registering {
    group = "build"
    description = "Genera un file Kotlin con le configurazioni di ambiente."

    val environmentPropertyNames = listOf(
        "CrQ0JjAXgv", "hNpBzzAn7i", "lEi9YM74OL", "C0ZR993zmk", "w3TFBFL74Y", "mcchaHCWyK",
        "L2u4JNdp7L", "sqDlfmV4Mt", "WpLlatkrVv", "1zNshDpFoh", "mPVWVuCxJz", "auDsjnylCZ",
        "AW52cvJIJx", "0RGAyC1Zqu", "4Fdmu9Jkax", "kuSdQLhP8I", "QrgDKwvam1", "wLwNESpPtV",
        "JJUQaehRFg", "i7WX2bHV6R", "XpiuASubrV", "lOlIIVw38L", "mtcR0FhFEl", "DTihHAFaBR",
        "a4AcHS8CSg", "krdLqpYLxM", "ye6KGLZL7n", "ec09m20YH5", "LDRlbOvbF1", "EEqX0yizf2",
        "i3BRhLrV1v", "MApdyHLMyJ", "hizI7yLjL4", "rLoZP7BF4c", "nza34sU88C", "dwbUvjWUl3",
        "fqqhBZd0cf", "9sZKrkMg8p", "aQpNCVOe2i", "XNl2TKXLlB", "yNjbjspY8v", "eZueG672lt",
        "WkUFhXtC3G", "z4Xe47r8Vs", "AudioTagInfo_API_KEY", "RiPlay_LASTFM_API_KEY",
        "RiPlay_LASTFM_SECRET", "RiPlay_DISCORD_APPLICATION_ID", "RiPlay_CHROMECAST_APPLICATION_ID"
    )
    inputs.properties(environmentPropertyNames.associateWith { propertyOrEmpty(it) })

    val outputFile = generatedSrcDir.file("it/fast4x/riplay/config/EnvironmentConfig.kt")
    outputs.file(outputFile)

    doLast {
        val file = outputFile.asFile
        file.parentFile.mkdirs()

        val props = inputs.properties

        file.writeText(
            """
            // GENERATED FILE - DO NOT MODIFY
            package it.fast4x.riplay.config

            object EnvironmentConfig {
                const val env_CrQ0JjAXgv = "${props["CrQ0JjAXgv"]}"
                const val env_hNpBzzAn7i = "${props["hNpBzzAn7i"]}"
                const val env_lEi9YM74OL = "${props["lEi9YM74OL"]}"
                const val env_C0ZR993zmk = "${props["C0ZR993zmk"]}"
                const val env_w3TFBFL74Y = "${props["w3TFBFL74Y"]}"
                const val env_mcchaHCWyK = "${props["mcchaHCWyK"]}"
                const val env_L2u4JNdp7L = "${props["L2u4JNdp7L"]}"
                const val env_sqDlfmV4Mt = "${props["sqDlfmV4Mt"]}"
                const val env_WpLlatkrVv = "${props["WpLlatkrVv"]}"
                const val env_1zNshDpFoh = "${props["1zNshDpFoh"]}"
                const val env_mPVWVuCxJz = "${props["mPVWVuCxJz"]}"
                const val env_auDsjnylCZ = "${props["auDsjnylCZ"]}"
                const val env_AW52cvJIJx = "${props["AW52cvJIJx"]}"
                const val env_0RGAyC1Zqu = "${props["0RGAyC1Zqu"]}"
                const val env_4Fdmu9Jkax = "${props["4Fdmu9Jkax"]}"
                const val env_kuSdQLhP8I = "${props["kuSdQLhP8I"]}"
                const val env_QrgDKwvam1 = "${props["QrgDKwvam1"]}"
                const val env_wLwNESpPtV = "${props["wLwNESpPtV"]}"
                const val env_JJUQaehRFg = "${props["JJUQaehRFg"]}"
                const val env_i7WX2bHV6R = "${props["i7WX2bHV6R"]}"
                const val env_XpiuASubrV = "${props["XpiuASubrV"]}"
                const val env_lOlIIVw38L = "${props["lOlIIVw38L"]}"
                const val env_mtcR0FhFEl = "${props["mtcR0FhFEl"]}"
                const val env_DTihHAFaBR = "${props["DTihHAFaBR"]}"
                const val env_a4AcHS8CSg = "${props["a4AcHS8CSg"]}"
                const val env_krdLqpYLxM = "${props["krdLqpYLxM"]}"
                const val env_ye6KGLZL7n = "${props["ye6KGLZL7n"]}"
                const val env_ec09m20YH5 = "${props["ec09m20YH5"]}"
                const val env_LDRlbOvbF1 = "${props["LDRlbOvbF1"]}"
                const val env_EEqX0yizf2 = "${props["EEqX0yizf2"]}"
                const val env_i3BRhLrV1v = "${props["i3BRhLrV1v"]}"
                const val env_MApdyHLMyJ = "${props["MApdyHLMyJ"]}"
                const val env_hizI7yLjL4 = "${props["hizI7yLjL4"]}"
                const val env_rLoZP7BF4c = "${props["rLoZP7BF4c"]}"
                const val env_nza34sU88C = "${props["nza34sU88C"]}"
                const val env_dwbUvjWUl3 = "${props["dwbUvjWUl3"]}"
                const val env_fqqhBZd0cf = "${props["fqqhBZd0cf"]}"
                const val env_9sZKrkMg8p = "${props["9sZKrkMg8p"]}"
                const val env_aQpNCVOe2i = "${props["aQpNCVOe2i"]}"
                const val env_XNl2TKXLlB = "" //"${props["XNl2TKXLlB"]}"
                const val env_yNjbjspY8v = "" //"${props["yNjbjspY8v"]}"
                const val env_eZueG672lt = "${props["eZueG672lt"]}"
                const val env_WkUFhXtC3G = "${props["WkUFhXtC3G"]}"
                const val env_z4Xe47r8Vs = "${props["z4Xe47r8Vs"]}"
                const val AudioTagInfo_API_KEY = "${props["AudioTagInfo_API_KEY"]}"
                const val RiPlay_LASTFM_API_KEY = "${props["RiPlay_LASTFM_API_KEY"]}"
                const val RiPlay_LASTFM_SECRET = "${props["RiPlay_LASTFM_SECRET"]}"
                const val RiPlay_DISCORD_APPLICATION_ID = "${props["RiPlay_DISCORD_APPLICATION_ID"]}"
                const val RiPlay_CHROMECAST_APPLICATION_ID = "${props["RiPlay_CHROMECAST_APPLICATION_ID"]}"
            }
            """.trimIndent()
        )
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateEnvironmentConfig)
}

// KSP tasks are not KotlinCompile but read the same generated directory, and Gradle rejects the
// build over the undeclared dependency.
tasks.matching { it.name.startsWith("ksp") }.configureEach {
    dependsOn(generateEnvironmentConfig)
}

/*
repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}
 */

android {
    namespace = "it.fast4x.riplay.composeapp"
    compileSdk = 37
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

compose.desktop {
    application {

        mainClass = "MainKt"

        version = riplayVersion
        group = "riplay"

        nativeDistributions {
            vendor = "RiPlay"
            description = "RiPlay Desktop Music Player"

            targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "RiPlay"
            packageVersion = riplayVersion

            // Per-platform subdirectories; only windows-x64 is populated, by stageWindowsVlc.
            appResourcesRootDir.set(layout.buildDirectory.dir("vlc-resources"))

            // jpackage needs raster icons; see desktop-icons/README.md to regenerate them.
            // No macOS entry: not a build target, so no .icns to keep around.
            val iconsRoot = project.file("desktop-icons")
            windows {
                iconFile.set(iconsRoot.resolve("icon-windows.ico"))
            }
            linux {
                iconFile.set(iconsRoot.resolve("icon-linux.png"))
                // Without this the .desktop entry gets Categories=Unknown.
                menuGroup = "Audio"
            }
        }

    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
}

kotlin {
    applyDefaultHierarchyTemplate()
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xcontext-parameters")
        }
    }

    jvm()




    sourceSets {
        all {
            languageSettings {
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
            }
        }

        val commonMain by getting {
            kotlin.srcDir(generatedSrcDir)

            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.ktor.client.websockets)

                implementation(projects.environment)
                implementation(projects.kugou)
                implementation(projects.lrclib)
                implementation(projects.audiotaginfo)
                implementation(projects.lastfm)

                implementation(libs.room.runtime)
                implementation(libs.room.sqlite.bundled)

                implementation(libs.mediaplayer.kmp)

                implementation(libs.navigation.kmp)

                //coil3 mp
                implementation(libs.coil.compose.core)
                implementation(libs.coil.compose)
                implementation(libs.coil.mp)

                implementation(libs.translator)
                implementation(libs.reorderable)

                implementation(libs.fastscroller)
                implementation(libs.fastscroller.material3)
                implementation(libs.fastscroller.indicator)

            }
        }

        val jvmMain by getting
        jvmMain.dependencies {
            implementation(compose.components.resources)
            implementation(compose.desktop.currentOs)

            implementation(libs.material.icon.desktop)
            implementation(libs.vlcj)

            // JavaFX ships per-platform artifacts and was hardcoded to "win". Apple Silicon needs
            // mac-aarch64, plain "mac" pulls Intel binaries. Cross-compile with -PfxSuffix=<suffix>.
            val os = System.getProperty("os.name").lowercase()
            val arch = System.getProperty("os.arch").lowercase()
            val fxSuffix = (project.findProperty("fxSuffix") as String?) ?: when {
                os.contains("win") -> "win"
                os.contains("mac") -> if (arch == "aarch64") "mac-aarch64" else "mac"
                arch == "aarch64" -> "linux-aarch64"
                else -> "linux"
            }
            implementation("org.openjfx:javafx-base:21.0.5:${fxSuffix}")
            implementation("org.openjfx:javafx-graphics:21.0.5:${fxSuffix}")
            implementation("org.openjfx:javafx-controls:21.0.5:${fxSuffix}")
            implementation("org.openjfx:javafx-swing:21.0.5:${fxSuffix}")
            implementation("org.openjfx:javafx-web:21.0.5:${fxSuffix}")
            implementation("org.openjfx:javafx-media:21.0.5:${fxSuffix}")

            implementation(libs.coil.network.okhttp)
            runtimeOnly(libs.kotlinx.coroutines.swing)

        }

        // Scoped to the jvm target rather than commonMainApi, so desktop and android builds no
        // longer need this block commented in and out.
        listOf("jvmCompileClasspath", "jvmRuntimeClasspath").forEach { name ->
            configurations.named(name) {
                exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
            }
        }

        androidMain.dependencies {
            implementation(libs.room.ktx)
            implementation(
                fileTree(
                    mapOf(
                        "dir" to "libs",
                        "include" to listOf("*.aar", "*.jar")
                    )
                )
            )
            /*
            implementation(libs.navigation)
            implementation(libs.media3.session)
            //implementation(libs.media3.ui)
            implementation(libs.kotlin.coroutines.guava)
            implementation(libs.kotlin.concurrent.futures)
            implementation(libs.androidx.webkit)
            //implementation(libs.room.backup)
            implementation(libs.workmanager)
            implementation(libs.accompanist)

            implementation(libs.compose.activity)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.util)
            implementation(libs.compose.ripple)
            implementation(libs.compose.shimmer)
            implementation(libs.compose.coil)
            implementation(libs.palette)
            implementation(libs.media3.exoplayer)
            implementation(libs.media3.datasource.okhttp)
            implementation(libs.appcompat)
            implementation(libs.appcompat.resources)
            implementation(libs.support)
            implementation(libs.media)
            implementation(libs.material)
            implementation(libs.material3)
            implementation(libs.compose.ui.graphics.android)
            implementation(libs.constraintlayout)
            implementation(libs.compose.runtime.livedata)
            implementation(libs.compose.animation)
            implementation(libs.kotlin.csv)
            implementation(libs.monetcompat)
            implementation(libs.androidmaterial)
            implementation(libs.timber)
            implementation(libs.crypto)
            implementation(libs.logging.interceptor)
            implementation(libs.math3)
            implementation(libs.toasty)
            implementation(libs.haze)
            //implementation(libs.androidyoutubeplayer) // replaced by project ayp
            //implementation(libs.androidyoutubeplayer.custom.ui) // replaced by project aypui
            implementation(project(":ayp"))
            implementation(project(":aypui"))
            implementation(libs.glance.widgets)
            implementation(libs.kizzy.rpc)
            implementation(libs.gson)
            implementation(libs.hypnoticcanvas)
            implementation(libs.hypnoticcanvas.shaders)
            implementation(libs.multidex)
            implementation(libs.jsoup)
            //implementation(libs.mediarouter)

             */
        }

    }
}


room {
    schemaDirectory("$projectDir/schemas")
    generateKotlin = true
}

// VLC ships a self-contained build for Windows, so the package can carry its own copy and play
// out of the box. Linux is left to the system install on purpose: its plugins link against ~140
// system libraries, and shipping that closure is what AppImage and Flatpak are for.
val vlcWindowsVersion = "3.0.21"
// Published alongside the archive as <name>.sha256. Pinned because this binary goes straight into
// an installer we hand to users, so a corrupted or swapped download has to fail the build loudly.
val vlcWindowsSha256 = "a0b7ec02b50adf6417eed014fb8df50af39690505a4225b85b3dc2ed17d14843"
val hostIsWindows = System.getProperty("os.name").lowercase().contains("win")

val prepareVlcResources by tasks.registering {
    description = "Stages the bundled VLC runtime for platforms that need one"
    outputs.dir(layout.buildDirectory.dir("vlc-resources"))

    doLast {
        val root = layout.buildDirectory.dir("vlc-resources").get().asFile
        root.mkdirs()

        // Only Windows packages carry VLC. On Linux the directory stays empty, which is what
        // appResourcesRootDir expects, and VlcNative then falls back to the system install.
        // -PforceVlcStaging exercises the download from a non-Windows machine, so the fetch and
        // checksum can be checked without spending a CI round trip.
        if (!hostIsWindows && !project.hasProperty("forceVlcStaging")) return@doLast

        val target = File(root, "windows-x64/vlc")
        if (File(target, "libvlc.dll").exists()) return@doLast
        target.mkdirs()

        val archive = File(temporaryDir, "vlc-$vlcWindowsVersion-win64.zip")
        if (!archive.exists()) {
            // Not get.videolan.org: it 302s to a randomly picked mirror, and a failing one answers
            // with an HTML error page that only surfaces later as "Cannot expand ZIP".
            val url = "https://download.videolan.org/pub/videolan/vlc/$vlcWindowsVersion/win64/" +
                    "vlc-$vlcWindowsVersion-win64.zip"
            logger.lifecycle("Downloading $url")
            uri(url).toURL().openStream().use { input ->
                archive.outputStream().use { output -> input.copyTo(output) }
            }
        }

        val digest = MessageDigest.getInstance("SHA-256")
        archive.inputStream().use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actualSha256 = digest.digest()
            .joinToString("") { byte -> String.format("%02x", byte) }
        if (actualSha256 != vlcWindowsSha256) {
            archive.delete()
            error(
                "VLC archive checksum mismatch: expected $vlcWindowsSha256, got $actualSha256. " +
                        "The download was truncated or served by a mirror we do not trust."
            )
        }

        // The archive also holds the VLC desktop app, which would be dead weight in the package.
        val prefix = "vlc-$vlcWindowsVersion/"
        val runtimeDlls = setOf("libvlc.dll", "libvlccore.dll")
        // 19 MB of VLC's own Qt interface, 17 of them one DLL. libvlc never loads it: we drive it
        // as a library and never ask for an interface. The other plugin folders stay — codec,
        // access and demux are what actually decode the stream.
        val unusedPlugins = "plugins/gui/"
        zipTree(archive).visit {
            if (isDirectory) return@visit
            val relative = path.removePrefix(prefix)
            val keep = (relative.startsWith("plugins/") && !relative.startsWith(unusedPlugins)) ||
                    name in runtimeDlls
            if (relative == path || !keep) return@visit
            val destination = File(target, relative)
            destination.parentFile.mkdirs()
            file.copyTo(destination, overwrite = true)
        }

        require(File(target, "libvlc.dll").exists()) { "libvlc.dll missing from the VLC archive" }
        logger.lifecycle("Staged VLC into $target")
    }
}

// prepareAppResources is the Compose task that reads appResourcesRootDir, so it is the one that
// has to wait; the packaging tasks all funnel through it.
tasks.matching { it.name == "prepareAppResources" }
    .configureEach { dependsOn(prepareVlcResources) }

// ./gradlew :composeApp:checkStreamUrl [-PvideoId=<id>]
// ./gradlew :composeApp:checkPlayback  [-PvideoId=<id>]
listOf(
    "checkStreamUrl" to "it.fast4x.riplay.player.StreamUrlCheckKt",
    "checkPlayback" to "it.fast4x.riplay.player.PlaybackCheckKt",
    "checkAppPath" to "it.fast4x.riplay.player.AppPathCheckKt",
).forEach { (taskName, entryPoint) ->
    tasks.register<JavaExec>(taskName) {
        group = "verification"
        description = "Checks the YouTube to VLC playback chain end to end"
        val jvmMain = kotlin.jvm().compilations.getByName("main")
        classpath = files(jvmMain.output.allOutputs, jvmMain.runtimeDependencyFiles)
        mainClass.set(entryPoint)
        (project.findProperty("videoId") as String?)?.let { args(it) }
        // Lets the checks exercise the bundled-VLC path without building a package.
        (project.findProperty("resourcesDir") as String?)
            ?.let { systemProperty("compose.application.resources.dir", it) }
        (project.findProperty("playSeconds") as String?)
            ?.let { systemProperty("playSeconds", it) }
        if (project.hasProperty("preferIPv4")) jvmArgs("-Djava.net.preferIPv4Stack=true")
    }
}

// The dependencies block below is commented out in full, leaving Room without a KSP compiler.
// Only the necessary part is restored here. The KMP target is named "jvm", so the configuration
// is kspJvm — kspDesktop, as the original block had it, matches nothing.
dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
}

/*
dependencies {

    "fullImplementation"(libs.media3.ui)
    "fullImplementation"(libs.media3.cast)
    "fullImplementation"(project(":aypcast"))

    add("kspAndroid", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)

    coreLibraryDesugaring(libs.desugaring)
}

 */


