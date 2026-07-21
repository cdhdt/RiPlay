package it.fast4x.riplay.player.webview

import dev.datlag.kcef.KCEFBrowser
import it.fast4x.riplay.player.Debug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.cef.browser.CefRendering
import player.PlayerController
import player.PlayerState
import java.util.Date
import javax.swing.JFrame

/**
 * Plays a videoId by driving the real music.youtube.com page in a hidden embedded Chromium — the
 * only engine that plays label/topic content on desktop (see the checkCef gate). The browser runs
 * in an off-screen 1px frame; audio does not depend on the window being visible.
 *
 * [load] takes a videoId, not a stream URL: we hand the id to YouTube Music and let it stream.
 */
class CefPlayerController : PlayerController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    // What the user wants; the poll loop keeps nudging the page toward it, since YouTube Music does
    // not start on its own and the SPA can drop a play() during navigation.
    @Volatile private var desiredPlaying = false

    // Requested track vs the one the page actually shows. loadURL issued right after construction is
    // dropped (the native browser is not ready yet), so the loop re-issues it until it takes.
    @Volatile private var targetVideoId: String? = null
    private var confirmedVideoId: String? = null

    private val browser: KCEFBrowser = CefRuntime.requireClient()
        .createBrowser("about:blank", CefRendering.DEFAULT, false)

    // Positioned far off-screen so the user never sees it, but left visible and non-minimized:
    // Chromium throttles/suspends a hidden or iconified window and the page then never plays.
    private val frame = JFrame().apply {
        isUndecorated = true
        setSize(400, 300)
        // -Driplay.cefvisible keeps it on-screen for debugging whether off-screen breaks playback.
        if (System.getProperty("riplay.cefvisible") == null) setLocation(-4000, -4000)
        contentPane.add(browser.uiComponent)
        isVisible = true
    }

    init {
        seedConsentCookie()
        scope.launch { pollLoop() }
    }

    override fun load(videoId: String) {
        Debug.log("cef") { "load $videoId" }
        desiredPlaying = true
        targetVideoId = videoId
    }

    override fun play() { desiredPlaying = true; js("var v=document.querySelector('video'); if(v) v.play();") }
    override fun pause() { desiredPlaying = false; js("var v=document.querySelector('video'); if(v) v.pause();") }
    override fun stop() { desiredPlaying = false; js("var v=document.querySelector('video'); if(v){ v.pause(); v.currentTime=0; }") }
    override fun seekTo(timestamp: Long) =
        js("var v=document.querySelector('video'); if(v) v.currentTime=${timestamp / 1000.0};")

    override fun setVolume(value: Float) {
        _state.update { it.copy(volume = value) }
        js("var v=document.querySelector('video'); if(v) v.volume=${value.coerceIn(0f, 1f)};")
    }

    override fun toggleSound() {
        val muted = !_state.value.isMuted
        _state.update { it.copy(isMuted = muted) }
        js("var v=document.querySelector('video'); if(v) v.muted=$muted;")
    }

    override fun dispose() {
        scope.cancel()
        runCatching { browser.close(true) }
        runCatching { frame.dispose() }
    }

    /** Fire-and-forget JS; control calls do not need a return value. */
    private fun js(script: String) {
        scope.launch { runCatching { browser.evaluateJavaScript(script) } }
    }

    private fun seedConsentCookie() {
        val now = Date()
        val expires = Date(now.time + 365L * 24 * 3600 * 1000)
        runCatching {
            org.cef.network.CefCookieManager.getGlobalManager().setCookie(
                "https://www.youtube.com",
                org.cef.network.CefCookie("SOCS", "CAI", ".youtube.com", "/", true, false, now, now, true, expires),
            )
        }
    }

    /** Clears the consent interstitial when it appears, and mirrors the <video> into [state]. */
    private suspend fun pollLoop() {
        var tick = 0
        while (true) {
            delay(400)
            val href = browser.evaluateJavaScript("String(location.href)") ?: continue
            if (Debug.enabled && tick++ % 5 == 0) Debug.log("cef") { "href=${href.take(55)}" }

            if (href.contains("consent")) {
                browser.evaluateJavaScript(
                    "(function(){var b=document.querySelectorAll('button');for(var i=0;i<b.length;i++){" +
                        "var t=(b[i].textContent||'').toLowerCase();" +
                        "if(t.indexOf('accept')>=0||t.indexOf('accepter')>=0||t.indexOf('reject')>=0||t.indexOf('refuser')>=0){b[i].click();break;}}})()"
                )
                continue
            }

            // Drive navigation here rather than in load(): re-issue loadURL until the page actually
            // shows the requested track, which also covers switching tracks later.
            val target = targetVideoId
            if (target != null && target != confirmedVideoId) {
                if (href.contains("watch?v=$target")) confirmedVideoId = target
                else browser.loadURL("https://music.youtube.com/watch?v=$target")
            }

            // One round trip returns the whole player state as a compact string, to keep the JS
            // bridge chatter down to a single call per tick.
            val snapshot = browser.evaluateJavaScript(
                "(function(){var v=document.querySelector('video');" +
                    "return v?[v.currentTime,v.duration,v.paused?0:1,v.muted?1:0,v.volume].join(','):''})()"
            ) ?: ""
            parseSnapshot(snapshot)
            if (Debug.enabled && tick % 5 == 0) Debug.log("cef") { "snapshot=[$snapshot]" }

            skipAdIfPresent()

            // YouTube Music does not resume by itself after an ad, and off-screen it reports
            // document.hidden=true and pauses. #movie_player is the underlying YouTube player and
            // exposes the same API as the IFrame (playVideo/pauseVideo/seekTo) — far more reliable
            // than the shadow-DOM play button, whose shadow root is not reachable here.
            val paused = snapshot.isNotEmpty() && snapshot.split(',').getOrNull(2) == "0"
            if (desiredPlaying && paused) {
                val r = browser.evaluateJavaScript(
                    "(function(){" +
                        "try{Object.defineProperty(document,'hidden',{value:false,configurable:true});" +
                        "Object.defineProperty(document,'visibilityState',{value:'visible',configurable:true});}catch(e){}" +
                        "var v=document.querySelector('video');if(!v||!v.paused)return'notpaused';" +
                        "var mp=document.getElementById('movie_player');" +
                        "if(mp&&mp.playVideo){mp.playVideo();return'mp';}" +
                        "if(v.play){v.play();}return'vplay';})()"
                )
                if (Debug.enabled) Debug.log("cef") { "play nudge: $r" }
            }
        }
    }

    /**
     * Skips audio ads the way the mobile app does: when the YouTube HTML5 player marks an ad
     * (ad-showing / video-ads / a skip button), fast-forward the ad video to its end so YouTube
     * moves straight to the track. YouTube Music embeds the same player, so the same markers work.
     */
    private suspend fun skipAdIfPresent() {
        val result = browser.evaluateJavaScript(
            "(function(){var v=document.querySelector('video');if(!v)return'';" +
                // On music.youtube.com the ad and the track share the same <video>. Seeking to the
                // end skips the ad, but doing it to a real track would break it, so never touch a
                // video longer than an ad (~90s) — the duration guard is the real safety net.
                "if(!isFinite(v.duration)||v.duration<=0||v.duration>90)return'';" +
                "var c=document.querySelector('.video-ads');" +
                "var ad=(c&&c.childElementCount>0)||document.getElementsByClassName('ad-showing').length>0;" +
                "if(ad){v.currentTime=v.duration-0.1;return'ad';}return'';})()"
        )
        if (!result.isNullOrEmpty()) Debug.log("cef") { "ad handled ($result)" }
    }

    private fun parseSnapshot(snapshot: String) {
        val parts = snapshot.split(',')
        if (parts.size < 5) return
        val time = parts[0].toDoubleOrNull() ?: return
        val duration = parts[1].toDoubleOrNull() ?: 0.0
        _state.update {
            it.copy(
                timestamp = (time * 1000).toLong(),
                duration = (if (duration.isFinite()) duration * 1000 else 0.0).toLong(),
                isPlaying = parts[2] == "1",
                isMuted = parts[3] == "1",
                volume = parts[4].toFloatOrNull() ?: it.volume,
            )
        }
    }
}
