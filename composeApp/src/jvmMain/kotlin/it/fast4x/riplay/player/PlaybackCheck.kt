package it.fast4x.riplay.player

import it.fast4x.riplay.commonutils.initializeEnvironment
import it.fast4x.riplay.player.vlcj.VlcNative
import kotlinx.coroutines.runBlocking
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import kotlin.system.exitProcess

/**
 * Plays a track through VLC and checks the clock actually moves:
 *
 *     ./gradlew :composeApp:checkPlayback -PvideoId=<id>
 *
 * Resolving a URL proves nothing about playback — the stream can 403, the codec can be missing,
 * libvlc can be absent. Uses a dummy audio output so it runs headless and in CI.
 */
fun main(args: Array<String>) = runBlocking {
    val videoId = args.firstOrNull() ?: "dQw4w9WgXcQ"

    initializeEnvironment()

    println("libvlc: ${VlcNative.describe()} — found=${VlcNative.available}")
    if (!VlcNative.available) {
        println(VlcNative.missingMessage())
        exitProcess(1)
    }

    val url = resolveAudioStreamUrl(videoId)
    check(url != null) { "FAILED: no URL resolved for $videoId" }

    val factory = MediaPlayerFactory(*VlcNative.factoryArgs, "--aout=dummy", "--quiet")
    val player = factory.mediaPlayers().newMediaPlayer()

    try {
        check(player.media().play(url)) { "FAILED: libvlc refused to open the stream" }

        // Startup covers the network handshake; VLC reports isPlaying before any audio is decoded.
        var elapsed = 0L
        repeat(30) {
            Thread.sleep(500)
            elapsed = player.status().time()
            if (elapsed > 1000) return@repeat
        }

        println("playing=${player.status().isPlaying} time=${elapsed}ms length=${player.status().length()}ms")
        check(player.status().isPlaying) { "FAILED: player is not playing" }
        check(elapsed > 0) { "FAILED: playback clock never advanced, nothing is being decoded" }

        println("OK: audio is decoding through VLC.")
    } finally {
        player.controls().stop()
        player.release()
        factory.release()
    }
}
