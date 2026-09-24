package com.talktiles.tablet

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// MARK: - Quick Edit (one button)

@Composable
fun QuickEditSheet(store: AACStore, slot: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val existing = remember(slot) { store.currentPage.tiles[slot] }
    val favorites = TileFavorites.shared

    var label by remember { mutableStateOf(existing?.label ?: "") }
    var tts by remember { mutableStateOf(existing?.tts ?: "") }
    var symbol by remember { mutableStateOf(existing?.symbolName) }
    var bgHex by remember { mutableStateOf(existing?.bgHex ?: "#FFFFFF") }
    var borderHex by remember { mutableStateOf(existing?.borderHex ?: "#CBD5E1") }
    var labelHex by remember { mutableStateOf(existing?.labelHex ?: "#1E293B") }
    var labelSize by remember { mutableStateOf(existing?.labelSize ?: 1.0) }
    var labelTop by remember { mutableStateOf(existing?.labelPositionTop ?: false) }
    var photoData by remember { mutableStateOf(existing?.photoData) }
    var audioData by remember { mutableStateOf(existing?.audioData) }
    var favoriteName by remember { mutableStateOf("") }
    var savedNote by remember { mutableStateOf<String?>(null) }
    var showSymbols by remember { mutableStateOf(false) }
    var showSaved by remember { mutableStateOf(false) }
    var needPro by remember { mutableStateOf<ProBlock?>(null) }
    val recorder = remember { AudioRecorder(context) }
    LaunchedEffect(recorder.recordedData) { recorder.recordedData?.let { audioData = it } }
    val startRecording = rememberRecordPermission { recorder.start() }

    val spoken = if (tts.isEmpty()) label else tts
    val favoriteNameToUse = favoriteName.trim().ifEmpty { label.trim() }
    val canSaveFavorite = favoriteNameToUse.isNotEmpty() && (photoData != null || audioData != null || symbol != null || label.isNotEmpty())

    fun save() {
        val updated = TileModel(
            id = slot, label = label, tts = tts.ifEmpty { label }, symbolName = symbol, photoData = photoData,
            bgHex = bgHex, borderHex = borderHex, labelHex = labelHex, labelSize = labelSize,
            audioData = audioData, isSoundItOut = false, labelPositionTop = labelTop
        )
        store.updateCurrentPage { p -> p.copy(tiles = p.tiles + (slot to updated)) }
    }

    val set = store.settings.symbolSet
    val open = if (SymbolLibrary.has(set)) set else SymbolSet.values().firstOrNull { SymbolLibrary.has(it) } ?: set
    val symbolFooter = if (!SymbolLibrary.anyAvailable) "No symbol library is bundled with this build." else {
        var t = "${SymbolLibrary.count(open)} ${open.title} are built in"
        if (open == SymbolSet.TALK_TILES) t += ", each with Bella's voice"
        t + ". Change the set under Settings › Pictures. The emoji below cover core words - yes, no, please, stop."
    }

    ModalSheet(title = label.trim().ifEmpty { "Quick Edit" }, onDismiss = onDismiss, trailing = "Save", onTrailing = { save(); onDismiss() }) {
        FormSection("Words", "Leave the second one empty to have the voice say exactly what is written on the button.") {
            LabeledField("On the button", "e.g. Apple, Help, Water", label) { label = it }
            LabeledField("Voice says", "Same as the button", tts) { tts = it }
        }

        FormSection("Voice") {
            FormButton("Play Preview", icon = Icons.Default.PlayCircle, enabled = spoken.isNotEmpty()) { SpeechManager.shared.speak(spoken) }
            FormButton(
                if (recorder.isRecording) "Stop Recording" else if (audioData == null) "Record Own Voice" else "Re-record Own Voice",
                tint = if (recorder.isRecording) Color.Red else BoardTheme.green,
                icon = if (recorder.isRecording) Icons.Default.Stop else Icons.Default.Mic
            ) { if (recorder.isRecording) recorder.stop() else startRecording() }
            FormButton("Play Recording", icon = Icons.Default.PlayCircle, enabled = audioData != null) { audioData?.let { SpeechManager.shared.playAudioData(it) } }
            if (audioData != null) FormButton("Remove Recording", tint = Color.Red) { audioData = null }
        }

        FormSection("Button Picture") {
            FormRow {
                Box(Modifier.size(74.dp).clip(RoundedCornerShape(10.dp)).background(hexColor("#F8FAFC")).border(1.dp, BoardTheme.line, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    val photo = PhotoCache.bitmap(photoData, "edit-$slot")
                    if (photo != null) Image(photo.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Default.Photo, null, tint = hexColor("#94A3B8"), modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(if (photoData != null) "Picture set" else "No picture", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BoardTheme.ink)
                    Text(if (photoData != null) "The picture replaces the symbol on this button." else "Use a photo of the real object or person.", fontSize = 12.sp, color = BoardTheme.slate)
                }
            }
            PictureSourceRows(hasPicture = photoData != null, maxDimension = 1024, onPicked = { photoData = it; symbol = null }, onRemove = { photoData = null })
        }

        FormSection("Symbol & Icon", symbolFooter) {
            val chosen = symbol
            if (chosen != null && !SymbolLibrary.isEmoji(chosen) && SymbolLibrary.image(chosen) != null) {
                FormRow {
                    SymbolPicture(chosen, 38.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(SymbolLibrary.readable(chosen), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BoardTheme.ink, modifier = Modifier.weight(1f))
                    TextAction("Remove", tint = BoardTheme.danger) { symbol = null }
                }
            }
            FormButton(if (symbol == null) "Choose a Symbol" else "Choose a Different Symbol", icon = Icons.Default.Search) { showSymbols = true }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for ((name, emoji) in listOf("eat" to "🍎", "water" to "💧", "yes" to "✅", "no" to "❌", "help" to "🙋", "happy" to "😊")) {
                    val on = symbol == name
                    Text(emoji, fontSize = 32.sp, modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) TT.colors.primarySoft else TT.colors.surfaceSunken)
                        .border(2.dp, if (on) BoardTheme.green else Color.Transparent, RoundedCornerShape(10.dp))
                        .accessibleClickable(label = name, ripple = false) {
                            symbol = name; photoData = null
                            if (label.isEmpty()) label = name.replaceFirstChar { it.uppercase() }
                        }
                        .padding(8.dp))
                }
            }
        }

        FormSection("Button Colours", "The starter boards colour buttons by kind - yellow for people, green for actions, orange for things. Matching a new button to the ones beside it keeps that meaning intact.") {
            ColorRow("Button Colour", bgHex) { bgHex = it }
            ColorRow("Border Colour", borderHex) { borderHex = it }
            ColorRow("Text Colour", labelHex) { labelHex = it }
        }

        FormSection("Word Size (${String.format("%.1fx", labelSize)})") {
            SliderRow("Size", String.format("%.1fx", labelSize), labelSize.toFloat(), 0.8f..2.0f, 11) { labelSize = (Math.round(it * 10) / 10.0) }
            ToggleRow("Word above the picture", labelTop) { labelTop = it }
        }

        FormSection("Saved Buttons", savedNote ?: "Saving keeps the picture, the recording, the words and the colours together, so this button can be put on any page without building it again.") {
            FormRow(onClick = { showSaved = true }) {
                Icon(Icons.Default.Star, null, tint = hexColor("#F59E0B"), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Use a Saved Button", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = BoardTheme.ink, modifier = Modifier.weight(1f))
                Text("${favorites.items.size}", fontSize = 14.sp, color = BoardTheme.slate)
                Icon(Icons.Default.ChevronRight, null, tint = BoardTheme.line)
            }
            FormRow {
                Icon(Icons.Default.StarBorder, null, tint = BoardTheme.green, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                PlainTextField(favoriteName, { favoriteName = it }, label.ifEmpty { "Name to save it under" }, Modifier.weight(1f))
            }
            FormButton(
                if (favorites.contains(favoriteNameToUse)) "Update Saved Button" else "Save This Button",
                tint = BoardTheme.green, icon = Icons.Default.StarBorder, enabled = canSaveFavorite
            ) {
                store.pro.blockSavingButton()?.let { needPro = it; return@FormButton }
                val saved = favorites.add(SavedTile(
                    name = favoriteNameToUse, label = label, tts = tts.ifEmpty { label }, symbolName = symbol,
                    photoData = photoData, audioData = audioData, bgHex = bgHex, borderHex = borderHex,
                    labelHex = labelHex, labelSize = labelSize, isSoundItOut = false, labelPositionTop = labelTop
                ))
                favoriteName = ""
                savedNote = "Saved as \"${saved.name}\". It is in Saved Buttons on every page now."
            }
        }
    }

    if (showSymbols) {
        SymbolPickerSheet(set = store.settings.symbolSet, onDismiss = { showSymbols = false }) { name ->
            symbol = name; photoData = null
            SymbolLibrary.talkTilesSymbol(name)?.let { sym ->
                if (label.isEmpty()) label = sym.label
                if (tts.isEmpty()) tts = sym.tts
            }
        }
    }
    if (needPro != null) UpgradeSheet(store, needPro, onDismiss = { needPro = null })
    if (showSaved) {
        SavedButtonPickerSheet(onDismiss = { showSaved = false }) { saved ->
            label = saved.label; tts = saved.tts; symbol = saved.symbolName; photoData = saved.photoData
            audioData = saved.audioData; bgHex = saved.bgHex; borderHex = saved.borderHex; labelHex = saved.labelHex
            labelSize = saved.labelSize; labelTop = saved.labelPositionTop
            savedNote = "Loaded \"${saved.name}\". Press Save to put it on this button."
        }
    }
}

// MARK: - Saved buttons

@Composable
fun SavedButtonPickerSheet(onDismiss: () -> Unit, onPick: (SavedTile) -> Unit) {
    val favorites = TileFavorites.shared
    var query by remember { mutableStateOf("") }
    val results = favorites.search(query)

    ModalSheet(title = "Saved Buttons", onDismiss = onDismiss, leading = "Back", scroll = false) {
        if (favorites.items.isEmpty()) {
            FormSection {
                Column(Modifier.padding(16.dp)) {
                    Text("Nothing saved yet", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = BoardTheme.ink)
                    Spacer(Modifier.height(8.dp))
                    Text("Build a button the way you want it - its picture, its recording, its colours - then use Save This Button. It will be here on every page after that.", fontSize = 14.sp, color = BoardTheme.slate)
                }
            }
        } else {
            FormSection {
                FormRow {
                    Icon(Icons.Default.Search, null, tint = hexColor("#94A3B8"))
                    Spacer(Modifier.width(8.dp))
                    PlainTextField(query, { query = it }, "Search saved buttons", Modifier.weight(1f))
                }
            }
            Text("Tap the bin to delete a saved button. Deleting it here does not touch any page it is already on.",
                fontSize = 13.sp, color = BoardTheme.slate, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            LazyColumn(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Color.White)) {
                items(results, key = { it.id }) { saved ->
                    FormRow(onClick = { onPick(saved); onDismiss() }) {
                        TileThumbnail(saved.photoData, saved.symbolName, saved.label, saved.bgHex, saved.borderHex, saved.labelHex, key = "fav-${saved.id}")
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(saved.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = BoardTheme.ink)
                            Text(saved.tts.ifEmpty { saved.label }, fontSize = 13.sp, color = BoardTheme.slate, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (saved.hasPhoto) Tag("Photo", Icons.Default.Photo, BoardTheme.blue)
                                if (saved.hasRecording) Tag("Recording", Icons.Default.GraphicEq, hexColor("#7C3AED"))
                                if (saved.symbolName != null && !saved.hasPhoto) Tag("Symbol", Icons.Default.Star, BoardTheme.green)
                            }
                        }
                        IconAction(Icons.Default.Delete, "Delete ${saved.name}", tint = BoardTheme.danger) { favorites.remove(saved.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(11.dp))
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = tint)
    }
}

// MARK: - Symbol picker

/** Searchable grid of bundled symbols; opens on the set chosen in Settings. */
@Composable
fun SymbolPickerSheet(set: SymbolSet, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val setsInBuild = SymbolSet.values().filter { SymbolLibrary.has(it) }
    var activeSet by remember { mutableStateOf(if (SymbolLibrary.has(set)) set else setsInBuild.firstOrNull() ?: set) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    val results = remember(query, activeSet, category) {
        SymbolLibrary.search(query, activeSet, if (activeSet == SymbolSet.TALK_TILES) category else null)
    }

    ModalSheet(title = "Symbols", onDismiss = onDismiss, scroll = false) {
        if (setsInBuild.size > 1) {
            SegmentedPicker(setsInBuild, activeSet, { "${it.title} (${SymbolLibrary.count(it)})" }, { activeSet = it })
            Spacer(Modifier.height(10.dp))
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = BoardTheme.slate)
            PlainTextField(query, { query = it }, "Search symbols", Modifier.weight(1f))
        }
        if (activeSet == SymbolSet.TALK_TILES && TalkTilesCatalog.categories.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip("All", category == null) { category = null }
                for (c in TalkTilesCatalog.categories) FilterChip(TalkTilesCatalog.categoryTitle(c), category == c) { category = c }
            }
        } else {
            Spacer(Modifier.height(8.dp))
        }
        when {
            !SymbolLibrary.anyAvailable -> Text("No symbols are bundled with this build.", color = BoardTheme.slate, modifier = Modifier.padding(24.dp))
            results.isEmpty() -> Text("Nothing matches “$query”.", color = BoardTheme.slate, modifier = Modifier.padding(24.dp))
            else -> LazyVerticalGrid(columns = GridCells.Adaptive(92.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                items(results, key = { it }) { name ->
                    Column(
                        Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White).accessibleClickable(label = SymbolLibrary.readable(name), ripple = false) { onPick(name); onDismiss() }.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.height(62.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            val img = remember(name) { SymbolLibrary.image(name) }
                            if (img != null) Image(img.asImageBitmap(), SymbolLibrary.readable(name), Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        }
                        Text(SymbolLibrary.readable(name), fontSize = 11.sp, color = BoardTheme.slate, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                    }
                }
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Text(if (activeSet == SymbolSet.MULBERRY) SymbolLibrary.ATTRIBUTION else "Talk Tiles pictures are drawn in-house. Every one has Bella's voice behind it.",
                        fontSize = 11.sp, color = BoardTheme.slate, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp))
                }
            }
        }
    }
}

// MARK: - Hotspot editor

@Composable
fun HotspotEditorSheet(store: AACStore, hotspot: HotspotModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var label by remember { mutableStateOf(hotspot.label) }
    var tts by remember { mutableStateOf(hotspot.tts) }
    var style by remember { mutableStateOf(hotspot.style) }
    var action by remember { mutableStateOf(hotspot.action) }
    var width by remember { mutableStateOf(hotspot.w) }
    var height by remember { mutableStateOf(hotspot.h) }
    var audioData by remember { mutableStateOf(hotspot.audioData) }
    var jumpPageId by remember { mutableStateOf(hotspot.jumpPageId) }
    val recorder = remember { AudioRecorder(context) }
    LaunchedEffect(recorder.recordedData) { recorder.recordedData?.let { audioData = it } }
    val startRecording = rememberRecordPermission { recorder.start() }
    val spoken = if (tts.isEmpty()) label else tts

    fun save() {
        store.updateCurrentPage { p ->
            p.copy(hotspots = p.hotspots.map { h ->
                if (h.id != hotspot.id) h else h.copy(
                    label = label, tts = tts.ifEmpty { label }, style = style, action = action,
                    audioData = audioData, jumpPageId = jumpPageId,
                    w = minOf(width, 100.0 - h.x), h = minOf(height, 100.0 - h.y)
                )
            })
        }
    }

    ModalSheet(title = "Edit talking spot", onDismiss = onDismiss, trailing = "Save", onTrailing = { save(); onDismiss() }) {
        FormSection("Label") {
            LabeledField("On the photo", "e.g. Cat, Sofa, TV", label) { label = it }
        }
        FormSection("Size") {
            val upperW = maxOf(MIN_PCT + 1, 100.0 - hotspot.x).toFloat()
            val upperH = maxOf(MIN_PCT + 1, 100.0 - hotspot.y).toFloat()
            SliderRow("Width", "${width.toInt()}%", width.toFloat(), MIN_PCT.toFloat()..upperW, 0) { width = it.toDouble() }
            SliderRow("Height", "${height.toInt()}%", height.toFloat(), MIN_PCT.toFloat()..upperH, 0) { height = it.toDouble() }
            FormButton("Reset to Default Size") { width = 25.0; height = 25.0 }
        }
        FormSection("When Tapped") {
            Box(Modifier.padding(12.dp)) { SegmentedPicker(HotspotAction.values().toList(), action, { it.raw }, { action = it }) }
            when (action) {
                HotspotAction.TTS -> {
                    LabeledField("Voice says", "What to speak when tapped", tts) { tts = it }
                    FormButton("Play Preview", icon = Icons.Default.PlayCircle, enabled = spoken.isNotEmpty()) { SpeechManager.shared.speak(spoken) }
                }
                HotspotAction.RECORDED -> {
                    FormButton(
                        if (recorder.isRecording) "Stop Recording" else if (audioData == null) "Record Voice" else "Re-record Voice",
                        tint = if (recorder.isRecording) Color.Red else BoardTheme.green,
                        icon = if (recorder.isRecording) Icons.Default.Stop else Icons.Default.Mic
                    ) { if (recorder.isRecording) recorder.stop() else startRecording() }
                    FormButton("Play Recording", icon = Icons.Default.PlayCircle, enabled = audioData != null) { audioData?.let { SpeechManager.shared.playAudioData(it) } }
                    if (audioData != null) FormButton("Remove Recording", tint = Color.Red) { audioData = null }
                }
                HotspotAction.JUMP -> {
                    FormRow(onClick = { jumpPageId = null }) {
                        Text("None", modifier = Modifier.weight(1f), color = BoardTheme.ink)
                        if (jumpPageId == null) Icon(Icons.Default.Star, null, tint = BoardTheme.green)
                    }
                    for (p in store.pages) {
                        FormRow(onClick = { jumpPageId = p.id }) {
                            Text(p.title, modifier = Modifier.weight(1f), color = BoardTheme.ink)
                            if (jumpPageId == p.id) Icon(Icons.Default.Star, null, tint = BoardTheme.green)
                        }
                    }
                }
            }
        }
        FormSection("Appearance in Player Mode") {
            Box(Modifier.padding(12.dp)) { SegmentedPicker(HotspotStyle.values().toList(), style, { it.raw }, { style = it }) }
        }
        FormSection {
            FormButton("Delete talking spot", tint = BoardTheme.danger, icon = Icons.Default.Delete) {
                store.updateCurrentPage { p -> p.copy(hotspots = p.hotspots.filter { it.id != hotspot.id }) }
                onDismiss()
            }
        }
    }
}

private const val MIN_PCT = 6.0
