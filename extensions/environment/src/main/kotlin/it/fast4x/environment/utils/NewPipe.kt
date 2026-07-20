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
     * Turns a format into a playable URL. YouTube either hands out the URL directly or wraps it in
     * a `signatureCipher` that has to be deciphered first.
     */
    fun getStreamUrl(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): Result<String> = runCatching {
        val url = format.url ?: format.signatureCipher?.let { cipher ->
            val params = cipher.split("&")
                .mapNotNull { it.split("=", limit = 2).takeIf { p -> p.size == 2 } }
                .associate { (k, v) -> k to URLDecoder.decode(v, "UTF-8") }

            val baseUrl = params["url"] ?: error("signatureCipher has no url parameter")
            val obfuscated = params["s"] ?: error("signatureCipher has no s parameter")
            val sigParam = params["sp"] ?: "signature"
            val signature = YoutubeJavaScriptPlayerManager.deobfuscateSignature(videoId, obfuscated)

            "$baseUrl&$sigParam=$signature"
        } ?: error("Format has neither url nor signatureCipher (itag ${format.itag})")

        // Deciphering the `n` parameter breaks whenever YouTube changes its player JS. Skipping it
        // only throttles the stream to ~50 KB/s, which beats not playing at all.
        runCatching {
            YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url)
        }.getOrElse { url }
    }

}