package it.fast4x.riplay.player

import it.fast4x.environment.EnvironmentExt
import it.fast4x.environment.models.Context
import it.fast4x.environment.utils.NewPipeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi

/**
 * Résout l'URL audio jouable d'une vidéo YouTube, à donner à VLCJ.
 *
 * Reprend la même chaîne que l'app Android (`extensions/players/SimpleMetadataPlayer.kt`) : tout passe
 * par le module `environment`, qui est en JVM pur — rien d'Android là-dedans.
 *
 * Renvoie null si la piste est indisponible (retirée, bloquée par région, âge restreint…) : à l'appelant
 * de décider quoi en faire. On ne lève pas, ce cas est courant et normal.
 */
@OptIn(ExperimentalSerializationApi::class)
suspend fun resolveAudioStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
    // Sans signatureTimestamp YouTube renvoie des formats dont la signature est inexploitable.
    val signatureTimestamp = NewPipeUtils.getSignatureTimestamp(videoId)
        .onFailure { println("resolveAudioStreamUrl: signatureTimestamp indisponible — $it") }
        .getOrNull()

    // DefaultWeb3 (client iOS) renvoie des URL directes ; DefaultWeb (WEB_REMIX) renvoie des
    // signatureCipher qu'il faut déchiffrer via NewPipeExtractor — étape que YouTube casse
    // régulièrement et qui est en panne à ce jour. On prend donc iOS d'abord, WEB en repli au cas
    // où la config distante changerait. Diagnostic : ./gradlew :composeApp:checkStreamUrl
    val player = listOf(Context.DefaultWeb3, Context.DefaultWeb)
        .firstNotNullOfOrNull { context ->
            EnvironmentExt.simpleMetadataPlayer(
                videoId = videoId,
                client = context.client,
                signatureTimestamp = signatureTimestamp,
            )
                .onFailure { println("resolveAudioStreamUrl: appel player en échec — $it") }
                .getOrNull()
                ?.takeIf { it.streamingData?.autoMaxQualityFormat != null }
        } ?: return@withContext null

    if (player.playabilityStatus?.status != "OK") {
        println("resolveAudioStreamUrl: $videoId non jouable — ${player.playabilityStatus?.status}")
        return@withContext null
    }

    // ponytail: autoMaxQualityFormat privilégie déjà les itags audio (251/140/141...), inutile de
    // refaire le tri ici. Un sélecteur de qualité viendra le jour où c'est réglable dans l'UI.
    val format = player.streamingData?.autoMaxQualityFormat
    if (format == null) {
        println("resolveAudioStreamUrl: aucun format exploitable " +
                "(${player.streamingData?.adaptiveFormats?.size ?: 0} formats reçus)")
        return@withContext null
    }

    NewPipeUtils.getStreamUrl(format, videoId)
        .onFailure { println("resolveAudioStreamUrl: déchiffrement d'URL en échec — $it") }
        .getOrNull()
}
