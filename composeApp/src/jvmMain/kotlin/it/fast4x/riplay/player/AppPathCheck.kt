package it.fast4x.riplay.player

import it.fast4x.riplay.commonutils.initializeEnvironment
import it.fast4x.riplay.player.vlcj.VlcjFrameController
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

/**
 * Plays through the exact controller the UI uses, rather than a bare media player:
 *
 *     ./gradlew :composeApp:checkAppPath -PvideoId=<id> [-PplaySeconds=90]
 *
 * checkPlayback drives a plain media player, so it cannot tell a YouTube refusal apart from the
 * app's own player setup being wrong. Same track passing there and failing here means the fault is
 * ours, not YouTube's.
 */
fun main(args: Array<String>) = runBlocking {
    val videoId = args.firstOrNull() ?: "dQw4w9WgXcQ"
    val targetMs = (System.getProperty("playSeconds")?.toLongOrNull() ?: 15L) * 1000

    initializeEnvironment()

    val url = resolveAudioStreamUrl(videoId)
    if (url == null) {
        println("FAILED: no URL resolved for $videoId")
        exitProcess(1)
    }

    // Exactly what FramePlayer does on a DisposableEffect(url).
    val controller = VlcjFrameController()
    controller.load(url)
    controller.play()

    var elapsed = 0L
    var stalledFor = 0
    while (elapsed < targetMs && stalledFor < 20) {
        Thread.sleep(500)
        val now = controller.state.value.timestamp
        stalledFor = if (now > elapsed) 0 else stalledFor + 1
        elapsed = now
    }

    val state = controller.state.value
    println("state: timestamp=${elapsed}ms duration=${state.duration}ms isPlaying=${state.isPlaying}")
    controller.dispose()

    if (elapsed < targetMs) {
        println("FAILED: the app's own controller stalled at ${elapsed}ms, expected ${targetMs}ms")
        exitProcess(1)
    }
    println("OK: the app's playback path works.")
}
