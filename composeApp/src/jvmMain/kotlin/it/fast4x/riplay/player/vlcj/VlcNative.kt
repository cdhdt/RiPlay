package it.fast4x.riplay.player.vlcj

import com.sun.jna.NativeLibrary
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import java.io.File

/**
 * Locates libvlc before any media player is created.
 *
 * Windows and macOS packages carry their own copy under the app resources, because VLC ships
 * self-contained builds there. Linux deliberately relies on the system install: its plugins link
 * against ~140 system libraries, so bundling them means shipping that whole closure, which is what
 * AppImage and Flatpak are for.
 */
object VlcNative {

    /**
     * Bundled VLC directory, or null when relying on a system install. The directory has to hold
     * an actual library: an empty one means a broken package, and claiming it works there would
     * trade a clear message for an UnsatisfiedLinkError deep in JNA.
     */
    private val bundledDir: File? =
        System.getProperty("compose.application.resources.dir")
            ?.let { File(it, "vlc") }
            ?.takeIf { dir ->
                dir.isDirectory && dir.listFiles().orEmpty().any {
                    it.name.startsWith("libvlc") || it.name == "vlc.dll"
                }
            }

    /** Extra arguments the media player factory needs to find the bundled plugins. */
    val factoryArgs: Array<String>
        get() = bundledDir
            ?.resolve("plugins")
            ?.takeIf { it.isDirectory }
            ?.let { arrayOf("--plugin-path=${it.absolutePath}") }
            ?: emptyArray()

    /**
     * Returns false when no libvlc could be found, leaving the caller to report it. Idempotent:
     * every controller calls it, only the first does the work.
     */
    val available: Boolean by lazy {
        val bundled = bundledDir
        if (bundled != null) {
            NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), bundled.absolutePath)
            System.setProperty("jna.library.path", bundled.absolutePath)
            true
        } else {
            NativeDiscovery().discover()
        }
    }

    /** Which libvlc is in use, so a broken package can be told apart from a missing install. */
    fun describe(): String =
        bundledDir?.let { "bundled (${it.absolutePath})" } ?: "system install"

    /** Shown to the user, so it has to say what to do rather than just what failed. */
    fun missingMessage(): String = buildString {
        append("VLC was not found, so playback is unavailable. ")
        when {
            System.getProperty("os.name").orEmpty().lowercase().contains("win") ->
                append("Reinstall RiPlay, or install VLC from https://www.videolan.org/vlc/")
            else ->
                append("Install it with your package manager, for example: sudo apt install vlc")
        }
    }
}
