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
 * Vérification autonome de [resolveAudioStreamUrl], sans lancer l'interface.
 *
 *   ./gradlew :composeApp:checkStreamUrl -PvideoId=<id>
 *
 * Le déchiffrement de signature est la partie la plus fragile de la chaîne (YouTube la change
 * plusieurs fois par an) et elle échoue silencieusement : on récupère une URL d'apparence normale
 * qui renvoie 403. D'où la requête réelle en fin de check plutôt qu'un simple test de non-nullité.
 */
fun main(args: Array<String>) = runBlocking {
    // "dQw4w9WgXcQ" par défaut : une vidéo publique, sans restriction d'âge ni de région.
    val videoId = args.firstOrNull() ?: "dQw4w9WgXcQ"

    initializeEnvironment()

    probeClients(videoId)

    println("Résolution de $videoId ...")
    val url = resolveAudioStreamUrl(videoId)
    check(url != null) { "ECHEC: aucune URL résolue pour $videoId" }
    println("URL obtenue (${url.length} caractères): ${url.take(120)}...")

    // Range 0-1 : on veut le code de retour et le type, pas le fichier.
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
    check(status == 200 || status == 206) {
        "ECHEC: le flux répond $status — signature probablement mal déchiffrée"
    }
    check(contentType.startsWith("audio/")) {
        "ECHEC: Content-Type '$contentType', un flux audio était attendu"
    }

    println("OK: flux audio jouable.")
}

/**
 * Diagnostic : pour chaque client YouTube connu, dit si les formats audio arrivent avec une URL
 * directe ou un signatureCipher. Un client à URL directe évite tout le déchiffrement de signature,
 * qui est la partie fragile de la chaîne.
 */
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
                    "statut=${player?.playabilityStatus?.status} itag=${format?.itag} " +
                    "urlDirecte=${format?.url != null} cipher=${format?.signatureCipher != null}"
        )
    }
}
