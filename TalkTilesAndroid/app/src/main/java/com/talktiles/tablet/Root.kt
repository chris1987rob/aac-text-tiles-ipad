package com.talktiles.tablet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
    object Pages : RootSheet()
    object Help : RootSheet()
    object Settings : RootSheet()
    object Pin : RootSheet()
}

@Composable
fun RootView(store: AACStore) {
    var showingHome by remember { mutableStateOf(true) }
    var sheet by remember { mutableStateOf<RootSheet?>(null) }

    fun enterEditor() { store.isEditMode = true; showingHome = false }
    fun requestEditor() { if (store.isLocked) sheet = RootSheet.Pin else enterEditor() }

    Box(Modifier.fillMaxSize().background(BoardTheme.background)) {
        if (showingHome) {
            HomeView(
                onLaunchPlayer = { store.isEditMode = false; showingHome = false },
                onLaunchEditor = { requestEditor() },
                onOpenHelp = { sheet = RootSheet.Help },
                onOpenSettings = { sheet = RootSheet.Settings },
                onOpenDownloads = { sheet = RootSheet.Gallery }
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
                    val spot = HotspotModel(id = newId, label = "Hotspot $newId", tts = "Hotspot $newId")
                    store.updateCurrentPage { it.copy(hotspots = it.hotspots + spot) }
                    sheet = RootSheet.Hotspot(spot)
                },
                onOpenPages = { sheet = RootSheet.Pages },
                onOpenOptions = { sheet = RootSheet.PageOptions },
                onOpenNewPage = { sheet = RootSheet.PageWizard }
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
        RootSheet.Pages -> PagesListSheet(store, dismiss)
        RootSheet.Help -> HelpSheet(dismiss)
        RootSheet.Settings -> SettingsSheet(store, dismiss)
        RootSheet.Pin -> PinPromptSheet(store.settings.lockPIN, dismiss) { enterEditor() }
    }
}

/** The main menu, in the same pastel language as the board. */
@Composable
fun HomeView(
    onLaunchPlayer: () -> Unit,
    onLaunchEditor: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDownloads: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(BoardTheme.background)) {
        Box(Modifier.fillMaxWidth().shadow(4.dp).background(BoardTheme.bar).height(64.dp), contentAlignment = Alignment.Center) {
            Text("TALK TILES", fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.5.sp, color = BoardTheme.ink)
        }
        RainbowBand()
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp).widthIn(max = 800.dp).align(Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // The big orange Player card.
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .shadow(10.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(hexColor("#FF9A3C"), hexColor("#F5722B"))))
                    .plainClickable(onClick = onLaunchPlayer)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(Modifier.size(74.dp).clip(CircleShape).background(Color.White).border(6.dp, BoardTheme.rainbow, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, null, tint = BoardTheme.accent, modifier = Modifier.size(40.dp))
                }
                Column {
                    Text("PLAYER", fontSize = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, color = Color.White)
                    Text("Tap tiles to talk", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.9f))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HomeCard("Page Editor", "Build and change pages", Icons.Default.Edit, "#4DB2FF", Modifier.weight(1f), onLaunchEditor)
                HomeCard("Settings", "Voice, touch, child lock, backup", Icons.Default.Settings, "#9B7BFF", Modifier.weight(1f), onOpenSettings)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HomeCard("Downloads", "Ready-made boards to add", Icons.Default.Download, "#2EC98A", Modifier.weight(1f), onOpenDownloads)
                HomeCard("Help", "How everything works", Icons.Default.QuestionMark, "#FFC93C", Modifier.weight(1f), onOpenHelp)
            }
        }
    }
}

@Composable
private fun HomeCard(title: String, subtitle: String, icon: ImageVector, color: String, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .height(110.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(hexColor(color))
            .plainClickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.size(58.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = hexColor(color), modifier = Modifier.size(28.dp))
        }
        Column {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f))
        }
    }
}
