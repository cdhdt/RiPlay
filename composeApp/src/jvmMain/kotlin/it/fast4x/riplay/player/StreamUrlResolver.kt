package it.fast4x.riplay.player

import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.Context
import it.fast4x.environment.utils.NewPipeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi

/**
 * Resolves a playable audio URL for VLCJ. Returns null when the track is unavailable
 * (removed, region blocked, age restricted) — a common case, not an error.
 */
@OptIn(ExperimentalSerializationApi::class)
suspend fun resolveAudioStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
    val signatureTimestamp = NewPipeUtils.getSignatureTimestamp(videoId)
        .onFailure { println("resolveAudioStreamUrl: no signature timestamp — $it") }
        .getOrNull()

    // The iOS client returns direct URLs; WEB_REMIX returns signatureCipher, whose deciphering
    // YouTube breaks regularly. Diagnose with ./gradlew :composeApp:checkStreamUrl
    val player = listOf(Context.DefaultWeb3, Context.DefaultWeb)
        .firstNotNullOfOrNull { context ->
            EnvironmentExt.simpleMetadataPlayer(
                videoId = videoId,
                client = context.client,
                signatureTimestamp = signatureTimestamp,
            )
                .onFailure { println("resolveAudioStreamUrl: player call failed — $it") }
                .getOrNull()
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

}
