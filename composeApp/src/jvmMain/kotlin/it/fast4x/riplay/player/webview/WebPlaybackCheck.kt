package it.fast4x.riplay.player.webview

import com.sun.net.httpserver.HttpServer
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import netscape.javascript.JSObject
import java.net.InetSocketAddress
import javax.swing.JFrame
import kotlin.system.exitProcess

/**
 * Phase 0 gate: proves (or disproves) that JavaFX WebKit can run YouTube's IFrame player and
 * actually decode audio, before any UI integration.
 *
 *     ./gradlew :composeApp:checkWebPlayback -PvideoId=<id>
 *
 * Succeeds only if the player's clock advances past 1 second — i.e. real playback, not just a
 * page that loaded. Throwaway: inline HTML, prints instead of wiring into the app.
 */

/** Minimal IFrame page. ORIGIN_URL is replaced with the local server's origin (the IFrame needs one). */
private val PLAYER_HTML = """
<!DOCTYPE html>
<html>
  <head><meta charset="utf-8"><style>html,body{margin:0;height:100%;background:#000}</style>
    <!-- defer so the library runs after the page is parsed and onYouTubeIframeAPIReady exists,
         otherwise the API becomes ready before the callback is defined and the call is lost. -->
    <script defer src="https://www.youtube.com/iframe_api"></script>
  </head>
  <body>
    <div id="player"></div>
    <script>
      // window.YouTubePlayerBridge is injected by JavaFX only after the document loads, so it is
      // referenced at call time here, never cached at parse time (which would capture undefined).
      function bridge() { return window.YouTubePlayerBridge; }
      window.onerror = function(m) { if (bridge()) bridge().jsLog('onerror: ' + m); };
      var player;
      var apiReady = false;
      // The IFrame API and the JavaFX-injected bridge become ready in an undefined order. Gate the
      // player creation on both, retrying until the bridge exists, so neither ordering loses events.
      function onYouTubeIframeAPIReady() { apiReady = true; tryStart(); }
      function tryStart() {
        if (!apiReady) return;
        if (!bridge()) { setTimeout(tryStart, 50); return; }
        bridge().sendYouTubeIFrameAPIReady();
        player = new YT.Player('player', {
          height: '200', width: '200',
          playerVars: { autoplay: 1, controls: 0, origin: 'ORIGIN_URL' },
          events: {
            onReady: function() { bridge().sendReady(); },
            onStateChange: function(e) {
              bridge().sendStateChange(String(e.data));
              if (e.data === 1) bridge().sendVideoDuration(player.getDuration());
            },
            onError: function(e) { bridge().sendError(String(e.data)); }
          }
        });
      }
      function loadVideo(id) { player.loadVideoById(id, 0); }
      function playVideo() { player.playVideo(); }
      function currentTime() { return player ? player.getCurrentTime() : -1; }
    </script>
  </body>
</html>
""".trimIndent()

/** JS → Java. JavaFX invokes these public methods when the page calls window.YouTubePlayerBridge.*. */
class SpikeBridge {
    @Volatile var apiReady = false
    @Volatile var ready = false
    @Volatile var lastState = ""
    @Volatile var duration = 0.0
    @Volatile var error = ""

    fun sendYouTubeIFrameAPIReady() { apiReady = true; println("[web] iframe api ready") }
    fun sendReady() { ready = true; println("[web] player ready") }
    fun sendStateChange(state: String) { lastState = state; println("[web] state=$state") }
    fun sendVideoDuration(d: Any?) { duration = (d as? Number)?.toDouble() ?: 0.0; println("[web] duration=$duration") }
    fun sendError(code: String) { error = code; println("[web] ERROR=$code") }
    fun jsLog(message: String) { println("[web][js] $message") }
}

fun main(args: Array<String>) {
    val videoId = args.firstOrNull() ?: "dQw4w9WgXcQ"

    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    val origin = "http://127.0.0.1:${server.address.port}"
    val html = PLAYER_HTML.replace("ORIGIN_URL", origin).toByteArray()
    server.createContext("/") { exchange ->
        exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
        exchange.sendResponseHeaders(200, html.size.toLong())
        exchange.responseBody.use { it.write(html) }
    }
    server.start()
    println("[web] serving IFrame at $origin")

    val bridge = SpikeBridge()
    lateinit var webView: WebView

    // Creating a JFXPanel boots the JavaFX toolkit; a shown frame gives the WebView a live scene,
    // without which WebKit will not run the page's JS or play audio.
    val panel = JFXPanel()
    val frame = JFrame("web-spike").apply {
        setSize(220, 240)
        contentPane.add(panel)
        isVisible = true
    }

    Platform.runLater {
        webView = WebView()
        panel.scene = Scene(webView)
        val engine = webView.engine
        engine.setOnError { println("[web] engine error: ${it.message}") }
        engine.loadWorker.stateProperty().addListener { _, _, state ->
            println("[web] loadWorker: $state")
            if (state == javafx.concurrent.Worker.State.SUCCEEDED) {
                (engine.executeScript("window") as JSObject).setMember("YouTubePlayerBridge", bridge)
                // Route the page's console.log through the bridge; WebEngine has no console otherwise.
                engine.executeScript("console.log = function(m){ window.YouTubePlayerBridge.jsLog('log: '+m); };")
                println("[web] bridge installed")
            }
        }
        engine.load(origin)
    }

    // Diagnose whether the external iframe_api script loaded and runs in this WebKit at all.
    Thread.sleep(6000)
    for (probe in listOf("navigator.userAgent", "typeof YT", "typeof onYouTubeIframeAPIReady",
            "document.querySelectorAll('script').length")) {
        println("[web][probe] $probe = ${evalString(webView, probe)}")
    }

    // Wait for the player to be ready, then load and play.
    if (!waitFor(20_000) { bridge.ready || bridge.error.isNotEmpty() }) {
        fail(server, frame, "player never became ready (api=${bridge.apiReady})")
    }
    if (bridge.error.isNotEmpty()) fail(server, frame, "onError=${bridge.error} before load")

    Platform.runLater {
        webView.engine.executeScript("loadVideo('$videoId'); playVideo();")
    }

    // Success only if the clock actually advances.
    var advanced = false
    val deadline = 25_000
    val start = readClock()
    var elapsed = 0L
    while (elapsed < deadline && !advanced) {
        Thread.sleep(500)
        elapsed = readClock() - start
        val t = currentTime(webView)
        println("[web] currentTime=$t state=${bridge.lastState} err=${bridge.error}")
        if (bridge.error.isNotEmpty()) fail(server, frame, "onError=${bridge.error} during playback")
        if (t > 1.0) advanced = true
    }

    server.stop(0)
    Platform.runLater { frame.dispose() }
    if (advanced) {
        println("OK: JavaFX WebView plays the IFrame — audio clock advanced.")
        exitProcess(0)
    } else {
        println("FAILED: clock never advanced past 1s (state=${bridge.lastState}).")
        exitProcess(1)
    }
}

private fun evalString(webView: WebView, script: String): String {
    val result = java.util.concurrent.CompletableFuture<String>()
    Platform.runLater { result.complete(runCatching { "${webView.engine.executeScript(script)}" }.getOrElse { "ERR:${it.message}" }) }
    return runCatching { result.get(3, java.util.concurrent.TimeUnit.SECONDS) }.getOrDefault("timeout")
}

private fun currentTime(webView: WebView): Double {
    val result = java.util.concurrent.CompletableFuture<Double>()
    Platform.runLater {
        result.complete((webView.engine.executeScript("currentTime()") as? Number)?.toDouble() ?: -1.0)
    }
    return runCatching { result.get(3, java.util.concurrent.TimeUnit.SECONDS) }.getOrDefault(-1.0)
}

private fun waitFor(timeoutMs: Long, condition: () -> Boolean): Boolean {
    val end = readClock() + timeoutMs
    while (readClock() < end) {
        if (condition()) return true
        Thread.sleep(200)
    }
    return condition()
}

private fun fail(server: HttpServer, frame: JFrame, message: String): Nothing {
    server.stop(0)
    Platform.runLater { frame.dispose() }
    println("FAILED: $message")
    exitProcess(1)
}

/** System.nanoTime-based clock; Date/currentTimeMillis add nothing here and nanoTime is monotonic. */
private fun readClock(): Long = System.nanoTime() / 1_000_000
