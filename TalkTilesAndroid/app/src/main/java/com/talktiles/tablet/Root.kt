package com.talktiles.tablet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Every screen this app can present on top of the board. One sheet at a time. */
sealed class RootSheet {
    data class Tile(val slot: Int) : RootSheet()
    data class KeyboardKey(val wordId: String) : RootSheet()
    data class Hotspot(val spot: HotspotModel) : RootSheet()
    object PageOptions : RootSheet()
    object PageWizard : RootSheet()
    object Gallery : RootSheet()
    object Find : RootSheet()
    object Phrases : RootSheet()
    object Help : RootSheet()
    object Settings : RootSheet()
    object Upgrade : RootSheet()
    /** The PIN prompt, and what opens once it is right. */
    data class Pin(val then: Protected) : RootSheet()
}

/** The doors that "Protect editing" puts a PIN on. Talking is never one of them. */
enum class Protected { EDITOR, SETTINGS, LIBRARY, UPGRADE }

@Composable
fun RootView(store: AACStore) {
    var showingHome by remember { mutableStateOf(true) }
    var sheet by remember { mutableStateOf<RootSheet?>(null) }
    // A right PIN opens every protected door until the person goes back to talking.
    var unlocked by remember { mutableStateOf(false) }

    fun open(target: Protected) {
        when (target) {
            Protected.EDITOR -> { store.isEditMode = true; showingHome = false; sheet = null }
            Protected.SETTINGS -> sheet = RootSheet.Settings
            Protected.LIBRARY -> sheet = RootSheet.Gallery
            Protected.UPGRADE -> sheet = RootSheet.Upgrade
        }
    }
    fun request(target: Protected) {
        if (store.isLocked && !unlocked) sheet = RootSheet.Pin(target) else open(target)
    }
    fun startTalking() {
        unlocked = false
        store.enterPlayer()
        showingHome = false
    }

    val c = TT.colors
    // targetSdk 35+ draws edge to edge: the surface behind the status bar is the
    // top bar's own colour continuing up, and the content keeps clear of both bars.
    Box(Modifier.fillMaxSize().background(c.surface).safeDrawingPadding().background(c.canvas)) {
        if (showingHome) {
            HomeView(
                store = store,
                onStartTalking = { startTalking() },
                onEditPages = { request(Protected.EDITOR) },
                onOpenLibrary = { request(Protected.LIBRARY) },
                onOpenSettings = { request(Protected.SETTINGS) },
                onOpenHelp = { sheet = RootSheet.Help },
                onOpenPro = { request(Protected.UPGRADE) }
            )
        } else {
            // Back from the board returns to the menu rather than leaving the app.
            BackHandler { showingHome = true }
            BoardView(
                store = store,
                onGoHome = { showingHome = true },
                onSelectTile = { sheet = RootSheet.Tile(it) },
                onSelectKey = { sheet = RootSheet.KeyboardKey(it) },
                onSelectHotspot = { sheet = RootSheet.Hotspot(it) },
                onAddHotspot = {
                    // Highest id + 1, not count + 1, so a deleted hotspot's id is never reused.
                    val newId = (store.currentPage.hotspots.maxOfOrNull { it.id } ?: 0) + 1
                    val spot = HotspotModel(id = newId, label = "Spot $newId", tts = "Spot $newId")
                    store.updateCurrentPage { it.copy(hotspots = it.hotspots + spot) }
                    sheet = RootSheet.Hotspot(spot)
                },
                onOpenFind = { sheet = RootSheet.Find },
                onOpenOptions = { sheet = RootSheet.PageOptions },
                onOpenNewPage = { sheet = RootSheet.PageWizard },
                onOpenPhrases = { sheet = RootSheet.Phrases }
            )
        }
    }

    val dismiss = { sheet = null }
    when (val s = sheet) {
        null -> {}
        is RootSheet.Tile -> QuickEditSheet(store, s.slot, dismiss)
        is RootSheet.KeyboardKey -> KeyboardKeyEditorSheet(store, s.wordId, dismiss)
        is RootSheet.Hotspot -> HotspotEditorSheet(store, s.spot, dismiss)
        RootSheet.PageOptions -> PageOptionsSheet(store, dismiss)
        RootSheet.PageWizard -> PageWizardSheet(store, dismiss)
        RootSheet.Gallery -> GallerySheet(store, dismiss)
        RootSheet.Find -> FindSheet(store, dismiss, onOpenPhrases = { sheet = RootSheet.Phrases })
        RootSheet.Phrases -> PhrasesSheet(store, dismiss)
        RootSheet.Help -> HelpSheet(dismiss)
        RootSheet.Settings -> SettingsSheet(store, dismiss)
        RootSheet.Upgrade -> UpgradeSheet(store, onDismiss = dismiss)
        is RootSheet.Pin -> PinPromptSheet(store.settings.lockPIN, dismiss) { unlocked = true; open(s.then) }
    }
}

/**
 * The front door. One strong action - talking - and four quiet ones. Lays
 * out as a column on a portrait tablet and side by side in landscape; every
 * card grows with the font rather than clipping it.
 */
@Composable
fun HomeView(
    store: AACStore,
    onStartTalking: () -> Unit,
    onEditPages: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenPro: () -> Unit = {}
) {
    val c = TT.colors
    val hasHistory = store.settings.lastPageId != null || !store.sentence.isEmpty
    val primaryTitle = if (hasHistory) "Continue talking" else "Start talking"
    val openOn = store.pages.getOrNull(store.currentPageIndex)?.title
    val primarySub = when {
        !store.sentence.isEmpty -> "Your sentence is still in the bar"
        openOn != null -> "Opens on $openOn"
        else -> "Tap a button and it speaks"
    }
    val pageCount = store.pages.count { it.enabled }
    val lockNote = if (store.isLocked) "PIN protected" else null

    BoxWithConstraints(Modifier.fillMaxSize().background(c.canvas)) {
        val wide = maxWidth >= 720.dp
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = TTSpace.xl, vertical = TTSpace.l)
                .widthIn(max = 1100.dp).align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(TTSpace.l)
        ) {
            // Masthead
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TTSpace.m)) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(c.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, null, tint = c.onPrimary, modifier = Modifier.size(24.dp))
                }
                Column {
                    Text("Talk Tiles", style = TTType.title, color = c.ink)
                    Text("$pageCount pages in this book", style = TTType.caption, color = c.inkSoft)
                }
            }

            val primary: @Composable (Modifier) -> Unit = { m ->
                Column(
                    m
                        .shadow(if (c.highContrast) 0.dp else 6.dp, TTShape.large)
                        .clip(TTShape.large)
                        .background(c.primary)
                        .border(if (c.highContrast) 2.dp else 0.dp, c.ink.copy(alpha = if (c.highContrast) 1f else 0f), TTShape.large)
                        .accessibleClickable(label = primaryTitle, shape = TTShape.large, onClick = onStartTalking)
                        .padding(TTSpace.xl),
                    verticalArrangement = Arrangement.spacedBy(TTSpace.m)
                ) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(c.onPrimary.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, null, tint = c.onPrimary, modifier = Modifier.size(40.dp))
                    }
                    Text(primaryTitle, style = TTType.display, color = c.onPrimary)
                    Text(primarySub, style = TTType.body, color = c.onPrimary.copy(alpha = 0.9f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            val secondary: @Composable (Modifier) -> Unit = { m ->
                Column(m, verticalArrangement = Arrangement.spacedBy(TTSpace.m)) {
                    HomeCard("Edit pages", "Change buttons, pages and pictures", Icons.Default.Edit, lockNote, onEditPages)
                    HomeCard("Page library", "Ready-made boards to add", Icons.Default.LibraryBooks, lockNote, onOpenLibrary)
                    HomeCard("Settings", "Voice, touch, protection, backup", Icons.Default.Settings, lockNote, onOpenSettings)
                    HomeCard("Help", "How everything works", Icons.Default.HelpOutline, null, onOpenHelp)
                    // Gone once bought; Settings still shows it.
                    if (!store.pro.isPro) HomeCard("Talk Tiles Pro", store.pro.statusLine, Icons.Default.WorkspacePremium, lockNote, onOpenPro)
                }
            }

            if (wide) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TTSpace.l)) {
                    primary(Modifier.weight(1.1f).heightIn(min = 220.dp))
                    secondary(Modifier.weight(1f))
                }
            } else {
                primary(Modifier.fillMaxWidth().heightIn(min = 200.dp))
                secondary(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun HomeCard(title: String, subtitle: String, icon: ImageVector, note: String?, onClick: () -> Unit) {
    val c = TT.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .shadow(if (c.highContrast) 0.dp else 2.dp, TTShape.medium)
            .clip(TTShape.medium)
            .background(c.surface)
            .border(if (c.highContrast) 2.dp else 1.dp, if (c.highContrast) c.ink else c.line, TTShape.medium)
            .accessibleClickable(label = title, shape = TTShape.medium, onClick = onClick)
            .padding(horizontal = TTSpace.l, vertical = TTSpace.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TTSpace.l)
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(c.primarySoft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.primaryDeep, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = TTType.heading, color = c.ink)
            Text(subtitle, style = TTType.caption, color = c.inkSoft)
        }
        if (note != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Lock, null, tint = c.inkFaint, modifier = Modifier.size(16.dp))
                Text(note, style = TTType.caption, color = c.inkFaint)
            }
        }
    }
}
