package com.olotpalot.feature.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.core.net.toUri
import com.olotpalot.core.common.AnalyticsEvent
import com.olotpalot.core.common.AnalyticsTracker
import com.olotpalot.core.common.ApplicationScope
import com.olotpalot.core.common.Clock
import com.olotpalot.domain.model.Chapter
import com.olotpalot.domain.model.ConsumptionMode
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.MediaAsset
import com.olotpalot.domain.model.MediaProgress
import com.olotpalot.domain.repository.ContentRepository
import com.olotpalot.domain.repository.PreferencesRepository
import com.olotpalot.domain.repository.ProgressRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackState(
    val item: ContentItem? = null,
    val chapters: List<Chapter> = emptyList(),
    val assets: List<MediaAsset> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val buffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1f,
    val sleepRemainingMs: Long? = null,
    val error: String? = null,
) {
    val isActive: Boolean get() = item != null
    val currentTitle: String get() = chapters.getOrNull(assets.getOrNull(currentIndex)?.chapterIndex ?: -1)?.title ?: item?.subtitle ?: ""
    val hasChapters: Boolean get() = assets.size > 1
    /** Overall fraction across all chapter assets, for rings and the mini-player. */
    val overallFraction: Float
        get() {
            val total = assets.sumOf { it.durationMs }
            if (total <= 0) return if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
            val prefix = assets.take(currentIndex).sumOf { it.durationMs }
            val within = if (durationMs > 0 && assets.getOrNull(currentIndex)?.durationMs ?: 0 > 0)
                positionMs.toFloat() / durationMs * assets[currentIndex].durationMs else 0f
            return ((prefix + within) / total).coerceIn(0f, 1f)
        }
}

/**
 * The one audio player in the process. Screens bind to it; leaving the Listen
 * screen never stops playback, and the mini-player controls it from anywhere.
 * A MediaSessionService will wrap this same instance for lock-screen controls.
 */
@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext context: Context,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val preferences: PreferencesRepository,
    private val analytics: AnalyticsTracker,
    private val appScope: ApplicationScope,
    private val clock: Clock,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), true)
        .setHandleAudioBecomingNoisy(true)
        .build()

    private var sleepJob: Job? = null
    private var tickJob: Job? = null
    private var completed = false
    private var started = false

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                val id = _state.value.item?.id ?: return
                if (isPlaying && !started) { started = true; analytics.track(AnalyticsEvent.LISTEN_STARTED, "contentId" to id) }
                if (!isPlaying) scope.launch { persist() }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                _state.update { it.copy(buffering = playbackState == Player.STATE_BUFFERING) }
                val id = _state.value.item?.id ?: return
                if (playbackState == Player.STATE_ENDED && !completed) {
                    completed = true
                    scope.launch {
                        progress.markCompleted(id, ConsumptionMode.LISTEN)
                        analytics.track(AnalyticsEvent.LISTEN_COMPLETED, "contentId" to id)
                    }
                }
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _state.update { it.copy(currentIndex = player.currentMediaItemIndex) }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _state.update { it.copy(error = "Couldn't play this right now. Check your connection and try again.") }
            }
        })
    }

    /** Loads [contentId] (resuming saved position) and starts playing. No-op if it is already loaded. */
    fun load(contentId: String, autoPlay: Boolean = true) {
        if (_state.value.item?.id == contentId) { if (autoPlay && !player.isPlaying) player.play(); return }
        scope.launch {
            persist()
            content.ensureSeeded()
            val item = content.getContent(contentId)
            val assets = item?.audioAssets.orEmpty()
            if (item == null || assets.isEmpty()) { _state.update { PlaybackState(error = "This story has no audio yet.") }; return@launch }
            val chapters = content.getChapters(contentId)
            val speed = preferences.preferences.first().playbackSpeed
            val saved = progress.audio(contentId)
            val items = assets.map { a ->
                MediaItem.Builder().setUri(a.url.toUri()).setMediaId(a.id)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(chapters.getOrNull(a.chapterIndex ?: -1)?.title ?: item.title)
                            .setAlbumTitle(item.title).setArtist(item.author.name).setArtworkUri(item.thumbnailUrl.toUri()).build(),
                    ).build()
            }
            val startIndex = saved?.let { s -> assets.indexOfFirst { it.id == s.assetId }.takeIf { it >= 0 } } ?: 0
            val prefix = assets.take(startIndex).sumOf { it.durationMs }
            val startPos = saved?.let { (it.positionMs - prefix).coerceAtLeast(0) } ?: 0L
            completed = false; started = false
            _state.value = PlaybackState(item = item, chapters = chapters, assets = assets, currentIndex = startIndex, speed = speed)
            player.setMediaItems(items, startIndex, startPos)
            player.playbackParameters = PlaybackParameters(speed)
            player.prepare()
            if (autoPlay) player.play()
            startTicking()
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            var sinceSave = 0L
            while (isActive) {
                delay(500)
                val pos = player.currentPosition.coerceAtLeast(0)
                val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
                _state.update { it.copy(positionMs = pos, durationMs = dur) }
                if (player.isPlaying) { sinceSave += 500; if (sinceSave >= 5_000) { sinceSave = 0; persist() } }
            }
        }
    }

    fun togglePlay() { if (player.isPlaying) player.pause() else { if (player.playbackState == Player.STATE_ENDED) player.seekTo(0, 0); player.play() } }
    fun seekTo(fraction: Float) { val d = player.duration; if (d > 0) player.seekTo((d * fraction).toLong()) }
    fun skip(ms: Long) { player.seekTo((player.currentPosition + ms).coerceIn(0, player.duration.coerceAtLeast(0))) }
    fun next() { if (player.hasNextMediaItem()) player.seekToNextMediaItem() }
    fun previous() { if (player.currentPosition > 3_000 || !player.hasPreviousMediaItem()) player.seekTo(0) else player.seekToPreviousMediaItem() }
    fun goTo(index: Int) { if (index in 0 until player.mediaItemCount) { player.seekTo(index, 0); player.play() } }
    fun retry() { _state.update { it.copy(error = null) }; player.prepare(); player.play() }

    fun setSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed)
        _state.update { it.copy(speed = speed) }
        scope.launch { preferences.setPlaybackSpeed(speed) }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        if (minutes == null) { _state.update { it.copy(sleepRemainingMs = null) }; return }
        sleepJob = scope.launch {
            var remaining = minutes * 60_000L
            while (remaining > 0) { _state.update { it.copy(sleepRemainingMs = remaining) }; delay(1_000); remaining -= 1_000 }
            player.pause()
            _state.update { it.copy(sleepRemainingMs = null) }
        }
    }

    /** Stops and clears the player (mini-player dismissed). Progress is kept. */
    fun stop() {
        appScope.scope.launch { withContext(kotlinx.coroutines.NonCancellable) { persistSnapshot(snapshot()) } }
        tickJob?.cancel(); sleepJob?.cancel()
        player.stop(); player.clearMediaItems()
        _state.value = PlaybackState()
    }

    private fun snapshot(): MediaProgress? {
        val st = _state.value
        val item = st.item ?: return null
        if (st.assets.isEmpty()) return null
        val index = player.currentMediaItemIndex.coerceIn(0, st.assets.lastIndex)
        val prefix = st.assets.take(index).sumOf { it.durationMs }
        val total = st.assets.sumOf { it.durationMs }.takeIf { it > 0 } ?: player.duration.coerceAtLeast(1)
        return MediaProgress(item.id, st.assets[index].id, (prefix + player.currentPosition.coerceAtLeast(0)).coerceAtMost(total), total, clock.now())
    }

    private suspend fun persist() = persistSnapshot(snapshot())
    private suspend fun persistSnapshot(p: MediaProgress?) { if (p != null) progress.saveAudio(p) }
}
