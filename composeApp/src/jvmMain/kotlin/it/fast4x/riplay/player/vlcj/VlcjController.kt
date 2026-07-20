package vlcj

import exception.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import it.fast4x.riplay.player.vlcj.VlcNative
import player.PlayerController
import player.PlayerState
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer

class VlcjController : PlayerController {

    init {
        // Was pointing JNA at a hardcoded user.dir/lib/libvlc.dll — a file where a directory is
        // expected, and Windows-only. Only the discovery call below it ever worked.
        if (!VlcNative.available) println(VlcNative.missingMessage())
    }

    internal val factory by lazy { MediaPlayerFactory(*VlcNative.factoryArgs) }

    internal var player: EmbeddedMediaPlayer? = null
        private set

    private val stateListener = object : MediaPlayerEventAdapter() {
        override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
            catch { mediaPlayer.audio().setVolume(50) }
            _state.update { it.copy(duration = mediaPlayer.status().length()) }
        }

        override fun playing(mediaPlayer: MediaPlayer) {
            _state.update { it.copy(isPlaying = true) }
        }

        override fun paused(mediaPlayer: MediaPlayer) {
            _state.update { it.copy(isPlaying = false) }
        }

        override fun stopped(mediaPlayer: MediaPlayer) {
            _state.update { it.copy(isPlaying = false) }
        }

        override fun finished(mediaPlayer: MediaPlayer) {
            _state.update { it.copy(isPlaying = false) }
        }

        override fun muted(mediaPlayer: MediaPlayer, muted: Boolean) {
            _state.update { it.copy(isMuted = muted) }
        }

        override fun volumeChanged(mediaPlayer: MediaPlayer, volume: Float) {
            _state.update { it.copy(volume = volume) }
        }

        override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
            _state.update { it.copy(timestamp = newTime) }
        }
    }

    private val _state = MutableStateFlow(PlayerState())

    override val state: StateFlow<PlayerState>
        get() = _state.asStateFlow()

    override fun load(url: String) = catch {
        player = factory.mediaPlayers()?.newEmbeddedMediaPlayer()?.apply {
            events()?.addMediaPlayerEventListener(stateListener)
            media()?.prepare(url)
        }
    }

    override fun play() = catch {
        player?.controls()?.play()
    }

    override fun pause() = catch {
        player?.controls()?.setPause(true)
    }

    override fun stop() = catch {
        player?.controls()?.stop()
    }

    override fun dispose() = catch {
        player?.run {
            controls().stop()
            events().removeMediaPlayerEventListener(stateListener)
            release()
        }
    }

    override fun seekTo(timestamp: Long) = catch {
        player?.controls()?.setTime(timestamp)
    }

    override fun setVolume(value: Float) = catch {
        player?.audio()?.setVolume((value * 100).toInt().coerceIn(0..100))
    }

    override fun toggleSound() = catch {
        player?.audio()?.mute()
    }
}