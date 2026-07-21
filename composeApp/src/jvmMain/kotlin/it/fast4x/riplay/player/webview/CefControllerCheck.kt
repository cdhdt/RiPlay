package it.fast4x.riplay.player.webview

import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

/**
 * Drives the real CefPlayerController end to end, without the Compose UI:
 *
 *     ./gradlew :composeApp:checkCefController -PvideoId=<id> [-PplaySeconds=30]
 *
 * Proves the controller (runtime init, hidden browser, consent, load, and state polling) plays a
 * label track and reports duration/position through its PlayerState — the contract the UI relies on.
 */
fun main(args: Array<String>): Unit = runBlocking {
    val videoId = args.firstOrNull() ?: "uhEsPlkhhXE"
    val targetMs = (System.getProperty("playSeconds")?.toLongOrNull() ?: 20L) * 1000

    println("[cef-ctl] initializing runtime ...")
    CefRuntime.ensureInitialized()
    check(CefRuntime.state.value == CefRuntime.Status.READY) { "FAILED: CEF runtime did not become ready" }

    val controller = CefPlayerController()
    controller.load(videoId)

    var elapsed = 0L
    var stalledFor = 0
    while (elapsed < targetMs && stalledFor < 40) {
        Thread.sleep(500)
        val now = controller.state.value.timestamp
        stalledFor = if (now > elapsed) 0 else stalledFor + 1
        elapsed = now
    }

    val s = controller.state.value
    println("[cef-ctl] timestamp=${elapsed}ms duration=${s.duration}ms isPlaying=${s.isPlaying}")
    controller.dispose()

    if (elapsed >= targetMs) {
        println("OK: CefPlayerController plays $videoId through its PlayerState.")
        exitProcess(0)
    } else {
        println("FAILED: playback stalled at ${elapsed}ms, expected ${targetMs}ms.")
        exitProcess(1)
    }
}
