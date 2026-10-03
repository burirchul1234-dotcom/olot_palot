package com.olotpalot.feature.story

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.olotpalot.core.common.AppResult
import com.olotpalot.core.navigation.StoryRoute
import com.olotpalot.domain.model.AccessDecision
import com.olotpalot.domain.model.Chapter
import com.olotpalot.domain.model.Comment
import com.olotpalot.domain.model.ConsumptionMode
import com.olotpalot.domain.model.ContentItem
import com.olotpalot.domain.repository.AccessRepository
import com.olotpalot.domain.repository.BookmarkRepository
import com.olotpalot.domain.repository.CommentRepository
import com.olotpalot.domain.repository.ContentRepository
import com.olotpalot.domain.usecase.ContentDetail
import com.olotpalot.domain.usecase.ContentLinks
import com.olotpalot.domain.usecase.ObserveContentDetailUseCase
import com.olotpalot.domain.usecase.ObserveRelatedUseCase
import com.olotpalot.domain.usecase.RateContentUseCase
import com.olotpalot.domain.usecase.RecordContentOpenedUseCase
import com.olotpalot.domain.usecase.RecordShareUseCase
import com.olotpalot.domain.usecase.ToggleFavouriteUseCase
import com.olotpalot.domain.usecase.ToggleLikeUseCase
import com.olotpalot.domain.usecase.ToggleSavedForLaterUseCase
import com.olotpalot.domain.usecase.UnlockWithRewardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoryUiState(
    val detail: ContentDetail? = null,
    val chapters: List<Chapter> = emptyList(),
    val related: List<ContentItem> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val commentCount: Int = 0,
    val bookmarkCount: Int = 0,
    val access: AccessDecision = AccessDecision.Granted,
    val notFound: Boolean = false,
    val rewardInProgress: Boolean = false,
    val rewardSheetMode: ConsumptionMode? = null,
)

sealed interface StoryEvent {
    data class Share(val subject: String, val text: String) : StoryEvent
    data class Message(val text: String) : StoryEvent
    data class Unlocked(val mode: ConsumptionMode) : StoryEvent
}

@HiltViewModel
class StoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeDetail: ObserveContentDetailUseCase,
    observeRelated: ObserveRelatedUseCase,
    comments: CommentRepository,
    bookmarks: BookmarkRepository,
    private val content: ContentRepository,
    private val access: AccessRepository,
    private val recordOpened: RecordContentOpenedUseCase,
    private val toggleLike: ToggleLikeUseCase,
    private val toggleFavourite: ToggleFavouriteUseCase,
    private val toggleSaved: ToggleSavedForLaterUseCase,
    private val rateContent: RateContentUseCase,
    private val recordShare: RecordShareUseCase,
    private val unlockWithReward: UnlockWithRewardUseCase,
) : ViewModel() {

    val contentId: String = savedStateHandle.toRoute<StoryRoute>().contentId

    private val _events = Channel<StoryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val notFound = MutableStateFlow(false)
    private val chapters = MutableStateFlow<List<Chapter>>(emptyList())
    private val rewardInProgress = MutableStateFlow(false)
    private val rewardSheetMode = MutableStateFlow<ConsumptionMode?>(null)

    private val detailFlow = observeDetail(contentId)
    private val relatedFlow = detailFlow.filterNotNull().flatMapLatest { observeRelated(it.item) }
    private val accessFlow = detailFlow.flatMapLatest { d -> if (d == null) flowOf(AccessDecision.Granted) else access.observeAccess(d.item) }

    val state: StateFlow<StoryUiState> = combine(
        combine(detailFlow, chapters, relatedFlow, accessFlow) { d, ch, rel, acc -> Core(d, ch, rel, acc) },
        comments.observeComments(contentId),
        bookmarks.observeForContent(contentId),
        notFound,
        combine(rewardInProgress, rewardSheetMode) { a, b -> a to b },
    ) { core, allComments, marks, missing, (busy, sheetMode) ->
        StoryUiState(
            detail = core.detail,
            chapters = core.chapters,
            related = core.related,
            comments = allComments.take(2),
            commentCount = allComments.size,
            bookmarkCount = marks.size,
            access = core.access,
            notFound = missing,
            rewardInProgress = busy,
            rewardSheetMode = sheetMode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StoryUiState())

    private data class Core(val detail: ContentDetail?, val chapters: List<Chapter>, val related: List<ContentItem>, val access: AccessDecision)

    init {
        viewModelScope.launch {
            content.ensureSeeded()
            val item = content.getContent(contentId)
            if (item == null) {
                notFound.value = true
            } else {
                chapters.value = content.getChapters(contentId)
                recordOpened(contentId, source = "detail")
            }
        }
    }

    fun onLike() = withState { toggleLike(contentId, it.state.isLiked) }
    fun onFavourite() = withState {
        toggleFavourite(contentId, it.state.isFavourite)
        _events.send(StoryEvent.Message(if (it.state.isFavourite) "Removed from favourites" else "Added to favourites"))
    }
    fun onSaveForLater() = withState {
        toggleSaved(contentId, it.state.isSavedForLater)
        _events.send(StoryEvent.Message(if (it.state.isSavedForLater) "Removed from your list" else "Saved for later"))
    }
    fun onRate(stars: Int) = withState { rateContent(contentId, stars) }

    fun onShare() = withState {
        recordShare(contentId)
        _events.send(StoryEvent.Share(it.item.title, "${it.item.title} by ${it.item.author.name}\n${ContentLinks.webLink(contentId)}"))
    }

    /** Called when a mode button is tapped; decides between opening and gating. */
    fun requestMode(mode: ConsumptionMode, open: (ConsumptionMode) -> Unit) {
        when (state.value.access) {
            AccessDecision.Granted -> open(mode)
            is AccessDecision.RequiresReward -> rewardSheetMode.value = mode
            is AccessDecision.RequiresPremium -> viewModelScope.launch { _events.send(StoryEvent.Message("This story is part of Premium, coming with accounts.")) }
        }
    }

    fun dismissReward() { if (!rewardInProgress.value) rewardSheetMode.value = null }

    fun watchRewardedAd() {
        val mode = rewardSheetMode.value ?: return
        if (rewardInProgress.value) return
        viewModelScope.launch {
            rewardInProgress.value = true
            when (val r = unlockWithReward(contentId)) {
                is AppResult.Success -> {
                    rewardInProgress.value = false
                    rewardSheetMode.value = null
                    _events.send(StoryEvent.Unlocked(mode))
                }
                is AppResult.Failure -> {
                    rewardInProgress.value = false
                    _events.send(StoryEvent.Message("Couldn't unlock. Please try again."))
                }
            }
        }
    }

    private fun withState(block: suspend (ContentDetail) -> Unit) {
        val d = state.value.detail ?: return
        viewModelScope.launch { block(d) }
    }
}
