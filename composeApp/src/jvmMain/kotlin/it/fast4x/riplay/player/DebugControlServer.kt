package it.fast4x.riplay.player

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.flow.MutableStateFlow
import player.PlayerController
import java.net.InetSocketAddress

/**
 * Local HTTP control surface for driving the app during development, so the player can be exercised
 * without pixel-level UI automation (which the environment cannot do). Off unless -Priplay.control.
 *
 *   curl localhost:8777/state
 *   curl "localhost:8777/load?v=<videoId>"   # sets the app's now-playing, same path as a click
 *   curl localhost:8777/play  /pause  /next  /prev
 *   curl "localhost:8777/seek?ms=30000"      curl "localhost:8777/volume?v=0.5"
 */
object DebugControlServer {

    private const val PORT = 8777

    /** Set by the UI so control commands reach the live controller. */
    @Volatile var controller: PlayerController? = null

    /** The UI collects this and applies it to its videoId, mirroring a song click end to end. */
    val requestedVideo = MutableStateFlow<String?>(null)

    @Volatile private var started = false

    fun startIfEnabled() {
        if (started || System.getProperty("riplay.control") == null) return
        started = true
        runCatching {
            HttpServer.create(InetSocketAddress("127.0.0.1", PORT), 0).apply {
                createContext("/") { handle(it) }
                start()
            }
            println("[riplay] control server on http://127.0.0.1:$PORT")
        }.onFailure { println("[riplay] control server failed: $it") }
    }

    private fun handle(exchange: HttpExchange) {
        val path = exchange.requestURI.path
        val query = exchange.requestURI.query.orEmpty().split('&')
            .mapNotNull { it.split('=', limit = 2).takeIf { p -> p.size == 2 } }
            .associate { (k, v) -> k to v }
        val c = controller

        val body = when (path) {
            "/state" -> c?.state?.value?.let {
                """{"videoId":"${requestedVideo.value ?: ""}","playing":${it.isPlaying},""" +
                    """"timestampMs":${it.timestamp},"durationMs":${it.duration},"volume":${it.volume},"muted":${it.isMuted}}"""
            } ?: """{"error":"no controller"}"""
            "/load" -> { query["v"]?.let { requestedVideo.value = it }; "ok ${query["v"]}" }
            "/play" -> { c?.play(); "ok" }
            "/pause" -> { c?.pause(); "ok" }
            "/seek" -> { query["ms"]?.toLongOrNull()?.let { c?.seekTo(it) }; "ok" }
            "/volume" -> { query["v"]?.toFloatOrNull()?.let { c?.setVolume(it) }; "ok" }
            else -> "unknown: $path"
        }

        val bytes = body.toByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
