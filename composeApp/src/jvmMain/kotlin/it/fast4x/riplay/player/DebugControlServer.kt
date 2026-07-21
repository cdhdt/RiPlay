package it.fast4x.riplay.player

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.flow.MutableStateFlow
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

    /** Set by the UI so control commands reach the live player. */
    @Volatile var player: QueuePlayer? = null

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
        val p = player

        val body = when (path) {
            "/state" -> p?.let {
                val s = it.playback.value
                val q = it.queue.value
                """{"videoId":"${q.current?.videoId ?: ""}","title":"${q.current?.title ?: ""}",""" +
                    """"playing":${s.isPlaying},"timestampMs":${s.timestamp},"durationMs":${s.duration},""" +
                    """"volume":${s.volume},"muted":${s.isMuted},"queueSize":${q.items.size},"index":${q.index},""" +
                    """"shuffle":${q.shuffle},"repeat":"${q.repeat}"}"""
            } ?: """{"error":"no player"}"""
            "/load" -> { query["v"]?.let { requestedVideo.value = it }; "ok ${query["v"]}" }
            "/play" -> { p?.play(); "ok" }
            "/pause" -> { p?.pause(); "ok" }
            "/next" -> { p?.next(); "ok" }
            "/prev" -> { p?.previous(); "ok" }
            "/shuffle" -> { p?.toggleShuffle(); "ok" }
            "/repeat" -> { p?.cycleRepeat(); "ok" }
            "/seek" -> { query["ms"]?.toLongOrNull()?.let { p?.seekTo(it) }; "ok" }
            "/volume" -> { query["v"]?.toFloatOrNull()?.let { p?.setVolume(it) }; "ok" }
            else -> "unknown: $path"
        }

        val bytes = body.toByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
