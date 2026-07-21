package it.fast4x.riplay.player

import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.Context
import it.fast4x.environment.utils.NewPipeUtils
import it.fast4x.riplay.player.Debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi

/**
 * Resolves a playable audio URL for a track. Returns null when the track is unavailable
 * (removed, region blocked, age restricted) — a common case, not an error.
 */
@OptIn(ExperimentalSerializationApi::class)
suspend fun resolveAudioStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
    val signatureTimestamp = NewPipeUtils.getSignatureTimestamp(videoId)
        .onFailure { println("resolveAudioStreamUrl: no signature timestamp — $it") }
        .getOrNull()

    // ANDROID_VR first: its direct URLs carry no PO token requirement and no per-URL byte budget,
    // so they survive a full track — the iOS client's URLs 403 partway through many songs. iOS and
    // WEB_REMIX stay as fallbacks. Diagnose with ./gradlew :composeApp:checkStreamUrl
    val player = listOf(Context.ANDROID_VR, Context.DefaultWeb3, Context.DefaultWeb)
        .firstNotNullOfOrNull { context ->
            EnvironmentExt.simpleMetadataPlayer(
                videoId = videoId,
                client = context.client,
                signatureTimestamp = signatureTimestamp,
            )
                .onFailure { println("resolveAudioStreamUrl: player call failed — $it") }
                .getOrNull()
                .also {
                    Debug.log("resolve") {
                        "$videoId via ${context.client.clientName}: " +
                            "status=${it?.playabilityStatus?.status} " +
                            "formats=${it?.streamingData?.adaptiveFormats?.size ?: 0} " +
                            "direct=${it?.streamingData?.autoMaxQualityFormat?.url != null}"
                    }
                }
                ?.takeIf { it.streamingData?.autoMaxQualityFormat != null }
        } ?: return@withContext null

    if (player.playabilityStatus?.status != "OK") {
        println("resolveAudioStreamUrl: $videoId not playable — ${player.playabilityStatus?.status}")
        return@withContext null
    }

    val format = player.streamingData?.autoMaxQualityFormat
    if (format == null) {
        println("resolveAudioStreamUrl: no usable format " +
                "(${player.streamingData?.adaptiveFormats?.size ?: 0} returned)")
        return@withContext null
    }

    NewPipeUtils.getStreamUrl(format, videoId)
        .onFailure { println("resolveAudioStreamUrl: url deciphering failed — $it") }
        .getOrNull()
        .also { Debug.log("resolve") { "$videoId itag=${format.itag} url=${it?.take(90)}" } }
}
