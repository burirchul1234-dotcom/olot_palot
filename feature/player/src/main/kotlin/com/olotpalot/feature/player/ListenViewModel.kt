package com.olotpalot.feature.player

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.toRoute
import com.olotpalot.core.common.AnalyticsEvent
import com.olotpalot.core.common.AnalyticsTracker
import com.olotpalot.core.common.ApplicationScope
import com.olotpalot.core.common.Clock
import com.olotpalot.core.navigation.ListenRoute
import com.olotpalot.domain.model.Chapter
import com.olotpalot.domain.model.ConsumptionMode
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.MediaAsset
import com.olotpalot.domain.model.MediaProgress
import com.olotpalot.domain.repository.ContentRepository
import com.olotpalot.domain.repository.PreferencesRepository
import com.olotpalot.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import javax.inject.Inject

data class ListenUiState(
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
    val ready: Boolean = false,
    val notFound: Boolean = false,
) {
    val currentTitle: String get() = chapters.getOrNull(assets.getOrNull(currentIndex)?.chapterIndex ?: -1)?.title ?: item?.subtitle ?: ""
    val hasChapters: Boolean get() = assets.size > 1
}

/**
 * Milestone-1 player: ExoPlayer owned by the ViewModel, released with it.
 * Progress is persisted as an overall position across chapter assets so
 * "Continue" can show one honest fraction. Background playback with a
 * MediaSessionService replaces the ownership model in the next milestone
 * without touching this screen's contract.
 */
@HiltViewModel
class ListenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val preferences: PreferencesRepository,
    private val analytics: AnalyticsTracker,
    private val appScope: ApplicationScope,
    private val clock: Clock,
) : ViewModel() {

    val contentId: String = savedStateHandle.toRoute<ListenRoute>().contentId

    private val _state = MutableStateFlow(ListenUiState())
    val state: StateFlow<ListenUiState> = _state

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), true)
        .setHandleAudioBecomingNoisy(true)
        .build()

    private var sleepJob: Job? = null
    private var completed = false
    private var started = false

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying && !started) { started = true; analytics.track(AnalyticsEvent.LISTEN_STARTED, "contentId" to contentId) }
            if (!isPlaying) viewModelScope.launch { persist() }
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update { it.copy(buffering = playbackState == Player.STATE_BUFFERING) }
            if (playbackState == Player.STATE_ENDED && !completed) {
                completed = true
                viewModelScope.launch {
                    progress.markCompleted(contentId, ConsumptionMode.LISTEN)
                    analytics.track(AnalyticsEvent.LISTEN_COMPLETED, "contentId" to contentId)
                }
            }
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _state.update { it.copy(currentIndex = player.currentMediaItemIndex) }
        }
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _state.update { it.copy(error = "Couldn't play this right now. Check your connection and try again.") }
        }
    }

    init {
        player.addListener(listener)
        viewModelScope.launch {
            content.ensureSeeded()
            val item = content.getContent(contentId)
            val assets = item?.audioAssets.orEmpty()
            if (item == null || assets.isEmpty()) { _state.update { it.copy(notFound = true) }; return@launch }
            val chapters = content.getChapters(contentId)
            val speed = preferences.preferences.first().playbackSpeed
            val saved = progress.audio(contentId)

            val items = assets.map { a ->
                MediaItem.Builder()
                    .setUri(a.url.toUri())
                    .setMediaId(a.id)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(chapters.getOrNull(a.chapterIndex ?: -1)?.title ?: item.title)
                            .setAlbumTitle(item.title)
                            .setArtist(item.author.name)
                            .setArtworkUri(item.thumbnailUrl.toUri())
                            .build(),
                    )
                    .build()
            }
            val startIndex = saved?.let { s -> assets.indexOfFirst { it.id == s.assetId }.takeIf { it >= 0 } } ?: 0
            val prefix = assets.take(startIndex).sumOf { it.durationMs }
            val startPos = saved?.let { (it.positionMs - prefix).coerceAtLeast(0) } ?: 0L

            _state.update { it.copy(item = item, chapters = chapters, assets = assets, currentIndex = startIndex, speed = speed, ready = true) }
            player.setMediaItems(items, startIndex, startPos)
            player.playbackParameters = PlaybackParameters(speed)
            player.prepare()
            player.play()
            tick()
        }
    }

    private fun tick() = viewModelScope.launch {
        var sinceSave = 0L
        while (isActive) {
            delay(500)
            val pos = player.currentPosition.coerceAtLeast(0)
            val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
            _state.update { it.copy(positionMs = pos, durationMs = dur) }
            if (player.isPlaying) {
                sinceSave += 500
                if (sinceSave >= 5_000) { sinceSave = 0; persist() }
            }
        }
    }

    fun togglePlay() { if (player.isPlaying) player.pause() else { if (player.playbackState == Player.STATE_ENDED) player.seekTo(0, 0); player.play() } }
    fun seekTo(fraction: Float) { val d = player.duration; if (d > 0) player.seekTo((d * fraction).toLong()) }
    fun skip(ms: Long) { player.seekTo((player.currentPosition + ms).coerceIn(0, player.duration.coerceAtLeast(0))) }
    fun next() { if (player.hasNextMediaItem()) player.seekToNextMediaItem() }
    fun previous() { if (player.currentPosition > 3_000 || !player.hasPreviousMediaItem()) player.seekTo(0) else player.seekToPreviousMediaItem() }
    fun goTo(index: Int) { if (index in 0 until player.mediaItemCount) { player.seekTo(index, 0); player.play() } }

    fun setSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed)
        _state.update { it.copy(speed = speed) }
        viewModelScope.launch { preferences.setPlaybackSpeed(speed) }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        if (minutes == null) { _state.update { it.copy(sleepRemainingMs = null) }; return }
        sleepJob = viewModelScope.launch {
            var remaining = minutes * 60_000L
            while (remaining > 0) { _state.update { it.copy(sleepRemainingMs = remaining) }; delay(1_000); remaining -= 1_000 }
            player.pause()
            _state.update { it.copy(sleepRemainingMs = null) }
        }
    }

    fun retry() { _state.update { it.copy(error = null) }; player.prepare(); player.play() }

    private fun snapshot(): MediaProgress? {
        val st = _state.value
        if (!st.ready || st.assets.isEmpty()) return null
        val index = player.currentMediaItemIndex.coerceIn(0, st.assets.lastIndex)
        val prefix = st.assets.take(index).sumOf { it.durationMs }
        val total = st.assets.sumOf { it.durationMs }.takeIf { it > 0 } ?: player.duration.coerceAtLeast(1)
        val within = player.currentPosition.coerceAtLeast(0)
        return MediaProgress(contentId, st.assets[index].id, (prefix + within).coerceAtMost(total), total, clock.now())
    }

    private suspend fun persist() { snapshot()?.let { progress.saveAudio(it) } }

    override fun onCleared() {
        val p = snapshot()
        player.removeListener(listener)
        player.release()
        if (p != null) appScope.scope.launch { withContext(NonCancellable) { progress.saveAudio(p) } }
    }
}
