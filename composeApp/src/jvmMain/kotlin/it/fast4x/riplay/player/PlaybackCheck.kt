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

    val userAgent = it.fast4x.environment.models.Context.DefaultWeb3.client.userAgent
    println("client user-agent: $userAgent")

    // Probes the very URL handed to VLC next. Resolving a second one would compare two different
    // URLs and prove nothing about which side is at fault.
    val probe = (java.net.URI(url).toURL().openConnection() as java.net.HttpURLConnection).apply {
        requestMethod = "GET"
        setRequestProperty("Range", "bytes=0-1")
        connectTimeout = 15_000
        readTimeout = 15_000
    }
    println("java GET on the same url: HTTP ${probe.responseCode} ${probe.contentType}")
    probe.disconnect()

    try {
        check(player.media().play(url)) {
            "FAILED: libvlc refused to open the stream"
        }

        // A short run only proves the first chunk works. -PplaySeconds=90 crosses a chunk boundary,
        // which is where the stream is refetched and where 403s showed up.
        val targetMs = (System.getProperty("playSeconds")?.toLongOrNull() ?: 15L) * 1000
        var elapsed = 0L
        var stalledFor = 0
        while (elapsed < targetMs && stalledFor < 20) {
            Thread.sleep(500)
            val now = player.status().time()
            stalledFor = if (now > elapsed) 0 else stalledFor + 1
            elapsed = now
        }

        println("playing=${player.status().isPlaying} time=${elapsed}ms length=${player.status().length()}ms")
        check(player.status().isPlaying) { "FAILED: player is not playing" }
        check(elapsed > 0) { "FAILED: playback clock never advanced, nothing is being decoded" }
        check(elapsed >= targetMs) {
            "FAILED: playback stalled at ${elapsed}ms, expected to reach ${targetMs}ms"
        }

        println("OK: audio is decoding through VLC.")
    } finally {
        player.controls().stop()
        player.release()
        factory.release()
    }
}
