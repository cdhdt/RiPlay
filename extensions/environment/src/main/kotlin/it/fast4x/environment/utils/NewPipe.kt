package it.fast4x.environment.utils

import it.fast4x.environment.Environment
import it.fast4x.environment.models.Context
import it.fast4x.environment.models.PlayerResponse
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import java.io.IOException
import java.net.Proxy
import java.net.URLDecoder

private class NewPipeDownloaderImpl(proxy: Proxy?) : Downloader() {

    private val client = OkHttpClient.Builder()
        .proxy(proxy)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, dataToSend?.toRequestBody())
            .url(url)
            .addHeader("User-Agent", Context.USER_AGENT)

        headers.forEach { (headerName, headerValueList) ->
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                headerValueList.forEach { headerValue ->
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            response.close()

            throw ReCaptchaException("NewPipe in Environment reCaptcha Challenge requested", url)
        }

        val responseBodyToReturn = response.body?.string()

        val latestUrl = response.request.url.toString()
        return Response(response.code, response.message, response.headers.toMultimap(), responseBodyToReturn, latestUrl)
    }

}

object NewPipeUtils {

    init {
        NewPipe.init(NewPipeDownloaderImpl(Environment.proxy))
    }

    fun getSignatureTimestamp(videoId: String): Result<Int> = runCatching {
        YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
    }

    /**
     * Transforme un [PlayerResponse.StreamingData.Format] en URL réellement jouable.
     *
     * Deux obstacles côté YouTube : soit l'URL est fournie telle quelle, soit elle arrive sous forme
     * de `signatureCipher` dont la signature doit être déchiffrée. Dans les deux cas le paramètre `n`
     * doit ensuite être déchiffré lui aussi, faute de quoi YouTube bride le débit à ~50 ko/s.
     */
    fun getStreamUrl(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): Result<String> = runCatching {
        val url = format.url ?: format.signatureCipher?.let { cipher ->
            val params = cipher.split("&")
                .mapNotNull { it.split("=", limit = 2).takeIf { p -> p.size == 2 } }
                .associate { (k, v) -> k to URLDecoder.decode(v, "UTF-8") }

            val baseUrl = params["url"] ?: error("signatureCipher sans paramètre url")
            val obfuscated = params["s"] ?: error("signatureCipher sans paramètre s")
            val sigParam = params["sp"] ?: "signature"
            val signature = YoutubeJavaScriptPlayerManager.deobfuscateSignature(videoId, obfuscated)

            "$baseUrl&$sigParam=$signature"
        } ?: error("Format sans url ni signatureCipher (itag ${format.itag})")

        // Le déchiffrement du paramètre `n` casse dès que YouTube modifie son player JS. Sans lui on
        // récupère quand même un flux jouable, simplement bridé — c'est très préférable à ne rien
        // jouer du tout. On ne laisse donc pas cet échec-là condamner une URL par ailleurs valide.
        runCatching {
            YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url)
        }.getOrElse { url }
    }

}