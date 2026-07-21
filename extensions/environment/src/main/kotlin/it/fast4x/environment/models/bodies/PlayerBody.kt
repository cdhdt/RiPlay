package it.fast4x.environment.models.bodies

import it.fast4x.environment.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class PlayerBody(
    val context: Context = Context.DefaultWeb,
    val videoId: String,
    val playlistId: String? = null,
    val contentCheckOk: Boolean = true,
    val racyCheckOk: Boolean = true,
    val playbackContext: PlaybackContext? = null,
    val cpn: String? = "dPK7AEPTvFz8geNI",
    val params: String? = null,
    // Was a hardcoded, long-expired PO token sent on every request, which made YouTube answer
    // LOGIN_REQUIRED for the ANDROID_VR client. A fresh token needs a JS runtime to generate, and
    // ANDROID_VR needs none at all, so we simply send nothing. Pass one explicitly if ever needed.
    val serviceIntegrityDimensions: ServiceIntegrityDimensions? = null,
) {
    @Serializable
    data class ServiceIntegrityDimensions(
        val poToken: String,
    )
    @Serializable
    data class PlaybackContext(
        val contentPlaybackContext: ContentPlaybackContext = ContentPlaybackContext(),
    ) {
        @Serializable
        data class ContentPlaybackContext(
            val html5Preference: String = "HTML5_PREF_WANTS",
            val signatureTimestamp: Int = 20110,
        )
    }
}
