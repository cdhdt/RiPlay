package player

data class PlayerState(
    val isPlaying: Boolean = false,
    val isMuted: Boolean = false,
    val volume: Float = .5f,
    val timestamp: Long = 0L,
    val duration: Long = 0L,
    /** True while an ad is being skipped: timestamp is seeked to the ad's end, so it is NOT a track end. */
    val adShowing: Boolean = false,
)