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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
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
    var enabled by remember { mutableStateOf(page.enabled) }
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
            var next = p.copy(title = title, gridSize = gridSize, bgHex = bgHex, express = express, enabled = enabled)
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
        FormSection("Behavior", "A page that is switched off stays in the book and in the editor, but the arrows skip it when talking - useful while a page is half built.") {
            ToggleRow("Sentence bar on this page", express) { express = it }
            ToggleRow("Show this page when talking", enabled) { enabled = it }
        }
        FormSection(footer = "Sends this page as a file you can message, email or save - another Talk Tiles device can add it to their book.") {
            FormButton("Share this page", tint = BoardTheme.ink, icon = Icons.Default.Share) {
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
            .accessibleClickable(label = template?.title ?: "Blank grid", role = androidx.compose.ui.semantics.Role.RadioButton, shape = RoundedCornerShape(14.dp), onClick = onClick)
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

// MARK: - Find (pages + words)

/**
 * One place to go anywhere in the book: type a page name or a word. A word
 * result opens the page it lives on - it is never spoken from here. In the
 * editor the same sheet manages the pages: switch on/off, move, delete.
 */
@Composable
fun FindSheet(store: AACStore, onDismiss: () -> Unit) {
    val c = TT.colors
    var query by remember { mutableStateOf("") }
    var refused by remember { mutableStateOf(false) }
    val editing = store.isEditMode
    val pageHits = remember(query, store.pages, editing) { VocabularySearch.pages(store.pages, query, editing) }
    val wordHits = remember(query, store.pages, editing) { VocabularySearch.search(store.pages, query, editing).take(60) }

    ModalSheet(title = if (editing) "Pages in this book" else "Find a page or word", onDismiss = onDismiss, leading = null, trailing = "Done", onTrailing = onDismiss, scroll = false) {
        Row(Modifier.fillMaxWidth().clip(TTShape.medium).background(c.surface).border(1.dp, c.line, TTShape.medium).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = c.inkSoft)
            PlainTextField(query, { query = it }, "Page name or word", Modifier.weight(1f))
        }
        Spacer(Modifier.height(TTSpace.m))
        LazyColumn(Modifier.fillMaxSize()) {
            if (query.isBlank() || pageHits.isNotEmpty()) {
                item { Text("PAGES", style = TTType.overline, color = c.inkSoft, modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)) }
            }
            items(pageHits, key = { it.id }) { page ->
                val index = store.pages.indexOf(page)
                val current = page.id == store.currentPage.id
                Column(Modifier.fillMaxWidth().padding(bottom = TTSpace.s).clip(TTShape.medium).background(c.surface).border(1.dp, c.line, TTShape.medium)) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = TTSpace.chrome)
                            .accessibleClickable(label = "Open ${page.title}", ripple = true) { if (store.goToPage(page.id)) onDismiss() }
                            .padding(horizontal = TTSpace.l, vertical = TTSpace.m),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${index + 1}", style = TTType.bodyStrong, color = c.primary, modifier = Modifier.width(32.dp))
                        Column(Modifier.weight(1f)) {
                            Text(page.title, style = TTType.bodyStrong, color = if (page.enabled) c.ink else c.inkFaint)
                            Text("${page.type.displayName} · ${if (page.type == PageType.KEYBOARD) "${page.keyboardKeys ?: SymbolWordBank.defaultKeyCount} keys" else "${page.gridSize} buttons"}" +
                                if (!page.enabled) " · off when talking" else "", style = TTType.caption, color = c.inkSoft)
                        }
                        if (current) Icon(Icons.Default.CheckCircle, "Current page", tint = c.success)
                    }
                    if (editing) {
                        Divider()
                        Row(Modifier.fillMaxWidth().padding(horizontal = TTSpace.s, vertical = TTSpace.xs), verticalAlignment = Alignment.CenterVertically) {
                            Text("Show when talking", style = TTType.label, color = c.inkSoft, modifier = Modifier.padding(start = TTSpace.s).weight(1f))
                            androidx.compose.material3.Switch(checked = page.enabled, onCheckedChange = { on -> store.updatePage(page.id) { it.copy(enabled = on) } },
                                modifier = Modifier.semantics { contentDescription = "Show ${page.title} when talking" })
                            IconAction(Icons.Default.KeyboardArrowUp, "Move ${page.title} up", enabled = index > 0) { store.movePage(index, index - 1) }
                            IconAction(Icons.Default.KeyboardArrowDown, "Move ${page.title} down", enabled = index < store.pages.size - 1) { store.movePage(index, index + 1) }
                            IconAction(Icons.Default.Delete, "Delete ${page.title}", tint = c.danger) {
                                if (store.pages.size <= 1) refused = true else store.removePage(index)
                            }
                        }
                    }
                }
            }
            if (query.isNotBlank()) {
                item { Text("WORDS", style = TTType.overline, color = c.inkSoft, modifier = Modifier.padding(start = 16.dp, top = TTSpace.s, bottom = 6.dp)) }
                if (wordHits.isEmpty()) item { Text("No button says \"$query\".", style = TTType.body, color = c.inkSoft, modifier = Modifier.padding(16.dp)) }
                items(wordHits.size, key = { "w-${wordHits[it].pageId}-${wordHits[it].kind}-${wordHits[it].label}-$it" }) { i ->
                    val hit = wordHits[i]
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = TTSpace.s).clip(TTShape.medium).background(c.surface).border(1.dp, c.line, TTShape.medium)
                            .heightIn(min = TTSpace.chrome)
                            .accessibleClickable(label = "Go to ${hit.label} on ${hit.pageTitle}") {
                                if (store.goToPage(hit.pageId)) { if (hit.kind == VocabularySearch.Kind.KEY) store.requestedKeyGroup = hit.keyGroupId; onDismiss() }
                            }
                            .padding(horizontal = TTSpace.l, vertical = TTSpace.m),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TTSpace.m)
                    ) {
                        TileThumbnail(hit.photoData, hit.symbolName, hit.label, "#FFFFFF", "#D6DDE4", "#16202B", size = 44, key = "find-$i")
                        Column(Modifier.weight(1f)) {
                            Text(hit.label, style = TTType.bodyStrong, color = c.ink)
                            val what = when (hit.kind) { VocabularySearch.Kind.TILE -> "Button"; VocabularySearch.Kind.HOTSPOT -> "Talking spot"; VocabularySearch.Kind.KEY -> "Keyboard word" }
                            Text("$what on ${hit.pageTitle}" + if (hit.spoken != hit.label) " · says \"${hit.spoken}\"" else "", style = TTType.caption, color = c.inkSoft, maxLines = 2)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(TTSpace.xxl)) }
        }
    }
    if (refused) {
        AlertDialog(onDismissRequest = { refused = false }, confirmButton = { TextButton({ refused = false }) { Text("OK") } },
            title = { Text("Keep at least one page") },
            text = { Text("A communication book needs somewhere to put buttons. Add another page before removing this one.") })
    }
}

/** Kept for older call sites. */
@Composable
fun PagesListSheet(store: AACStore, onDismiss: () -> Unit) = FindSheet(store, onDismiss)

// MARK: - Saved phrases

/**
 * Sentences kept for later. Tapping one puts it in the bar and speaks it -
 * exactly as it was saved. Saving needs no PIN: it is the person's own
 * speech. Deleting is an editor job.
 */
@Composable
fun PhrasesSheet(store: AACStore, onDismiss: () -> Unit) {
    val c = TT.colors
    val library = PhraseLibrary.shared
    var name by remember { mutableStateOf("") }
    val current = store.sentence.items

    ModalSheet(title = "Saved phrases", onDismiss = onDismiss, leading = null, trailing = "Done", onTrailing = onDismiss, scroll = false) {
        if (current.isNotEmpty()) {
            FormSection("Save what is in the bar", "The phrase is kept exactly as it was built, recordings included, and works without any connection.") {
                FormRow { PlainTextField(name, { name = it }, current.joinToString(" ") { it.label }, Modifier.weight(1f)) }
                FormButton("Save this sentence", tint = c.primary, icon = Icons.Default.Bookmark) {
                    library.add(current, name); name = ""
                }
            }
        }
        if (library.items.isEmpty()) {
            FormSection {
                Column(Modifier.padding(16.dp)) {
                    Text("No saved phrases yet", style = TTType.heading, color = c.ink)
                    Spacer(Modifier.height(8.dp))
                    Text("Build a sentence in the bar, open this sheet and save it. It will be one tap from then on.", style = TTType.body, color = c.inkSoft)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(library.items, key = { it.id }) { phrase ->
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = TTSpace.s).clip(TTShape.medium).background(c.surface).border(1.dp, c.line, TTShape.medium)
                            .heightIn(min = TTSpace.chrome)
                            .accessibleClickable(label = "Say ${phrase.name}") {
                                store.sentence.replaceWith(phrase.items); store.speakSentence(); onDismiss()
                            }
                            .padding(start = TTSpace.l, end = TTSpace.s, top = TTSpace.m, bottom = TTSpace.m),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TTSpace.m)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = c.primary)
                        Column(Modifier.weight(1f)) {
                            Text(phrase.name, style = TTType.bodyStrong, color = c.ink)
                            if (phrase.spokenText != phrase.name) Text(phrase.spokenText, style = TTType.caption, color = c.inkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        if (store.isEditMode) IconAction(Icons.Default.Delete, "Delete ${phrase.name}", tint = c.danger) { library.remove(phrase.id) }
                    }
                }
                item { Spacer(Modifier.height(TTSpace.xxl)) }
            }
        }
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

    ModalSheet(title = "Page library", onDismiss = onDismiss, leading = null, trailing = "Done", onTrailing = onDismiss, scroll = false) {
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
                            if (done) Badge("Added", color = TT.colors.success, onColor = Color.White)
                            else PrimaryButton("Add to book", minHeight = TTSpace.touch) { store.addPage(t.makePage()); installed = t.id }
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
    HelpTopic(Icons.Default.PlayArrow, "#0F6E8C", "Talking",
        "Start talking opens the book on the page it was last on. Tap a button and it speaks. The top bar has Home, Previous page and Next page, the page name with its place in the book, and Find. Nothing on the talking screen can change a page by accident."),
    HelpTopic(Icons.Default.Search, "#0F6E8C", "Find a page or word",
        "Tap Find (or the page name) and type. Pages match by name; buttons, talking spots and keyboard words match by what is on them or what they say. Choosing a result opens that page - it does not speak the word."),
    HelpTopic(Icons.Default.ChatBubble, "#0F6E8C", "The sentence bar",
        "Pages with the sentence bar on collect words as buttons are tapped. Speak says the whole sentence in order - a button that has your own recording plays that recording. Stop halts it. Remove last word takes off the last one; Clear empties the bar and Undo brings it back. The bar keeps the sentence when you turn the page. Saved phrases keeps a sentence for one-tap use later."),
    HelpTopic(Icons.Default.Edit, "#C4731F", "Edit pages",
        "Edit pages is the same board with editing switched on. Tap any button, filled or empty, to open the button editor. Previous and Next step through every page, including ones switched off. Page options changes this page; New page adds one; the page name opens the page list, where pages can be switched on or off, moved and deleted."),
    HelpTopic(Icons.Default.GridView, "#C4731F", "Editing a button",
        "\"On the button\" is the word shown; \"Voice says\" is what is spoken - leave it empty and the voice reads the word. Below that: Play preview, record your own voice, a photo from the camera or library, thousands of pictures to search, button and text colours, word size. Saved buttons keeps a finished button so it can be put on any page later."),
    HelpTopic(Icons.Default.Add, "#C4731F", "New pages and page kinds",
        "New page asks for a name and a kind. A Standard Grid is a board of 1 to 48 buttons, blank or from a ready-made board. A Visual Scene is a photo with talking spots on it. A Symbol Keyboard is a keyboard made of pictures, grouped as People, Actions, Describing, Things and so on."),
    HelpTopic(Icons.Default.Photo, "#C4731F", "Visual scenes",
        "A scene page shows a photo with talking spots over things in it. In the editor, Add talking spot puts a new one on the picture; drag it into place and drag its corner handle to size it. Tap a spot to name it and choose what it does: speak, play a recording, or open another page. The picture itself is changed in Page options."),
    HelpTopic(Icons.Default.Tune, "#C4731F", "Page options",
        "Rename the page, pick its background, change how many buttons a grid has, switch the sentence bar on, switch the page off while it is half built, and Share this page as a file another Talk Tiles can add to its book."),
    HelpTopic(Icons.Default.Download, "#C4731F", "Page library",
        "Ready-made boards - food, feelings, school, bedtime and more - each with pictures on every button. Add to book puts the board in as a new page you can then change however you like."),
    HelpTopic(Icons.Default.RecordVoiceOver, "#1E7B4E", "Voice and touch",
        "Settings › Voice chooses the voice and how fast it speaks. Bella is the app's own recorded voice; device voices read anything typed. Under Touch: Hold to speak makes a button wait until the finger has rested on it, with a ring that fills as it waits; Pause before repeat stops one press landing five times; Speak when the finger lifts waits for the release, so a press that slides off does not speak."),
    HelpTopic(Icons.Default.Lock, "#1E7B4E", "Protect editing",
        "Settings › Protect editing puts a four-digit PIN on Edit pages, Page library and Settings, so the book cannot be changed by the person using it. Talking, Find and the sentence bar are never behind the PIN. To keep Talk Tiles on the screen, Settings › Keep on screen uses Android's screen pinning; to leave, hold Back and Overview together."),
    HelpTopic(Icons.Default.Backup, "#1E7B4E", "Backing up",
        "Settings › Backup writes the whole book - every page, button, photo, recording, saved button and saved phrase - to one file. Save it to Files or share it. Restore reads it back onto any tablet or iPad running Talk Tiles; the book being replaced is kept in a snapshot first. Reset to the starter book erases everything, after a snapshot.")
)

@Composable
fun HelpSheet(onDismiss: () -> Unit) {
    ModalSheet(title = "Help", onDismiss = onDismiss, leading = null, trailing = "Done", onTrailing = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(TTShape.large).background(TT.colors.primarySoft).padding(18.dp)) {
            Text("Talk Tiles guide", style = TTType.title, color = TT.colors.ink)
            Text("How each part of the app works, for the person talking and for the people who help.", style = TTType.body, color = TT.colors.inkSoft)
        }
        Spacer(Modifier.height(14.dp))
        for (t in helpTopics) {
            Row(Modifier.fillMaxWidth().padding(bottom = 14.dp).clip(TTShape.medium).background(TT.colors.surface).border(1.dp, TT.colors.line, TTShape.medium).padding(16.dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(hexColor(t.color)), contentAlignment = Alignment.Center) {
                    Icon(t.icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(t.title, style = TTType.heading, color = TT.colors.ink)
                    Spacer(Modifier.height(6.dp))
                    Text(t.body, style = TTType.body, color = TT.colors.inkSoft)
                }
            }
        }
    }
}

// MARK: - PIN prompt

@Composable
fun PinPromptSheet(expected: String, onDismiss: () -> Unit, onUnlocked: () -> Unit) {
    val c = TT.colors
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

    ModalSheet(title = "Protect editing", onDismiss = onDismiss, leading = "Cancel") {
        Column(Modifier.fillMaxWidth().padding(top = TTSpace.xl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TTSpace.l)) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(c.primarySoft), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lock, null, tint = c.primaryDeep, modifier = Modifier.size(32.dp))
            }
            Text("Enter the editing PIN", style = TTType.title, color = c.ink)
            Text("Editing is protected so the book cannot change by accident. Talking never needs the PIN.", style = TTType.body, color = c.inkSoft, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 360.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.semantics { contentDescription = "${entered.length} of 4 digits entered" }) {
                for (i in 0 until 4) Box(Modifier.size(18.dp).clip(CircleShape).background(if (i < entered.length) c.primary else c.line))
            }
            if (wrong) Text("That PIN is not right. Try again.", style = TTType.bodyStrong, color = c.danger)
            Column(Modifier.widthIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(TTSpace.m)) {
                for (row in listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫"))) {
                    Row(horizontalArrangement = Arrangement.spacedBy(TTSpace.m)) {
                        for (key in row) {
                            if (key.isEmpty()) Spacer(Modifier.size(84.dp, 64.dp))
                            else Box(
                                Modifier.size(84.dp, 64.dp).clip(TTShape.small).background(c.surface).border(1.dp, c.line, TTShape.small)
                                    .semantics { contentDescription = if (key == "⌫") "Delete digit" else "Key $key" }
                                    .accessibleClickable { press(key) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(key, style = TTType.title, color = c.ink)
                            }
                        }
                    }
                }
            }
        }
    }
}
