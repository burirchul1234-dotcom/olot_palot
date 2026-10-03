package com.olotpalot.feature.story

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.olotpalot.core.common.AnalyticsEvent
import com.olotpalot.core.common.AnalyticsTracker
import com.olotpalot.core.common.AppError
import com.olotpalot.core.common.AppResult
import com.olotpalot.core.design.components.DisplayText
import com.olotpalot.core.design.components.EmptyState
import com.olotpalot.core.design.components.Eyebrow
import com.olotpalot.core.design.components.IconButtonPlain
import com.olotpalot.core.design.components.PrimaryButton
import com.olotpalot.core.design.components.Rule
import com.olotpalot.core.design.components.TextAction
import com.olotpalot.core.design.theme.OPShapes
import com.olotpalot.core.design.theme.OPTheme
import com.olotpalot.core.navigation.CommentsRoute
import com.olotpalot.domain.model.Comment
import com.olotpalot.domain.repository.CommentRepository
import com.olotpalot.domain.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommentsUiState(
    val title: String = "",
    val comments: List<Comment> = emptyList(),
    val loaded: Boolean = false,
    val posting: Boolean = false,
    val editing: Comment? = null,
)

@HiltViewModel
class CommentsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val comments: CommentRepository,
    private val content: ContentRepository,
    private val analytics: AnalyticsTracker,
) : ViewModel() {
    val contentId: String = savedStateHandle.toRoute<CommentsRoute>().contentId
    private val posting = MutableStateFlow(false)
    private val editing = MutableStateFlow<Comment?>(null)
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val state: StateFlow<CommentsUiState> = combine(
        comments.observeComments(contentId), content.observeContent(contentId), posting, editing,
    ) { list, item, busy, edit ->
        CommentsUiState(title = item?.title.orEmpty(), comments = list, loaded = true, posting = busy, editing = edit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CommentsUiState())

    fun submit(text: String, onDone: () -> Unit) {
        if (posting.value) return
        viewModelScope.launch {
            posting.value = true
            val target = editing.value
            val result = if (target != null) comments.edit(target.id, text) else comments.create(contentId, text)
            posting.value = false
            when (result) {
                is AppResult.Success -> {
                    if (target == null) analytics.track(AnalyticsEvent.COMMENT_CREATED, "contentId" to contentId)
                    editing.value = null
                    onDone()
                }
                is AppResult.Failure -> _messages.send((result.error as? AppError.Message)?.text ?: "Couldn't post that.")
            }
        }
    }

    fun startEdit(c: Comment) { editing.value = c }
    fun cancelEdit() { editing.value = null }
    fun delete(c: Comment) = viewModelScope.launch {
        when (val r = comments.delete(c.id)) {
            is AppResult.Success -> _messages.send("Comment removed")
            is AppResult.Failure -> _messages.send((r.error as? AppError.Message)?.text ?: "Couldn't remove that.")
        }
    }
}

@Composable
fun CommentsScreen(onBack: () -> Unit, viewModel: CommentsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = OPTheme.colors
    val gutter = OPTheme.spacing.gutter
    val snackbar = remember { SnackbarHostState() }
    var draft by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(state.editing) { state.editing?.let { draft = it.body } }

    Column(Modifier.fillMaxSize().background(c.background).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = gutter, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButtonPlain(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
            Column(Modifier.weight(1f)) {
                Eyebrow("Comments")
                DisplayText(state.title, style = OPTheme.type.titleSmall, maxLines = 1)
            }
        }
        Rule()

        Box(Modifier.weight(1f)) {
            when {
                !state.loaded -> Unit
                state.comments.isEmpty() -> Box(Modifier.padding(horizontal = gutter)) {
                    EmptyState("No comments yet", "Start the conversation below.")
                }
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = gutter, vertical = 8.dp)) {
                    items(state.comments.size, key = { state.comments[it].id }) { i ->
                        val comment = state.comments[i]
                        Column(Modifier.padding(vertical = 10.dp)) {
                            CommentPreview(comment)
                            if (comment.isMine) {
                                Spacer(Modifier.height(4.dp))
                                Row {
                                    TextAction("Edit", { viewModel.startEdit(comment) }, color = c.inkMuted)
                                    Spacer(Modifier.width(12.dp))
                                    TextAction("Delete", { viewModel.delete(comment) }, color = c.inkMuted)
                                }
                            }
                        }
                        if (i < state.comments.lastIndex) Rule()
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }

        Rule()
        Column(
            Modifier.fillMaxWidth().background(c.surface).padding(horizontal = gutter, vertical = 12.dp)
                .windowInsetsPadding(WindowInsets.navigationBars).windowInsetsPadding(WindowInsets.ime),
        ) {
            if (state.editing != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Eyebrow("Editing your comment", color = c.accent)
                    Spacer(Modifier.weight(1f))
                    TextAction("Cancel", { viewModel.cancelEdit(); draft = "" }, color = c.inkMuted)
                }
                Spacer(Modifier.height(6.dp))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Box(
                    Modifier.weight(1f).heightIn(min = 44.dp, max = 140.dp).clip(OPShapes.control)
                        .border(1.dp, c.rule, OPShapes.control).background(c.background).padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    if (draft.isEmpty()) Text("Say something about this story", style = OPTheme.type.bodySmall, color = c.inkFaint)
                    BasicTextField(
                        value = draft, onValueChange = { if (it.length <= 1000) draft = it },
                        textStyle = OPTheme.type.bodySmall.copy(color = c.ink), cursorBrush = SolidColor(c.accent),
                        modifier = Modifier.fillMaxWidth(), maxLines = 6,
                    )
                }
                Spacer(Modifier.width(8.dp))
                PrimaryButton(if (state.editing != null) "Save" else "Post", enabled = draft.isNotBlank() && !state.posting, onClick = {
                    viewModel.submit(draft) { draft = "" }
                })
            }
        }
    }
}
