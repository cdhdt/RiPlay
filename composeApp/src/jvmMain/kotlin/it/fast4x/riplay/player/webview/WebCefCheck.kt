package it.fast4x.riplay.player.webview

import dev.datlag.kcef.KCEF
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.cef.browser.CefRendering
import java.io.File
import javax.swing.JFrame
import kotlin.system.exitProcess

/**
 * JCEF gate: prove embedded Chromium (full MSE) plays a label/topic track that both the IFrame and
 * JavaFX WebView refused.
 *
 *     ./gradlew :composeApp:checkCef -PvideoId=<id>
 *
 * Loads the real music.youtube.com watch page (native origin, no embed restriction) and asserts the
 * <video> clock advances. First run downloads the ~150 MB CEF bundle into ~/.riplay/kcef.
 */
fun main(args: Array<String>): Unit = runBlocking {
    val videoId = args.firstOrNull() ?: "uhEsPlkhhXE"
    val url = "https://music.youtube.com/watch?v=$videoId"
    val installDir = File(System.getProperty("user.home"), ".riplay/kcef")

    println("[cef] initializing (bundle in $installDir) ...")
    KCEF.initBlocking(
        builder = {
            installDir(installDir)
            progress {
                onDownloading { println("[cef] downloading ${it.toInt()}%") }
                onInitialized { println("[cef] initialized") }
            }
            settings {
                cachePath = File(installDir, "cache").absolutePath
                // Under a plain OpenJDK, JCEF otherwise looks for the render subprocess in
                // java.home/lib (a JetBrains Runtime layout) and crashes; point it at the bundle.
                browserSubProcessPath = File(installDir, "jcef_helper").absolutePath
                resourcesDirPath = installDir.absolutePath
                localesDirPath = File(installDir, "locales").absolutePath
            }
        },
        onError = { it?.printStackTrace() },
    )

    val client = KCEF.newClientBlocking()
    val browser = client.createBrowser("about:blank", CefRendering.DEFAULT, false)
    val frame = JFrame("cef-spike").apply {
        setSize(500, 400)
        contentPane.add(browser.uiComponent)
        isVisible = true
    }

    // Give CefApp time to reach INITIALIZED — the cookie store is not usable before that.
    delay(3000)

    // SOCS=CAI skips the EU consent wall that otherwise parks the page on consent.youtube.com.
    // CEF has its own cookie store, so it must be set here (not via java.net.CookieHandler).
    val now = java.util.Date()
    val expires = java.util.Date(now.time + 365L * 24 * 3600 * 1000)
    org.cef.network.CefCookieManager.getGlobalManager().setCookie(
        "https://www.youtube.com",
        org.cef.network.CefCookie("SOCS", "CAI", ".youtube.com", "/", true, false, now, now, true, expires),
    )

    browser.loadURL(url)
    println("[cef] loading $url")

    var advanced = false
    repeat(45) {
        delay(1500)
        val time = browser.evaluateJavaScript(
            "(function(){var v=document.querySelector('video');return v?String(v.currentTime):'novideo'})()"
        )
        val ready = browser.evaluateJavaScript(
            "(function(){var v=document.querySelector('video');return v?String(v.readyState):'-1'})()"
        )
        val mse = browser.evaluateJavaScript("String(typeof window.MediaSource)")
        val href = browser.evaluateJavaScript("String(location.href)")
        val title = browser.evaluateJavaScript("String(document.title)")
        println("[cef] href=${href?.take(70)} title=${title?.take(40)}")

        // The SOCS cookie alone no longer clears consent; click the accept/reject button when the
        // page is the interstitial, in whatever language it renders.
        if (href?.contains("consent") == true) {
            val clicked = browser.evaluateJavaScript(
                "(function(){var b=document.querySelectorAll('button');for(var i=0;i<b.length;i++){" +
                    "var t=(b[i].textContent||'').toLowerCase();" +
                    "if(t.indexOf('accept')>=0||t.indexOf('accepter')>=0||t.indexOf('reject')>=0||t.indexOf('refuser')>=0){b[i].click();return t;}}" +
                    "return 'no-btn/'+b.length;})()"
            )
            println("[cef] consent click: $clicked")
        }
        // Nudge autoplay: mute, play, unmute; also click the play button if present.
        browser.evaluateJavaScript(
            "(function(){var v=document.querySelector('video');" +
                "if(v){v.muted=true;if(v.play)v.play();setTimeout(function(){v.muted=false},1500);}" +
                "var b=document.querySelector('#play-pause-button,.play-pause-button');if(b)b.click();})()"
        )
        println("[cef] MediaSource=$mse readyState=$ready currentTime=$time")
        if ((time?.toDoubleOrNull() ?: -1.0) > 1.0) advanced = true
        if (advanced) return@repeat
    }

    frame.isVisible = false
    KCEF.disposeBlocking()
    if (advanced) {
        println("OK: embedded Chromium plays $videoId (clock advanced).")
        exitProcess(0)
    } else {
        println("FAILED: <video> clock never advanced for $videoId.")
        exitProcess(1)
    }
}
