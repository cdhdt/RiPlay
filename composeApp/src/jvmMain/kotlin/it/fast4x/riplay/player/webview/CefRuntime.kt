package it.fast4x.riplay.player.webview

import dev.datlag.kcef.KCEF
import dev.datlag.kcef.KCEFClient
import it.fast4x.riplay.player.Debug
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Owns the single embedded-Chromium runtime. JCEF can only be initialized once per process, so this
 * is a process-wide singleton; the first call blocks while the ~150 MB CEF bundle downloads.
 */
object CefRuntime {

    /** null = not started, a progress 0..100 while downloading, then ready/failed via [state]. */
    enum class Status { IDLE, INITIALIZING, READY, FAILED }

    private val _state = MutableStateFlow(Status.IDLE)
    val state: StateFlow<Status> = _state.asStateFlow()

    private val _downloadPercent = MutableStateFlow(0)
    val downloadPercent: StateFlow<Int> = _downloadPercent.asStateFlow()

    @Volatile private var client: KCEFClient? = null
    private val lock = Any()

    val installDir: File = File(System.getProperty("user.home"), ".riplay/kcef")

    /** Blocking and idempotent; call it off the UI thread. Safe to call from several threads. */
    fun ensureInitialized() {
        if (client != null) return
        synchronized(lock) {
            if (client != null) return
            _state.value = Status.INITIALIZING
            runCatching {
                KCEF.initBlocking(
                    builder = {
                        installDir(installDir)
                        // Let a page start audio on its own (no user gesture reaches this hidden
                        // browser), and stop Chromium from suspending/throttling media just because
                        // the window is off-screen — otherwise the track loads but never advances.
                        args(
                            "--autoplay-policy=no-user-gesture-required",
                            "--disable-background-timer-throttling",
                            "--disable-backgrounding-occluded-windows",
                            "--disable-renderer-backgrounding",
                            "--disable-background-media-suspend",
                        )
                        progress {
                            onDownloading { _downloadPercent.value = it.toInt() }
                        }
                        settings {
                            cachePath = File(installDir, "cache").absolutePath
                            // Plain OpenJDK (not JetBrains Runtime): point the render subprocess and
                            // resources at the downloaded bundle, or JCEF looks in java.home and crashes.
                            browserSubProcessPath = File(installDir, "jcef_helper").absolutePath
                            resourcesDirPath = installDir.absolutePath
                            localesDirPath = File(installDir, "locales").absolutePath
                        }
                    },
                    onError = { it?.printStackTrace() },
                )
                client = KCEF.newClientBlocking()
                _state.value = Status.READY
                Debug.log("cef") { "runtime ready" }
            }.onFailure {
                _state.value = Status.FAILED
                println("CefRuntime: init failed — $it")
            }
        }
    }

    fun requireClient(): KCEFClient =
        client ?: error("CefRuntime not initialized; call ensureInitialized() first")
}
