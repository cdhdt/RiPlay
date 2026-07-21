package it.fast4x.riplay.player

import it.fast4x.environment.Environment
import it.fast4x.environment.models.bodies.NextBody
import it.fast4x.environment.requests.nextPage
import it.fast4x.riplay.player.webview.CefPlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import player.PlayerController
import player.PlayerState

enum class RepeatMode { OFF, ALL, ONE }

/** One entry in the play queue. Metadata is what the UI shows; the videoId is what CEF plays. */
data class QueueItem(
    val videoId: String,
    val title: String? = null,
    val artist: String? = null,
    val thumbnailUrl: String? = null,
)

data class QueueState(
    val items: List<QueueItem> = emptyList(),
    val index: Int = -1,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
) {
    val current: QueueItem? get() = items.getOrNull(index)
    val hasNext: Boolean get() = index < items.lastIndex || repeat != RepeatMode.OFF
    val hasPrevious: Boolean get() = index > 0 || repeat != RepeatMode.OFF
}

/**
 * The queue lives here in Kotlin rather than relying on YouTube Music's own (fragile) queue: each
 * track is played by handing its videoId to the CEF audio engine. next/previous just move the index
 * and load the neighbouring track, so shuffle, repeat and autoplay-on-end are ours to control.
 */
class QueuePlayer(private val audio: CefPlayerController) : PlayerController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Underlying audio state (position, duration, playing) from the CEF controller. */
    val playback: StateFlow<PlayerState> = audio.state

    // PlayerController: lets existing transport UI (DefaultBottomBar) bind to this facade directly.
    override val state: StateFlow<PlayerState> = audio.state
    override fun load(url: String) = playNow(listOf(QueueItem(url)))
    override fun stop() = audio.stop()

    private val _queue = MutableStateFlow(QueueState())
    val queue: StateFlow<QueueState> = _queue.asStateFlow()

    /** Guards the end-of-track auto-advance so it fires once per track, not every poll tick. */
    private var advancedForVideoId: String? = null

    init {
        scope.launch {
            audio.state.collect { s -> maybeAutoAdvance(s) }
        }
    }

    /** Replace the queue and start at [startIndex] — "play this album/song now". */
    fun playNow(items: List<QueueItem>, startIndex: Int = 0) {
        if (items.isEmpty()) return
        _queue.update { it.copy(items = items, index = startIndex.coerceIn(0, items.lastIndex)) }
        loadCurrent()
    }

    /** Append tracks without disturbing what is playing. */
    fun enqueue(items: List<QueueItem>) {
        if (items.isEmpty()) return
        _queue.update { it.copy(items = it.items + items) }
    }

    fun next() {
        val q = _queue.value
        val nextIndex = when {
            q.repeat == RepeatMode.ONE -> q.index
            q.index < q.items.lastIndex -> q.index + 1
            q.repeat == RepeatMode.ALL -> 0
            else -> return
        }
        _queue.update { it.copy(index = nextIndex) }
        loadCurrent()
    }

    fun previous() {
        // Match every player: restart the track if we are past the first few seconds, else go back.
        if (playback.value.timestamp > 3000) {
            audio.seekTo(0)
            return
        }
        val q = _queue.value
        val prevIndex = when {
            q.index > 0 -> q.index - 1
            q.repeat == RepeatMode.ALL -> q.items.lastIndex
            else -> { audio.seekTo(0); return }
        }
        _queue.update { it.copy(index = prevIndex) }
        loadCurrent()
    }

    fun toggleShuffle() {
        _queue.update { state ->
            val current = state.current ?: return@update state.copy(shuffle = !state.shuffle)
            if (!state.shuffle) {
                // Shuffle the rest, keeping the current track at the front.
                val rest = state.items.filterIndexed { i, _ -> i != state.index }.shuffled()
                state.copy(items = listOf(current) + rest, index = 0, shuffle = true)
            } else {
                state.copy(shuffle = false)
            }
        }
    }

    fun cycleRepeat() {
        _queue.update {
            it.copy(repeat = when (it.repeat) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            })
        }
    }

    /** Jump to a specific queue position (used by the queue panel). */
    fun playAt(index: Int) {
        if (index !in _queue.value.items.indices) return
        _queue.update { it.copy(index = index) }
        loadCurrent()
    }

    /** Remove a queued track; keeps the same track playing, or skips if it was the one removed. */
    fun removeAt(index: Int) {
        val before = _queue.value
        if (index !in before.items.indices) return
        val playingId = before.current?.videoId
        val removedCurrent = index == before.index
        val newItems = before.items.toMutableList().apply { removeAt(index) }
        val newIndex = when {
            newItems.isEmpty() -> -1
            else -> newItems.indexOfFirst { it.videoId == playingId }
                .takeIf { it >= 0 } ?: index.coerceIn(0, newItems.lastIndex)
        }
        _queue.update { it.copy(items = newItems, index = newIndex) }
        if (removedCurrent && newIndex >= 0) loadCurrent()
    }

    /** Reorder the queue while keeping the current track playing. */
    fun move(from: Int, to: Int) {
        val before = _queue.value
        if (from !in before.items.indices || to !in before.items.indices) return
        val playingId = before.current?.videoId
        val newItems = before.items.toMutableList().apply { add(to, removeAt(from)) }
        val newIndex = newItems.indexOfFirst { it.videoId == playingId }.takeIf { it >= 0 } ?: before.index
        _queue.update { it.copy(items = newItems, index = newIndex) }
    }

    // Transport delegates to the audio engine.
    override fun play() = audio.play()
    override fun pause() = audio.pause()
    override fun seekTo(timestamp: Long) = audio.seekTo(timestamp)
    override fun setVolume(value: Float) = audio.setVolume(value)
    override fun toggleSound() = audio.toggleSound()

    override fun dispose() {
        scope.cancel()
        audio.dispose()
    }

    private fun loadCurrent() {
        val item = _queue.value.current ?: return
        advancedForVideoId = null
        audio.load(item.videoId)
    }

    /**
     * When the current track reaches its end, advance the queue ourselves — otherwise YouTube Music
     * would start its own radio. Fires once per track thanks to [advancedForVideoId].
     */
    private fun maybeAutoAdvance(s: PlayerState) {
        val current = _queue.value.current ?: return
        // Never treat an ad as a track end: the ad-skip seeks currentTime to the ad's end, so
        // timestamp ≈ duration while an ad plays. Without this the radio fires on every pre-roll.
        if (s.adShowing) return
        // Also guard the load transient (tiny positive duration with timestamp ~0), where
        // `timestamp >= duration - 1200` would be true immediately (duration - 1200 < 0).
        val nearEnd = s.timestamp > 0 && s.duration > 1200 && s.timestamp >= s.duration - 1200
        if (!nearEnd || advancedForVideoId == current.videoId) return
        advancedForVideoId = current.videoId
        when {
            _queue.value.hasNext || _queue.value.repeat == RepeatMode.ONE -> next()
            // Queue exhausted with repeat off: keep playing by starting YouTube's radio for this track.
            else -> scope.launch { startRadio(current.videoId) }
        }
    }

    /** Fetch YouTube Music's "up next" radio for [seedVideoId], append it and play on. */
    private suspend fun startRadio(seedVideoId: String) {
        val related = withContext(Dispatchers.IO) {
            Environment.nextPage(NextBody(videoId = seedVideoId, isAudioOnly = true))
                ?.getOrNull()?.itemsPage?.items.orEmpty()
                .map { QueueItem(it.key, it.title, it.authors?.joinToString(", ") { a -> a.name.orEmpty() }, it.thumbnail?.url) }
                .filter { it.videoId.isNotEmpty() && it.videoId != seedVideoId }
        }
        if (related.isEmpty()) return
        enqueue(related)
        next()
    }
}
