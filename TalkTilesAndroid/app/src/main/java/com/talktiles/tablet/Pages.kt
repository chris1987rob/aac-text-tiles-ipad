package com.talktiles.tablet

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File

/** Hands a file to the system share sheet. */
fun shareFile(context: android.content.Context, file: File, mime: String = "application/json") {
    val uri = FileProvider.getUriForFile(context, "com.talktiles.tablet.files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, file.name))
}

// MARK: - Page Options

@Composable
fun PageOptionsSheet(store: AACStore, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val page = remember { store.currentPage }
    val gridSizes = SymbolWordBank.keyCountOptions

    var title by remember { mutableStateOf(page.title) }
    var gridSize by remember { mutableStateOf(if (page.gridSize in gridSizes) page.gridSize else SymbolWordBank.nearestKeyCount(page.gridSize)) }
    var bgHex by remember { mutableStateOf(page.bgHex) }
    var express by remember { mutableStateOf(page.express) }
    var keyboardKeys by remember { mutableStateOf(SymbolWordBank.nearestKeyCount(page.keyboardKeys)) }
    var keyboardGroups by remember { mutableStateOf(page.keyboardGroups ?: SymbolWordBank.groups.map { it.id }) }
    var scenePhotoData by remember { mutableStateOf(page.sceneImageData) }

    val used = page.tiles.keys.count { it <= gridSize }
    val total = page.tiles.size
    val gridFooter = if (total > gridSize)
        "How many buttons fit on this page. This page has $total buttons - at $gridSize the last ${total - gridSize} are hidden until you make the grid bigger again. Nothing is deleted."
    else "How many buttons fit on this page. $used of $gridSize are filled in."

    fun save() {
        store.updateCurrentPage { p ->
            var next = p.copy(title = title, gridSize = gridSize, bgHex = bgHex, express = express)
            if (p.type == PageType.SCENE) next = next.copy(sceneImageData = scenePhotoData)
            if (p.type == PageType.KEYBOARD) next = next.copy(keyboardKeys = keyboardKeys, keyboardGroups = keyboardGroups)
            next
        }
    }

    ModalSheet(title = "Page Options", onDismiss = onDismiss, trailing = "Save", onTrailing = { save(); onDismiss() }) {
        FormSection("Page") {
            FormRow { PlainTextField(title, { title = it }, "Page Title", Modifier.weight(1f), fontSize = 17) }
            ColorRow("Background", bgHex) { bgHex = it }
        }
        if (page.type == PageType.GRID) {
            FormSection("Button Grid Layout", gridFooter) {
                Box(Modifier.padding(12.dp)) { SegmentedPicker(gridSizes, gridSize, { "$it" }, { gridSize = it }) }
            }
        }
        if (page.type == PageType.SCENE) {
            FormSection("Scene Picture", "Hotspots sit on top of this picture. Changing it keeps the hotspots exactly where they are.") {
                ScenePicturePicker(scenePhotoData, onPicked = { scenePhotoData = it }, onRemove = { scenePhotoData = null })
            }
        }
        if (page.type == PageType.KEYBOARD) {
            KeyboardSetupSections(keyboardKeys, { keyboardKeys = it }, keyboardGroups, { keyboardGroups = it })
        }
        FormSection("Behavior") {
            ToggleRow("Express sentence bar", express) { express = it }
        }
        FormSection(footer = "Sends this page as a file you can message, email or save - another Talk Tiles device can add it to their book.") {
            FormButton("Share This Page", tint = BoardTheme.ink, icon = Icons.Default.Share) {
                shareFile(context, BookBackup.writePage(context, store.currentPage))
            }
        }
    }
}

/** The scene picture control, shared by the wizard and Page Options: preview + source rows. */
@Composable
fun ScenePicturePicker(data: ByteArray?, onPicked: (ByteArray) -> Unit, onRemove: () -> Unit) {
    val img = PhotoCache.bitmap(data, "scene-pick")
    if (img != null) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(img.asImageBitmap(), null, Modifier.size(300.dp, 190.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, BoardTheme.line, RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(8.dp))
            Text("Preview - this is what shows on the page", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BoardTheme.slate)
            Text("${img.width} x ${img.height} picture", fontSize = 11.sp, color = hexColor("#94A3B8"))
        }
    }
    PictureSourceRows(hasPicture = data != null, maxDimension = 2048, onPicked = onPicked, onRemove = onRemove)
}

// MARK: - New Page Wizard

@Composable
fun PageWizardSheet(store: AACStore, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("New Board") }
    var type by remember { mutableStateOf(PageType.GRID) }
    var gridSize by remember { mutableStateOf(4) }
    var preset by remember { mutableStateOf("Blank") }
    var category by remember { mutableStateOf("All") }
    var scenePhotoData by remember { mutableStateOf<ByteArray?>(null) }
    var keyboardKeys by remember { mutableStateOf(SymbolWordBank.defaultKeyCount) }
    var keyboardGroups by remember { mutableStateOf(SymbolWordBank.groups.map { it.id }) }

    val visibleTemplates = if (category == "All") PageTemplateCatalog.all else PageTemplateCatalog.templates(category)

    fun create() {
        val template = PageTemplateCatalog.all.firstOrNull { it.id == preset }
        val page: PageModel = if (type == PageType.GRID && template != null) {
            template.makePage().copy(
                title = title.ifEmpty { template.title },
                gridSize = maxOf(gridSize, template.buttonCount)
            )
        } else {
            var p = PageModel(title = title, type = type, gridSize = gridSize)
            if (type == PageType.SCENE) p = p.copy(sceneImageData = scenePhotoData)
            if (type == PageType.KEYBOARD) {
                p = p.copy(keyboardKeys = keyboardKeys, keyboardGroups = keyboardGroups)
                if (title.isEmpty() || title == "New Board") p = p.copy(title = "Symbol Keyboard")
            }
            p
        }
        store.addPage(page)
    }

    ModalSheet(title = "New Page Wizard", onDismiss = onDismiss, trailing = "Create Page", onTrailing = { create(); onDismiss() }) {
        FormSection("1. Page Title & Format") {
            FormRow { PlainTextField(title, { title = it }, "Page Title", Modifier.weight(1f)) }
            Box(Modifier.padding(12.dp)) { SegmentedPicker(PageType.values().toList(), type, { it.displayName }, { type = it }) }
        }
        when (type) {
            PageType.GRID -> {
                FormSection("2. Button Grid Layout") {
                    Box(Modifier.padding(12.dp)) { SegmentedPicker(SymbolWordBank.keyCountOptions, gridSize, { "$it" }, { gridSize = it }) }
                }
                FormSection("3. Starter Content") {
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip("All", category == "All") { category = "All" }
                        for (c in PageTemplateCatalog.categories) FilterChip(c, category == c) { category = c }
                    }
                    // Not a LazyVerticalGrid: this sits inside a scrolling column.
                    val cards: List<PageTemplate?> = listOf<PageTemplate?>(null) + visibleTemplates
                    val perRow = 3
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (row in cards.chunked(perRow)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                for (t in row) {
                                    Box(Modifier.weight(1f)) { TemplateCard(t, selected = preset == (t?.id ?: "Blank")) { preset = t?.id ?: "Blank" } }
                                }
                                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
            PageType.SCENE -> FormSection("2. Scene Picture",
                if (scenePhotoData == null) "Optional - a page with no picture opens on a plain backdrop, and you can add one later from Page Options. Hotspots are placed on top of this picture."
                else "Hotspots are placed on top of this picture. You can replace it later from Page Options.") {
                ScenePicturePicker(scenePhotoData, onPicked = { scenePhotoData = it }, onRemove = { scenePhotoData = null })
            }
            PageType.KEYBOARD -> KeyboardSetupSections(keyboardKeys, { keyboardKeys = it }, keyboardGroups, { keyboardGroups = it }, stepNumber = 2)
        }
    }
}

@Composable
private fun TemplateCard(template: PageTemplate?, selected: Boolean, onClick: () -> Unit) {
    val accent = template?.accent ?: "#94A3B8"
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(hexColor("#F8FAFC"))
            .border(if (selected) 3.dp else 1.5.dp, if (selected) BoardTheme.green else hexColor(accent).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .plainClickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Text(template?.icon ?: "⬜", fontSize = 30.sp, lineHeight = 36.sp)
            Text(template?.title ?: "Blank Grid", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BoardTheme.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(template?.summary ?: "Empty buttons to fill in yourself", fontSize = 11.sp, color = BoardTheme.slate, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            if (template != null) {
                Text("${template.buttonCount} buttons", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(hexColor(template.accent)).padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
        if (selected) {
            Icon(Icons.Default.CheckCircle, null, tint = BoardTheme.green, modifier = Modifier.align(Alignment.TopEnd).size(22.dp).background(Color.White, CircleShape))
        }
    }
}

// MARK: - Pages navigator

/** Pages list with its own Edit/Done control. */
@Composable
fun PagesListSheet(store: AACStore, onDismiss: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var refused by remember { mutableStateOf(false) }

    val navBottom = LocalNavBarBottom.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.activity.compose.BackHandler { onDismiss() }
        Column(Modifier.fillMaxSize().background(BoardTheme.background).padding(bottom = navBottom)) {
            Box(Modifier.fillMaxWidth().background(Color.White).height(56.dp).padding(horizontal = 12.dp)) {
                Text(if (editing) "Done" else "Edit", color = BoardTheme.blue, fontSize = 17.sp, modifier = Modifier.align(Alignment.CenterStart).plainClickable { editing = !editing }.padding(8.dp))
                Text("Pages in this Book", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BoardTheme.ink, modifier = Modifier.align(Alignment.Center))
                Text("Done", color = BoardTheme.green, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterEnd).plainClickable(onClick = onDismiss).padding(8.dp))
            }
            LazyColumn(Modifier.fillMaxSize().padding(16.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)) {
                items(store.pages.size, key = { store.pages[it].id }) { index ->
                    val page = store.pages[index]
                    FormRow(onClick = { store.currentPageIndex = index; onDismiss() }) {
                        if (editing) {
                            Icon(Icons.Default.Delete, "Delete", tint = BoardTheme.danger, modifier = Modifier.plainClickable {
                                if (store.pages.size <= 1) refused = true else store.removePage(index)
                            }.padding(end = 12.dp))
                        }
                        Text("${index + 1}.", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = BoardTheme.green, modifier = Modifier.width(32.dp))
                        Column(Modifier.weight(1f)) {
                            Text(page.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = BoardTheme.ink)
                            Text("${page.type.displayName} • ${page.gridSize} buttons", fontSize = 12.sp, color = BoardTheme.slate)
                        }
                        if (index == store.currentPageIndex) Icon(Icons.Default.CheckCircle, null, tint = BoardTheme.green)
                    }
                }
            }
        }
    }
    if (refused) {
        AlertDialog(onDismissRequest = { refused = false }, confirmButton = { TextButton({ refused = false }) { Text("OK") } },
            title = { Text("Keep at least one page") },
            text = { Text("A communication book needs somewhere to put buttons. Add another page before removing this one.") })
    }
}

// MARK: - Template gallery ("Downloads")

@Composable
fun GallerySheet(store: AACStore, onDismiss: () -> Unit) {
    var search by remember { mutableStateOf("") }
    var installed by remember { mutableStateOf<String?>(null) }
    val q = search.trim().lowercase()
    val matches = if (q.isEmpty()) PageTemplateCatalog.all else PageTemplateCatalog.all.filter { t ->
        t.title.lowercase().contains(q) || t.summary.lowercase().contains(q) || t.tiles.any { it.label.lowercase().contains(q) }
    }

    ModalSheet(title = "Template Gallery", onDismiss = onDismiss, leading = null, trailing = "Done", onTrailing = onDismiss, scroll = false) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = BoardTheme.slate)
            PlainTextField(search, { search = it }, "Search templates", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            for (category in PageTemplateCatalog.categories) {
                val inCat = matches.filter { it.category == category }
                if (inCat.isEmpty()) continue
                item { Text(category.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BoardTheme.slate, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 6.dp)) }
                items(inCat, key = { it.id }) { t ->
                    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(16.dp)) {
                        Text(t.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = BoardTheme.ink)
                        Text(t.summary, fontSize = 15.sp, color = BoardTheme.slate)
                        Text(t.tiles.take(6).joinToString(" · ") { it.label } + if (t.buttonCount > 6) " …" else "", fontSize = 12.sp, color = hexColor("#94A3B8"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill("${t.buttonCount} buttons"); Spacer(Modifier.width(6.dp)); Pill("${t.gridSize}-grid")
                            Spacer(Modifier.weight(1f))
                            val done = installed == t.id
                            Text(if (done) "✓ Added" else "Add to Book", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(hexColor(if (done) "#94A3B8" else "#008369"))
                                    .plainClickable { store.addPage(t.makePage()); installed = t.id }
                                    .padding(horizontal = 14.dp, vertical = 6.dp))
                        }
                    }
                }
            }
            if (matches.isEmpty()) item { Text("No templates match \"$search\"", color = BoardTheme.slate, modifier = Modifier.padding(16.dp)) }
        }
    }
}

@Composable
private fun Pill(text: String) {
    Text(text, fontSize = 12.sp, color = BoardTheme.ink, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(hexColor("#E2E8F0")).padding(horizontal = 8.dp, vertical = 3.dp))
}

// MARK: - Help

private data class HelpTopic(val icon: ImageVector, val color: String, val title: String, val body: String)

private val helpTopics = listOf(
    HelpTopic(Icons.Default.PlayArrow, "#F5893B", "Player",
        "Player is the screen a child uses. Tap a tile and it speaks. On the top bar, the round arrow steps back one page, the orange house returns to this menu, and tapping the page name opens the list of every page in the book so you can jump straight to one. Nothing on the Player screen can change a page by accident."),
    HelpTopic(Icons.Default.ChatBubble, "#4DB2FF", "The sentence bar",
        "Pages with the sentence bar switched on collect words as tiles are tapped - \"I want + Eat + Pizza\". Press the round PLAY button to hear the whole sentence, or tap the words themselves. The red X clears it. Turn the bar on or off for any page in Page Options, under Behavior."),
    HelpTopic(Icons.Default.Edit, "#4DB2FF", "Page Editor",
        "Page Editor is the same board with editing switched on. Tap any tile - filled or empty - to open the button editor. On the top bar: the arrow steps back a page, the house goes home, the sliders open Page Options, and the orange + starts a new page. Tap the page name to rename it right there; the small arrow next to it opens the page list."),
    HelpTopic(Icons.Default.GridView, "#2EC98A", "Editing a button",
        "Two boxes at the top: \"On the button\" is the word shown on the tile, \"Voice says\" is what is spoken - leave it empty and the voice reads the button's word. Below that: Play Preview, record your own voice, a photo from the camera or library, thousands of picture symbols to search, button and text colours, and word size. Saved Buttons keeps a finished button so you can drop it onto any page later."),
    HelpTopic(Icons.Default.Add, "#F5893B", "New pages and page kinds",
        "The + button opens the New Page Wizard. Give the page a name and pick a kind. A Standard Grid is a board of buttons - choose how many (1 to 48) and start blank or from a ready-made board. A Visual Scene Display is a photo with talking spots on it. A Symbol Keyboard is a keyboard made of pictures instead of letters, grouped as People, Actions, Describing, Things and so on - press pictures to build a sentence."),
    HelpTopic(Icons.Default.Photo, "#9B7BFF", "Visual scenes",
        "A scene page shows a photo - the living room, the playground - with invisible talking spots over things in it. In the editor, Add Hotspot puts a new spot on the picture; drag it into place and drag its corner handle to size it. Tap a spot to name it and choose what it says. The picture itself is changed in Page Options."),
    HelpTopic(Icons.Default.Tune, "#4DB2FF", "Page Options",
        "The sliders button in the editor. Rename the page, pick its background colour, change how many buttons a grid has, switch the sentence bar on, and Share This Page as a file another Talk Tiles device can add to its book."),
    HelpTopic(Icons.Default.Download, "#2EC98A", "Downloads",
        "Ready-made boards - food, feelings, school, bedtime and more - each with pictures already on every button. Tap Add to Book and the board appears as a new page you can then change however you like."),
    HelpTopic(Icons.Default.RecordVoiceOver, "#9B7BFF", "Voice and touch",
        "In Settings you can choose the voice and how fast it speaks, and test it. Bella is the app's own recorded voice. Under Touch: Hold to speak makes a tile wait until the finger has rested on it, so a brushing hand does not talk; Pause before repeat stops one press landing five times; Speak when the finger lifts waits for the release."),
    HelpTopic(Icons.Default.Lock, "#FF6B6B", "Child Lock",
        "Lock editing in Settings hides every way into the Page Editor behind a PIN, so the board cannot be changed by the child using it. To stop the child leaving Talk Tiles altogether, use Android's own screen pinning: Settings > Security > App pinning, then pin Talk Tiles from the recent apps view."),
    HelpTopic(Icons.Default.Backup, "#FFC93C", "Backing up",
        "Settings > Backup writes the whole book - every page, button, photo and recording - to one file you can email to yourself or keep in your files. Restore reads it back onto any tablet or iPad running Talk Tiles. Do this whenever you have spent real time building, and always before changing devices. Reset to the starter book, at the bottom of Settings, erases everything and cannot be undone.")
)

@Composable
fun HelpSheet(onDismiss: () -> Unit) {
    ModalSheet(title = "Help", onDismiss = onDismiss, leading = null, trailing = "Done", onTrailing = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(BoardTheme.sentence).padding(18.dp)) {
            Text("TALK TILES GUIDE", fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp, color = BoardTheme.ink)
            Text("How each part of the app works, for parents, teachers and therapists.", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = BoardTheme.inkSoft)
        }
        Spacer(Modifier.height(14.dp))
        for (t in helpTopics) {
            Row(Modifier.fillMaxWidth().padding(bottom = 14.dp).shadow(3.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(Color.White).padding(16.dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(hexColor(t.color)), contentAlignment = Alignment.Center) {
                    Icon(t.icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(t.title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = BoardTheme.ink)
                    Spacer(Modifier.height(6.dp))
                    Text(t.body, fontSize = 16.sp, color = hexColor("#334155"), lineHeight = 22.sp)
                }
            }
        }
    }
}

// MARK: - PIN prompt

@Composable
fun PinPromptSheet(expected: String, onDismiss: () -> Unit, onUnlocked: () -> Unit) {
    var entered by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    fun press(key: String) {
        wrong = false
        if (key == "⌫") { if (entered.isNotEmpty()) entered = entered.dropLast(1); return }
        if (entered.length >= 4) return
        entered += key
        if (entered.length < 4) return
        if (entered == expected) { onUnlocked(); onDismiss() } else { wrong = true; entered = "" }
    }

    ModalSheet(title = "Locked", onDismiss = onDismiss, leading = null, trailing = "Cancel", onTrailing = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Icon(Icons.Default.Lock, null, tint = BoardTheme.green, modifier = Modifier.size(42.dp))
            Text("Enter the PIN to edit", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BoardTheme.ink)
            Text("This keeps the board safe from accidental changes.", fontSize = 15.sp, color = BoardTheme.slate)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                for (i in 0 until 4) Box(Modifier.size(18.dp).clip(CircleShape).background(if (i < entered.length) BoardTheme.green else hexColor("#E2E8F0")))
            }
            if (wrong) Text("That PIN is not right. Try again.", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BoardTheme.danger)
            Column(Modifier.widthIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (row in listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫"))) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (key in row) {
                            if (key.isEmpty()) Spacer(Modifier.size(74.dp, 58.dp))
                            else Box(Modifier.size(74.dp, 58.dp).clip(RoundedCornerShape(10.dp)).background(hexColor("#F1F5F9")).plainClickable { press(key) }, contentAlignment = Alignment.Center) {
                                Text(key, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = hexColor("#1E293B"))
                            }
                        }
                    }
                }
            }
        }
    }
}
