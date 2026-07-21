package it.fast4x.riplay.player

/**
 * Verbose playback logging, off by default. Turn on with any of:
 *
 *     ./gradlew :composeApp:run -Priplay.debug          (adds the JVM flag, see build.gradle.kts)
 *     RIPLAY_DEBUG=1 <the packaged binary>
 *     -Driplay.debug=true                               (raw JVM flag)
 *
 * Logs the stream resolution and every HTTP request VLC's audio is fed from, so a track that will
 * not start can be traced without guessing.
 */
object Debug {
    val enabled: Boolean =
        System.getProperty("riplay.debug")?.let { it.isEmpty() || it.toBoolean() } == true ||
            System.getenv("RIPLAY_DEBUG").let { it == "1" || it?.toBoolean() == true }

    fun log(message: String) {
        if (enabled) println("[riplay] $message")
    }

    inline fun log(tag: String, block: () -> String) {
        if (enabled) println("[riplay] $tag: ${block()}")
    }
}
