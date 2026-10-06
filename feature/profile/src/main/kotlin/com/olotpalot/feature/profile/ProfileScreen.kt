package com.olotpalot.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.olotpalot.core.common.firstGrapheme
import com.olotpalot.core.design.components.DisplayText
import com.olotpalot.core.design.components.Eyebrow
import com.olotpalot.core.design.components.InlineNotice
import com.olotpalot.core.design.components.PrimaryButton
import com.olotpalot.core.design.components.Rule
import com.olotpalot.core.design.components.SectionHeader
import com.olotpalot.core.design.components.TextAction
import com.olotpalot.core.design.components.TextToggle
import com.olotpalot.core.design.theme.OPShapes
import com.olotpalot.core.design.theme.OPTheme
import com.olotpalot.core.design.theme.Palettes
import com.olotpalot.domain.model.LibrarySection
import com.olotpalot.domain.model.ThemeMode

@Composable
fun ProfileScreen(onOpenSection: (LibrarySection) -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = OPTheme.colors
    val gutter = OPTheme.spacing.gutter
    var editingName by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(viewModel::applyImageTheme) }

    Column(Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState()).padding(horizontal = gutter).padding(bottom = 32.dp)) {
        // Identity
        Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(c.ink), contentAlignment = Alignment.Center) {
                Text(state.prefs.displayName.firstGrapheme().uppercase(), style = OPTheme.type.title.copy(fontSize = 24.sp), color = c.background)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                DisplayText(state.prefs.displayName, style = OPTheme.type.headline, maxLines = 1)
                Text("Guest on this device", style = OPTheme.type.caption, color = c.inkMuted)
            }
            TextAction("Edit", { editingName = true }, color = c.inkMuted)
        }

        // Streak: the one number that's about coming back.
        if (state.prefs.streakCount >= 1) {
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth().clip(OPShapes.tile).background(c.accentSoft).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.LocalFireDepartment, null, Modifier.size(22.dp), tint = c.accent)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(if (state.prefs.streakCount > 1) "Day ${state.prefs.streakCount} in a row" else "Day one", style = OPTheme.type.label, color = c.ink)
                    Text(if (state.prefs.streakCount > 1) "Open a story tomorrow to keep the thread." else "Come back tomorrow to start a streak.", style = OPTheme.type.caption, color = c.inkMuted)
                }
            }
        }

        // Statistics: four honest numbers, no vanity.
        Spacer(Modifier.height(22.dp))
        Rule()
        Row(Modifier.fillMaxWidth()) {
            Stat(state.stats.storiesCompleted.toString(), "Stories finished", Modifier.weight(1f))
            Stat(state.stats.minutesListened.toString(), "Minutes listened", Modifier.weight(1f))
        }
        Rule()
        Row(Modifier.fillMaxWidth()) {
            Stat(state.stats.videosWatched.toString(), "Videos watched", Modifier.weight(1f))
            Stat(state.stats.saved.toString(), "Saved", Modifier.weight(1f))
        }
        Rule()

        // Appearance — theme lives here and only here.
        SectionHeader("Appearance", Modifier.padding(top = 28.dp))
        Eyebrow("Theme", color = c.inkFaint)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ThemeMode.entries.forEach { mode ->
                val pal = if (mode == ThemeMode.CUSTOM) null else Palettes.forMode(mode, null)
                val selected = state.prefs.theme == mode
                Column(
                    Modifier.clip(OPShapes.control)
                        .clickable(role = Role.RadioButton) { if (mode == ThemeMode.CUSTOM) picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) else viewModel.setTheme(mode) }
                        .semantics { this.selected = selected; contentDescription = "${mode.label} theme" }
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape)
                            .border(if (selected) 2.dp else 1.dp, if (selected) c.accent else c.rule, CircleShape)
                            .background(pal?.background ?: c.surfaceRaised),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (pal != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(pal.ink))
                                Spacer(Modifier.width(3.dp))
                                Box(Modifier.size(6.dp).clip(CircleShape).background(pal.accent))
                            }
                        } else {
                            Icon(Icons.Outlined.AddPhotoAlternate, null, Modifier.size(18.dp), tint = c.inkMuted)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(mode.label, style = OPTheme.type.caption, color = if (selected) c.ink else c.inkMuted)
                }
            }
        }
        if (state.prefs.theme == ThemeMode.CUSTOM) {
            Spacer(Modifier.height(6.dp))
            Text("Custom theme drawn from your picture. Choose another picture to change it.", style = OPTheme.type.caption, color = c.inkMuted)
        }

        Spacer(Modifier.height(22.dp))
        Eyebrow("Reading text size", color = c.inkFaint)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("A", style = OPTheme.type.caption, color = c.inkMuted)
            var local by remember(state.prefs.fontScale) { mutableStateOf(state.prefs.fontScale) }
            Slider(
                value = local, onValueChange = { local = it }, onValueChangeFinished = { viewModel.setFontScale(local) },
                valueRange = 0.85f..1.6f, steps = 5, modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                colors = SliderDefaults.colors(thumbColor = c.ink, activeTrackColor = c.ink, inactiveTrackColor = c.rule, activeTickColor = c.background, inactiveTickColor = c.inkFaint),
            )
            Text("A", style = OPTheme.type.title, color = c.ink)
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Reduce motion", style = OPTheme.type.label, color = c.ink)
                Text("Fewer animations throughout the app", style = OPTheme.type.caption, color = c.inkMuted)
            }
            Switch(
                checked = state.prefs.reducedMotion, onCheckedChange = viewModel::setReducedMotion,
                colors = SwitchDefaults.colors(checkedThumbColor = c.background, checkedTrackColor = c.ink, uncheckedThumbColor = c.inkMuted, uncheckedTrackColor = c.surface, uncheckedBorderColor = c.rule),
            )
        }

        // Listening
        SectionHeader("Listening", Modifier.padding(top = 28.dp))
        Eyebrow("Default speed", color = c.inkFaint)
        Row {
            listOf(0.8f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
                TextToggle(if (s == 1f) "1×" else "${s}×".replace(".0×", "×"), selected = state.prefs.playbackSpeed == s, onClick = { viewModel.setPlaybackSpeed(s) })
            }
        }

        // Library shortcuts
        SectionHeader("Your library", Modifier.padding(top = 28.dp))
        listOf(LibrarySection.SAVED, LibrarySection.FAVOURITES, LibrarySection.BOOKMARKS, LibrarySection.HISTORY, LibrarySection.DOWNLOADS).forEachIndexed { i, section ->
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpenSection(section) }.padding(vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(section.title, style = OPTheme.type.body, color = c.ink, modifier = Modifier.weight(1f))
                Text("→", style = OPTheme.type.body, color = c.inkFaint)
            }
            if (i < 4) Rule()
        }

        // Account
        SectionHeader("Account", Modifier.padding(top = 28.dp))
        InlineNotice("You're browsing as a guest. Everything you save, like and finish is kept on this device and will move to your account when you sign in.")
        Spacer(Modifier.height(20.dp))
        Text("Olot Palot  ·  ওলট পালট", style = OPTheme.type.caption, color = c.inkFaint)
    }

    if (editingName) {
        var draft by remember { mutableStateOf(state.prefs.displayName) }
        AlertDialog(
            onDismissRequest = { editingName = false },
            containerColor = c.surface,
            titleContentColor = c.ink,
            textContentColor = c.ink,
            shape = OPShapes.tile,
            title = { DisplayText("Your name", style = OPTheme.type.title) },
            text = {
                Box(Modifier.fillMaxWidth().clip(OPShapes.control).border(1.dp, c.rule, OPShapes.control).background(c.background).padding(12.dp)) {
                    BasicTextField(draft, { if (it.length <= 40) draft = it }, singleLine = true, textStyle = OPTheme.type.body.copy(color = c.ink), cursorBrush = SolidColor(c.accent), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { PrimaryButton("Save", { viewModel.setDisplayName(draft); editingName = false }, enabled = draft.isNotBlank()) },
            dismissButton = { TextAction("Cancel", { editingName = false }, color = c.inkMuted) },
        )
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 14.dp)) {
        Text(value, style = OPTheme.type.numeral.copy(fontSize = 30.sp, lineHeight = 30.sp), color = OPTheme.colors.ink)
        Spacer(Modifier.height(4.dp))
        Eyebrow(label)
    }
}

@Suppress("unused")
private val previewColor = Color.Transparent
