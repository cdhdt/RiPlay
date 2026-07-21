package it.fast4x.riplay.player.webview

import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpCookie
import java.net.URI
import javax.swing.JFrame
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * Approach-1 gate: drive the real music.youtube.com watch page (native origin, not an embed) in a
 * JavaFX WebView, and prove its <video> clock advances — i.e. it plays the same label/topic tracks
 * the IFrame embed refuses.
 *
 *     ./gradlew :composeApp:checkWebMusic -PvideoId=<id>
 *
 * Handles the two known blockers: the EU consent wall (pre-seeded SOCS cookie) and blocked autoplay
 * (mute, play(), then unmute — the standard muted-autoplay workaround).
 */
fun main(args: Array<String>) {
    val videoId = args.firstOrNull() ?: "uhEsPlkhhXE"
    val url = "https://music.youtube.com/watch?v=$videoId"

    // WebEngine reads cookies from CookieHandler.getDefault(). SOCS=CAI is what skips the consent
    // interstitial that would otherwise redirect us to consent.youtube.com.
    val cookies = CookieManager(null, CookiePolicy.ACCEPT_ALL)
    for (c in listOf("SOCS=CAI", "PREF=hl=en&tz=UTC")) {
        HttpCookie(c.substringBefore('='), c.substringAfter('=')).apply {
            domain = ".youtube.com"; path = "/"; version = 0
        }.let { cookies.cookieStore.add(URI("https://music.youtube.com"), it) }
    }
    java.net.CookieHandler.setDefault(cookies)

    lateinit var webView: WebView
    val panel = JFXPanel()
    val frame = JFrame("web-music-spike").apply { setSize(420, 320); contentPane.add(panel); isVisible = true }

    Platform.runLater {
        webView = WebView()
        webView.engine.userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        panel.scene = Scene(webView)
        webView.engine.setOnError { println("[music] engine error: ${it.message}") }
        webView.engine.load(url)
    }

    println("[music] loading $url")

    // The decisive capability probe: YouTube streams via MSE. If JavaFX WebKit lacks MediaSource,
    // the <video> can never receive data and no web approach here can ever play — settling both
    // the IFrame and the real-page routes at once.
    Thread.sleep(8000)
    println("[music] MediaSource=${eval(webView, "typeof window.MediaSource")}")
    println("[music] opus supported=${eval(webView, "(window.MediaSource&&MediaSource.isTypeSupported)?MediaSource.isTypeSupported('audio/webm; codecs=\"opus\"'):'n/a'")}")
    println("[music] EME=${eval(webView, "typeof navigator.requestMediaKeySystemAccess")}")

    var advanced = false
    val deadline = readClock() + 60_000
    while (readClock() < deadline && !advanced) {
        Thread.sleep(1500)
        val href = eval(webView, "location.href")
        val hasVideo = eval(webView, "!!document.querySelector('video')")
        val time = eval(webView, "(function(){var v=document.querySelector('video');return v?v.currentTime:-1})()")
        val ready = eval(webView, "(function(){var v=document.querySelector('video');return v?v.readyState:-1})()")
        println("[music] href=${href.take(60)} video=$hasVideo readyState=$ready currentTime=$time")

        if (href.contains("consent")) println("[music] WARNING: hit a consent page")

        // Nudge playback: mute (so autoplay is allowed), start, then unmute. Also click the play
        // button if the SPA exposes one — either path is enough to get the clock moving.
        eval(webView, """
            (function(){
              var v=document.querySelector('video');
              if(v){ v.muted=true; var p=v.play(); if(p&&p.catch)p.catch(function(){}); setTimeout(function(){v.muted=false;},1500); }
              var b=document.querySelector('#play-pause-button, tp-yt-paper-icon-button.play-pause-button, .play-pause-button');
              if(b) b.click();
              return v?v.currentTime:-2;
            })()
        """.trimIndent())

        if ((time.toDoubleOrNull() ?: -1.0) > 1.0) advanced = true
    }

    Platform.runLater { frame.dispose() }
    if (advanced) {
        println("OK: real music.youtube.com page plays $videoId in JavaFX WebView (clock advanced).")
        exitProcess(0)
    } else {
        println("FAILED: <video> clock never advanced for $videoId.")
        exitProcess(1)
    }
}

private fun eval(webView: WebView, script: String): String {
    val result = CompletableFuture<String>()
    Platform.runLater {
        result.complete(runCatching { "${webView.engine.executeScript(script)}" }.getOrElse { "ERR:${it.message}" })
    }
    return runCatching { result.get(4, TimeUnit.SECONDS) }.getOrDefault("timeout")
}

private fun readClock(): Long = System.nanoTime() / 1_000_000
