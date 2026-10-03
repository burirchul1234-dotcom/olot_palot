package com.olotpalot.feature.player

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.toRoute
import com.olotpalot.core.common.AnalyticsEvent
import com.olotpalot.core.common.AnalyticsTracker
import com.olotpalot.core.common.ApplicationScope
import com.olotpalot.core.common.Clock
import com.olotpalot.core.navigation.PlayerRoute
import com.olotpalot.domain.model.ConsumptionMode
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.model.MediaProgress
import com.olotpalot.domain.model.UserContentState
import com.olotpalot.domain.repository.ContentRepository
import com.olotpalot.domain.repository.EngagementRepository
import com.olotpalot.domain.repository.ProgressRepository
import com.olotpalot.domain.usecase.ContentLinks
import com.olotpalot.domain.usecase.ObserveRelatedUseCase
import com.olotpalot.domain.usecase.RecordShareUseCase
import com.olotpalot.domain.usecase.ToggleLikeUseCase
import com.olotpalot.domain.usecase.ToggleSavedForLaterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import javax.inject.Inject

data class WatchPlayerUiState(
    val item: ContentItem? = null,
    val user: UserContentState? = null,
    val related: List<ContentItem> = emptyList(),
    val isPlaying: Boolean = false,
    val error: String? = null,
    val ready: Boolean = false,
    val notFound: Boolean = false,
)

@HiltViewModel
class WatchPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val engagement: EngagementRepository,
    observeRelated: ObserveRelatedUseCase,
    private val toggleLike: ToggleLikeUseCase,
    private val toggleSaved: ToggleSavedForLaterUseCase,
    private val recordShare: RecordShareUseCase,
    private val analytics: AnalyticsTracker,
    private val appScope: ApplicationScope,
    private val clock: Clock,
) : ViewModel() {

    val contentId: String = savedStateHandle.toRoute<PlayerRoute>().contentId
    private val _state = MutableStateFlow(WatchPlayerUiState())
    val state: StateFlow<WatchPlayerUiState> = _state
    private val _share = Channel<Pair<String, String>>(Channel.BUFFERED)
    val share = _share.receiveAsFlow()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
        .setHandleAudioBecomingNoisy(true)
        .build()

    private var assetId: String = ""
    private var completed = false
    private var started = false

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying && !started) { started = true; analytics.track(AnalyticsEvent.VIDEO_STARTED, "contentId" to contentId) }
            if (!isPlaying) viewModelScope.launch { persist() }
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED && !completed) {
                completed = true
                viewModelScope.launch {
                    progress.markCompleted(contentId, ConsumptionMode.WATCH)
                    analytics.track(AnalyticsEvent.VIDEO_COMPLETED, "contentId" to contentId)
                }
            }
        }
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _state.update { it.copy(error = "Couldn't play this video right now.") }
        }
    }

    init {
        player.addListener(listener)
        viewModelScope.launch {
            content.ensureSeeded()
            val item = content.getContent(contentId)
            val asset = item?.videoAsset
            if (item == null || asset == null) { _state.update { it.copy(notFound = true) }; return@launch }
            assetId = asset.id
            val saved = progress.video(contentId)
            _state.update { it.copy(item = item, ready = true) }
            player.setMediaItem(
                MediaItem.Builder().setUri(asset.url.toUri()).setMediaId(asset.id)
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(item.title).setArtist(item.author.name).build()).build(),
                saved?.positionMs ?: 0L,
            )
            player.prepare()
            player.play()
            engagement.recordOpen(contentId)
            launch {
                var sinceSave = 0L
                while (isActive) { delay(1_000); if (player.isPlaying) { sinceSave += 1_000; if (sinceSave >= 5_000) { sinceSave = 0; persist() } } }
            }
        }
        viewModelScope.launch {
            combine(
                content.observeContent(contentId).filterNotNull(),
                engagement.observeState(contentId),
                content.observeContent(contentId).filterNotNull().flatMapLatest { observeRelated(it, limit = 6) },
            ) { item, user, related -> Triple(item, user, related) }.collect { (item, user, related) ->
                _state.update { it.copy(item = item, user = user, related = related.filter { r -> r.supports(ConsumptionMode.WATCH) }) }
            }
        }
    }

    fun onLike() { val u = _state.value.user ?: return; viewModelScope.launch { toggleLike(contentId, u.isLiked) } }
    fun onSave() { val u = _state.value.user ?: return; viewModelScope.launch { toggleSaved(contentId, u.isSavedForLater) } }
    fun onShare() {
        val item = _state.value.item ?: return
        viewModelScope.launch { recordShare(contentId); _share.send(item.title to "${item.title} by ${item.author.name}\n${ContentLinks.webLink(contentId)}") }
    }
    fun retry() { _state.update { it.copy(error = null) }; player.prepare(); player.play() }

    private fun snapshot(): MediaProgress? {
        if (!_state.value.ready) return null
        val d = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: _state.value.item?.videoDurationMs ?: return null
        return MediaProgress(contentId, assetId, player.currentPosition.coerceIn(0, d), d, clock.now())
    }

    private suspend fun persist() { snapshot()?.let { progress.saveVideo(it) } }

    override fun onCleared() {
        val p = snapshot()
        player.removeListener(listener)
        player.release()
        if (p != null) appScope.scope.launch { withContext(NonCancellable) { progress.saveVideo(p) } }
    }
}
