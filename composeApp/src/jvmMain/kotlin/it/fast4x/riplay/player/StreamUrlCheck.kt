package it.fast4x.riplay.player

import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.Context
import it.fast4x.environment.utils.NewPipeUtils
import it.fast4x.riplay.commonutils.initializeEnvironment
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import java.net.HttpURLConnection
import java.net.URI

/**
 * Standalone check for [resolveAudioStreamUrl], without starting the UI:
 *
 *     ./gradlew :composeApp:checkStreamUrl -PvideoId=<id>
 *
 * It fetches the stream rather than just asserting a non-null URL, because a stale signature
 * yields a normal looking URL that answers 403.
 */
fun main(args: Array<String>) = runBlocking {
    val videoId = args.firstOrNull() ?: "dQw4w9WgXcQ"

    initializeEnvironment()

    probeClients(videoId)

    println("Resolving $videoId ...")
    val url = resolveAudioStreamUrl(videoId)
    check(url != null) { "FAILED: no URL resolved for $videoId" }
    println("Got URL (${url.length} chars): ${url.take(120)}...")

    val connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        setRequestProperty("Range", "bytes=0-1")
        connectTimeout = 15_000
        readTimeout = 15_000
    }

    val status = connection.responseCode
    val contentType = connection.contentType.orEmpty()
    connection.disconnect()

    println("HTTP $status, Content-Type: $contentType")
    check(status == 200 || status == 206) { "FAILED: stream answered $status, signature likely stale" }
    check(contentType.startsWith("audio/")) { "FAILED: got '$contentType', expected an audio stream" }

    println("OK: audio stream is playable.")
}

/** Reports which clients hand out direct URLs and which require signature deciphering. */
@OptIn(ExperimentalSerializationApi::class)
private suspend fun probeClients(videoId: String) {
    val clients = listOf(
        "DefaultWeb" to Context.DefaultWeb.client,
        "DefaultWeb2" to Context.DefaultWeb2.client,
        "DefaultWeb3" to Context.DefaultWeb3.client,
        "TVHTML5" to Context.TVHTML5_SIMPLY_EMBEDDED_PLAYER.client,
    )

    for ((label, client) in clients) {
        val timestamp = NewPipeUtils.getSignatureTimestamp(videoId).getOrNull()
        val player = EnvironmentExt.simpleMetadataPlayer(
            videoId = videoId, client = client, signatureTimestamp = timestamp,
        ).getOrNull()

        val format = player?.streamingData?.autoMaxQualityFormat
        println(
            "  $label (${client.clientName}/${client.clientVersion}): " +
                    "status=${player?.playabilityStatus?.status} itag=${format?.itag} " +
                    "directUrl=${format?.url != null} cipher=${format?.signatureCipher != null}"
        )
    }
}
