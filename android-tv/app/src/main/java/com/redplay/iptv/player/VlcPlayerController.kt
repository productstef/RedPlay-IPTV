package com.redplay.iptv.player

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.redplay.iptv.data.PlaybackRequest
import com.redplay.iptv.data.PlayerSnapshot
import com.redplay.iptv.data.TrackOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.VLCVideoLayout

class VlcPlayerController(context: Context) {
    private val appContext = context.applicationContext
    private val libVlc = LibVLC(appContext, arrayListOf("--network-caching=1500", "--clock-jitter=0", "--clock-synchro=0"))
    private val player = MediaPlayer(libVlc)
    private var attachedView: VLCVideoLayout? = null
    private var currentRequest: PlaybackRequest? = null
    private var lastVolume = 80
    private var subtitleSize = 40
    private val _snapshot = MutableStateFlow(PlayerSnapshot())
    val snapshot: StateFlow<PlayerSnapshot> = _snapshot.asStateFlow()

    init {
        player.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Opening, MediaPlayer.Event.Buffering -> update(loading = true)
                MediaPlayer.Event.Playing -> {
                    update(playing = true, loading = false, error = "")
                    refreshTracks()
                }
                MediaPlayer.Event.Paused, MediaPlayer.Event.Stopped, MediaPlayer.Event.EndReached -> update(playing = false, loading = false)
                MediaPlayer.Event.EncounteredError -> update(playing = false, loading = false, error = "Playback error")
                MediaPlayer.Event.TimeChanged, MediaPlayer.Event.LengthChanged, MediaPlayer.Event.PositionChanged -> {
                    update(timeMillis = player.time.coerceAtLeast(0L), lengthMillis = player.length.coerceAtLeast(0L))
                }
                MediaPlayer.Event.ESAdded, MediaPlayer.Event.ESSelected -> refreshTracks()
            }
        }
    }

    fun attach(view: VLCVideoLayout) {
        if (attachedView === view) return
        runCatching { if (attachedView != null) player.detachViews() }
        attachedView = view
        view.post {
            if (attachedView === view) runCatching { player.attachViews(view, null, true, false) }
        }
    }

    fun detach(view: VLCVideoLayout) {
        if (attachedView !== view) return
        runCatching { player.detachViews() }
        attachedView = null
    }

    fun play(request: PlaybackRequest) {
        if (currentRequest?.key == request.key) return
        currentRequest = request
        val media = Media(libVlc, Uri.parse(request.url))
        media.setHWDecoderEnabled(true, false)
        media.addOption(":network-caching=${if (request.isLive) 1500 else 800}")
        media.addOption(":freetype-rel-fontsize=$subtitleSize")
        media.addOption(":subsdec-encoding=UTF-8")
        request.headers.forEach { (key, value) ->
            when (key.lowercase()) {
                "user-agent" -> media.addOption(":http-user-agent=$value")
                "referer", "referrer" -> media.addOption(":http-referrer=$value")
                "cookie" -> media.addOption(":http-cookie=$value")
                else -> media.addOption(":http-header=$key: $value")
            }
        }
        player.media = media
        media.release()
        player.volume = _snapshot.value.volume
        player.play()
        request.localSubtitlePath?.let { path ->
            // Missing sidecar subtitles are intentionally non-fatal. This is only called
            // after the library probe/download succeeded.
            runCatching { player.addSlave(IMedia.Slave.Type.Subtitle, path, true) }
        }
        update(loading = true, error = "")
    }

    fun playPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun stop() = player.stop()

    fun seekTo(fraction: Float) {
        val length = player.length
        if (length > 0) player.time = (length * fraction.coerceIn(0f, 1f)).toLong()
    }

    fun skipBy(deltaMillis: Long) {
        val length = player.length
        if (length > 0) player.time = (player.time + deltaMillis).coerceIn(0L, length)
    }

    fun setVolume(value: Int) {
        val v = value.coerceIn(0, 100)
        if (v > 0) lastVolume = v
        player.volume = v
        update(volume = v, muted = v == 0)
    }

    fun toggleMute() {
        if (_snapshot.value.volume == 0) setVolume(lastVolume.coerceAtLeast(20)) else {
            lastVolume = _snapshot.value.volume
            setVolume(0)
        }
    }

    fun selectAudio(id: Int) {
        player.audioTrack = id
        refreshTracks()
    }

    fun setSubtitleSize(value: Int) {
        subtitleSize = value.coerceIn(24, 60)
    }

    fun selectSubtitle(id: Int) {
        player.spuTrack = id
        refreshTracks()
    }

    fun release() {
        runCatching { player.stop() }
        runCatching { player.detachViews() }
        player.release()
        libVlc.release()
    }

    private fun refreshTracks() {
        val audio = player.audioTracks?.map { TrackOption(it.id, it.name ?: "Audio ${it.id}") }.orEmpty()
        val subs = player.spuTracks?.map { TrackOption(it.id, it.name ?: if (it.id == -1) "Off" else "Subtitle ${it.id}") }.orEmpty()
        _snapshot.value = _snapshot.value.copy(
            audioTracks = audio,
            selectedAudio = player.audioTrack,
            subtitleTracks = if (subs.any { it.id == -1 }) subs else listOf(TrackOption(-1, "Off")) + subs,
            selectedSubtitle = player.spuTrack,
            timeMillis = player.time.coerceAtLeast(0L),
            lengthMillis = player.length.coerceAtLeast(0L),
        )
    }

    private fun update(
        playing: Boolean = _snapshot.value.playing,
        loading: Boolean = _snapshot.value.loading,
        error: String = _snapshot.value.error,
        timeMillis: Long = _snapshot.value.timeMillis,
        lengthMillis: Long = _snapshot.value.lengthMillis,
        volume: Int = _snapshot.value.volume,
        muted: Boolean = _snapshot.value.muted,
    ) {
        _snapshot.value = _snapshot.value.copy(
            playing = playing,
            loading = loading,
            error = error,
            timeMillis = timeMillis,
            lengthMillis = lengthMillis,
            volume = volume,
            muted = muted,
        )
    }
}

@Composable
fun VlcVideoSurface(
    controller: VlcPlayerController,
    request: PlaybackRequest?,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(request?.key) { request?.let(controller::play) }
    AndroidView(
        modifier = modifier,
        factory = { context -> VLCVideoLayout(context).also(controller::attach) },
        update = { view -> controller.attach(view) },
        onRelease = { view -> controller.detach(view) },
    )
}
