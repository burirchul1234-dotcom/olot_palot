package com.olotpalot.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.olotpalot.core.design.components.DisplayText
import com.olotpalot.core.design.components.Eyebrow
import com.olotpalot.core.design.components.LoadingBlocks
import com.olotpalot.core.design.components.PrimaryButton
import com.olotpalot.core.design.components.TextAction
import com.olotpalot.core.design.components.enterRise
import com.olotpalot.core.design.theme.OPShapes
import com.olotpalot.core.design.theme.OPTheme
import com.olotpalot.domain.model.Taxon
import com.olotpalot.domain.model.TaxonKind
import com.olotpalot.domain.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(content: ContentRepository) : ViewModel() {
    val moods: StateFlow<List<Taxon>> = content.observeTaxa(TaxonKind.MOOD)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/**
 * First launch: three taps that make Home yours before you've read a word.
 * Nothing is required — Skip keeps the editorial defaults.
 */
@Composable
fun OnboardingScreen(onDone: (Set<String>) -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val moods by viewModel.moods.collectAsStateWithLifecycle()
    val c = OPTheme.colors
    var chosen by rememberSaveable { mutableStateOf(setOf<String>()) }

    Column(
        Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = OPTheme.spacing.gutter),
    ) {
        Spacer(Modifier.height(48.dp))
        Column(Modifier.enterRise(0)) {
            Text("Olot Palot", style = OPTheme.type.wordmark.copy(fontSize = 40.sp, lineHeight = 44.sp), color = c.ink)
            Text("ওলট পালট", style = OPTheme.type.title, color = c.accent)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "Stories you can listen to, read, and sometimes watch — in English and বাংলা.",
            style = OPTheme.type.body, color = c.inkMuted, modifier = Modifier.enterRise(1),
        )
        Spacer(Modifier.height(40.dp))
        Column(Modifier.enterRise(2)) {
            Eyebrow("One question", color = c.accent)
            Spacer(Modifier.height(8.dp))
            DisplayText("What are you in the mood for?", style = OPTheme.type.headline)
            Spacer(Modifier.height(6.dp))
            Text("Pick any that feel right. You can change this later.", style = OPTheme.type.bodySmall, color = c.inkMuted)
        }
        Spacer(Modifier.height(20.dp))
        if (moods.isEmpty()) {
            LoadingBlocks(rows = 3)
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                moods.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { mood ->
                            val on = mood.id in chosen
                            MoodTile(mood.name, MoodColors.of(mood.id), selected = on, modifier = Modifier.weight(1f)) {
                                chosen = if (on) chosen - mood.id else chosen + mood.id
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton(if (chosen.isEmpty()) "Start exploring" else "Start with ${chosen.size} mood${if (chosen.size > 1) "s" else ""}", onClick = { onDone(chosen) }, modifier = Modifier.weight(1f))
            Spacer(Modifier.height(0.dp).padding(8.dp))
            TextAction("Skip", onClick = { onDone(emptySet()) }, color = c.inkMuted)
        }
        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}

/** A mood as a tinted tile: colour is the identity, the word is the label. */
@Composable
fun MoodTile(name: String, color: Color, selected: Boolean, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 64.dp, onClick: () -> Unit) {
    val c = OPTheme.colors
    val bg = if (selected) Brush.linearGradient(listOf(color, color.copy(alpha = 0.88f)))
    else Brush.linearGradient(listOf(color.copy(alpha = 0.26f), color.copy(alpha = 0.14f)))
    Box(
        modifier.height(height).clip(OPShapes.tile).background(bg)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        DisplayText(name, style = OPTheme.type.title, color = if (selected) Color(0xFFFFFDF8) else c.ink, maxLines = 1)
        if (selected) {
            Box(Modifier.align(Alignment.CenterEnd).size(22.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xFFFFFDF8)), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Check, null, Modifier.size(14.dp), tint = color)
            }
        }
    }
}
