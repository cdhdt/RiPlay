import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
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

// Définie dans gradle.properties, partagée avec androidApp pour que desktop et Android
// n'affichent jamais deux numéros différents. Surchargeable par -PriplayVersion=<x.y.z>.
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

// ponytail: les tâches KSP ne sont pas des KotlinCompile mais lisent le même répertoire généré.
// Sans cette ligne Gradle refuse le build (dépendance implicite non déclarée entre tâches).
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

        // ponytail: était "0.0.1" en dur alors qu'Android en est à 0.7.85. Source unique dans
        // gradle.properties, surchargeable en CI par -PriplayVersion=<x.y.z> pour les tags.
        version = riplayVersion
        group = "riplay"

        nativeDistributions {
            vendor = "RiPlay"
            description = "RiPlay Desktop Music Player"

            targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "RiPlay"
            packageVersion = riplayVersion

            // ponytail: bloc réactivé. Il était commenté et pointait vers un répertoire absent —
            // le décommenter tel quel cassait le build. Les icônes sont maintenant versionnées,
            // générées depuis assets/design/latest/app_icon.svg (voir desktop-icons/README.md).
            // macOS volontairement omis : pas de cible mac, donc pas de .icns à maintenir.
            val iconsRoot = project.file("desktop-icons")
            windows {
                iconFile.set(iconsRoot.resolve("icon-windows.ico"))
            }
            linux {
                iconFile.set(iconsRoot.resolve("icon-linux.png"))
                // Sans ça le .desktop sort avec Categories=Unknown et l'app n'atterrit
                // dans aucune section du menu d'applications.
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

            // ponytail: était "win" en dur, donc rien ne buildait hors Windows.
            // Classifier JavaFX déduit de l'OS qui build. Pour cross-compiler, forcer -PfxSuffix=win|linux|mac.
            val os = System.getProperty("os.name").lowercase()
            val fxSuffix = (project.findProperty("fxSuffix") as String?) ?: when {
                os.contains("win") -> "win"
                os.contains("mac") -> "mac"
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

        // ponytail: remplace le bloc commenté "uncomment for desktop / comment for android".
        // Scopé aux configurations du target jvm au lieu de commonMainApi : plus besoin de
        // commenter/décommenter entre un build desktop et un build android.
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

// ponytail: vérifie la résolution d'URL de flux sans lancer l'UI.
//   ./gradlew :composeApp:checkStreamUrl [-PvideoId=<id>]
tasks.register<JavaExec>("checkStreamUrl") {
    group = "verification"
    description = "Résout une URL de flux audio YouTube et vérifie qu'elle est réellement jouable"
    val jvmMain = kotlin.jvm().compilations.getByName("main")
    classpath = files(jvmMain.output.allOutputs, jvmMain.runtimeDependencyFiles)
    mainClass.set("it.fast4x.riplay.player.StreamUrlCheckKt")
    (project.findProperty("videoId") as String?)?.let { args(it) }
}

// ponytail: le bloc dependencies d'origine (juste en dessous) est commenté en entier, donc Room
// n'avait aucun compilateur KSP enregistré et ne générait rien. On ne réactive que le strict
// nécessaire, sans les flavors ni le desugaring qui sont probablement la cause du commentaire.
// Le target KMP s'appelle "jvm" : la config est donc kspJvm, pas kspDesktop qui n'existe pas.
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


