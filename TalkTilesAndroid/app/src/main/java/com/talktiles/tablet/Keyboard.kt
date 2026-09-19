package com.talktiles.tablet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A keyboard made of pictures instead of letters. The keys FILL the space
 * under the sentence bar; the rest of a group goes on numbered pages. The
 * sentence bar itself is the book's one bar, drawn by BoardView.
 */
@Composable
fun KeyboardPageView(store: AACStore, onSelectKey: (String) -> Unit) {
    val page = store.currentPage
    val c = TT.colors
    var groupId by remember { mutableStateOf("") }
    var justPressed by remember { mutableStateOf<String?>(null) }
    var wordPage by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    val visibleGroups = SymbolWordBank.groups(page.keyboardGroups)
    val currentWords: List<SymbolWord> = run {
        val base = visibleGroups.firstOrNull { it.id == groupId }?.words ?: visibleGroups.firstOrNull()?.words ?: emptyList()
        val edits = page.keyboardEdits
        base.mapNotNull { word ->
            val edit = edits?.get(word.id)
            if (edit?.hidden == true && !store.isEditMode) null else word.applying(edit)
        }
    }
    fun isHidden(word: SymbolWord) = page.keyboardEdits?.get(word.id)?.hidden == true
    val keysPerScreen = max(2, page.keyboardKeys ?: SymbolWordBank.defaultKeyCount)
    val pageCount = max(1, ceil(currentWords.size.toDouble() / keysPerScreen).toInt())
    val wordsPerPage = max(1, ceil(currentWords.size.toDouble() / pageCount).toInt())
    val wordsOnScreen: List<SymbolWord> = run {
        val p = wordPage.coerceIn(0, pageCount - 1)
        val start = p * wordsPerPage
        if (start >= currentWords.size) emptyList() else currentWords.subList(start, min(start + wordsPerPage, currentWords.size))
    }

    LaunchedEffect(store.currentPageIndex, visibleGroups.map { it.id }) {
        val ids = visibleGroups.map { it.id }
        if (groupId !in ids) { groupId = ids.firstOrNull() ?: ""; wordPage = 0 }
        if (wordPage >= pageCount) wordPage = 0
    }
    // A search result can ask for a group to be shown.
    LaunchedEffect(store.requestedKeyGroup) {
        val wanted = store.requestedKeyGroup ?: return@LaunchedEffect
        if (visibleGroups.any { it.id == wanted }) { groupId = wanted; wordPage = 0 }
        store.requestedKeyGroup = null
    }

    fun press(word: SymbolWord) {
        if (!TouchAccess.shouldFire("keyboard-${word.id}", store.settings.repeatLockout)) return
        store.sentence.add(SentenceItem.from(word))
        val audio = word.audioData
        if (audio != null) SpeechManager.shared.playAudioData(audio)
        else SpeechManager.shared.speak(word.tts, store.settings.speechRate.toFloat(), store.settings.voiceId)
        justPressed = word.id
        scope.launch { delay(220); if (justPressed == word.id) justPressed = null }
    }

    Column(Modifier.fillMaxSize().padding(vertical = TTSpace.s), verticalArrangement = Arrangement.spacedBy(TTSpace.s)) {
        // Group tabs and page arrows share one row.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(horizontal = TTSpace.m), horizontalArrangement = Arrangement.spacedBy(TTSpace.s)) {
                for (group in visibleGroups) {
                    val selected = group.id == groupId
                    Box(
                        Modifier
                            .heightIn(min = TTSpace.touch)
                            .clip(TTShape.pill)
                            .background(hexColor(group.color).copy(alpha = if (selected) 1f else 0.5f))
                            .border(2.dp, if (selected) c.ink.copy(alpha = 0.5f) else Color.Transparent, TTShape.pill)
                            .accessibleClickable(label = "${group.title} words", role = Role.Tab, shape = TTShape.pill) { groupId = group.id; wordPage = 0 }
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(group.title, style = TTType.label.copy(fontWeight = FontWeight.Bold), color = if (selected) c.ink else c.inkSoft)
                    }
                }
            }
            if (pageCount > 1) {
                Row(Modifier.padding(end = TTSpace.m), horizontalArrangement = Arrangement.spacedBy(TTSpace.xs), verticalAlignment = Alignment.CenterVertically) {
                    BarButton(Icons.Default.KeyboardArrowLeft, "Previous keys", size = TTSpace.touch, enabled = wordPage > 0) { wordPage-- }
                    Text("${min(wordPage, pageCount - 1) + 1}/$pageCount", style = TTType.label, color = c.inkSoft, modifier = Modifier.widthIn(min = 36.dp), textAlign = TextAlign.Center)
                    BarButton(Icons.Default.KeyboardArrowRight, "More keys", size = TTSpace.touch, enabled = wordPage < pageCount - 1) { wordPage++ }
                }
            }
        }

        // Symbol grid, sized from the words the pages actually carry.
        BoxWithConstraints(Modifier.fillMaxSize().weight(1f)) {
            val keys = wordsPerPage
            val ratio = maxWidth.value / max(maxHeight.value, 1f)
            val target = max(1.0, (ratio / 1.25).toDouble())
            var cols = sqrt(keys * target).roundToInt().coerceIn(1, keys)
            val rows = max(1, ceil(keys.toDouble() / cols).toInt())
            cols = max(1, ceil(keys.toDouble() / rows).toInt())
            val spacing = when { keys <= 6 -> 14.dp; keys <= 12 -> 12.dp; keys <= 20 -> 10.dp; else -> 8.dp }
            val pad = if (keys <= 12) 14.dp else 10.dp
            val cellW = ((maxWidth - pad * 2 - spacing * (cols - 1)) / cols).coerceAtLeast(48.dp)
            val cellH = ((maxHeight - pad * 2 - spacing * (rows - 1)) / rows).coerceAtLeast(48.dp)
            val words = wordsOnScreen

            Column(Modifier.fillMaxSize().padding(pad), verticalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterVertically)) {
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally)) {
                        for (col in 0 until cols) {
                            val i = r * cols + col
                            if (i < words.size) {
                                val word = words[i]
                                SymbolKey(word, cellW, cellH, editing = store.isEditMode, hidden = store.isEditMode && isHidden(word),
                                    pressed = justPressed == word.id) {
                                    if (store.isEditMode) onSelectKey(word.id) else press(word)
                                }
                            } else {
                                Spacer(Modifier.size(cellW, cellH))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SymbolKey(word: SymbolWord, width: Dp, height: Dp, editing: Boolean, hidden: Boolean, pressed: Boolean, onClick: () -> Unit) {
    val c = TT.colors
    val reduceMotion = TT.reduceMotion
    val labelSize = min(22f, max(13f, height.value * 0.15f))
    val iconSize = max(20f, height.value - labelSize - 18f).dp
    val says = if (word.tts.isNotEmpty() && !word.tts.equals(word.label, ignoreCase = true)) ". Says: ${word.tts}" else ""
    val label = if (editing) "Edit key ${word.label}" + (if (hidden) ", hidden on this page" else "") else word.label + says
    Box(
        Modifier
            .size(width, height)
            .scale(if (pressed && !reduceMotion) 0.96f else 1f)
            .alpha(if (hidden) 0.4f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .background(hexColor(word.color))
            .border(if (pressed) 4.dp else if (c.highContrast) 2.dp else 1.dp, if (pressed) c.pressed else if (c.highContrast) c.ink else c.line, RoundedCornerShape(12.dp))
            .semantics { contentDescription = label }
            .accessibleClickable(ripple = false, shape = RoundedCornerShape(12.dp), onClick = onClick)
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            KeyPicture(word, iconSize)
            Spacer(Modifier.height(4.dp))
            Text(word.label, fontSize = labelSize.sp, fontWeight = FontWeight.Bold, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (editing) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(5.dp).size(min(22f, max(13f, height.value * 0.16f)).dp + 6.dp)
                    .clip(CircleShape).background(if (hidden) c.inkFaint else c.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (hidden) Icons.Default.VisibilityOff else Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.fillMaxSize().padding(3.dp))
            }
        }
    }
}

/** A key shows a photo if the page put one there, then a bundled symbol, else the emoji. */
@Composable
fun KeyPicture(word: SymbolWord, size: Dp) {
    val photo = PhotoCache.bitmap(word.photoData, "key-${word.id}")
    if (photo != null) {
        Image(photo.asImageBitmap(), null, Modifier.size(size).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
        return
    }
    val img = if (SymbolLibrary.isEmoji(word.icon)) null else remember(word.icon) { SymbolLibrary.image(word.icon) }
    if (img != null) {
        Image(img.asImageBitmap(), null, Modifier.size(size), contentScale = ContentScale.Fit)
    } else {
        Text(word.icon, fontSize = (size.value * 0.8f).sp, maxLines = 1, lineHeight = (size.value * 0.95f).sp)
    }
}

/** The keyboard's setup, shared by the New Page Wizard and Page Options. */
@Composable
fun KeyboardSetupSections(keys: Int, onKeys: (Int) -> Unit, groupIds: List<String>, onGroups: (List<String>) -> Unit, stepNumber: Int? = null) {
    fun header(step: Int, text: String) = if (stepNumber == null) text else "${stepNumber + step - 1}. $text"

    LaunchedEffect(Unit) { onKeys(SymbolWordBank.nearestKeyCount(keys)) }

    FormSection(header(1, "Picture Grid Layout"),
        "The same sizes a Standard Grid page offers. The pictures fill the whole screen under the sentence bar, so fewer pictures means bigger ones. Words that do not fit go on a second page you reach with the arrows.") {
        Box(Modifier.padding(12.dp)) {
            SegmentedPicker(SymbolWordBank.keyCountOptions, keys, { "$it" }, onKeys)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(SymbolWordBank.keyCountLabel(keys), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BoardTheme.green, modifier = Modifier.weight(1f))
            Text("$keys pictures at a time", fontSize = 13.sp, color = BoardTheme.slate)
        }
    }

    FormSection(header(2, "Word Groups"),
        "The colours are the Fitzgerald key used across AAC systems - yellow people, green actions, orange things. Turn groups off to start smaller; you can turn them back on any time.") {
        for (group in SymbolWordBank.groups) {
            val on = group.id in groupIds
            ToggleRow(group.title, on, leading = {
                Box(Modifier.size(26.dp).clip(RoundedCornerShape(5.dp)).background(hexColor(group.color)).border(1.dp, BoardTheme.line, RoundedCornerShape(5.dp)))
                Spacer(Modifier.width(4.dp))
                Text("${group.words.size}", fontSize = 13.sp, color = BoardTheme.slate)
            }) { want ->
                if (want) { if (group.id !in groupIds) onGroups(groupIds + group.id) }
                else if (groupIds.size > 1) onGroups(groupIds.filter { it != group.id })
            }
        }
    }
}

/** Editing one key of one keyboard page: an override over a word the word bank owns. */
@Composable
fun KeyboardKeyEditorSheet(store: AACStore, wordId: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val original = remember(wordId) { SymbolWordBank.word(wordId) }
    val page = store.currentPage
    val applied = remember(wordId) { original?.applying(page.keyboardEdits?.get(wordId)) }

    var label by remember { mutableStateOf(applied?.label ?: "") }
    var tts by remember { mutableStateOf(applied?.tts ?: "") }
    var icon by remember { mutableStateOf(applied?.icon ?: "") }
    var colorHex by remember { mutableStateOf(applied?.color ?: "#FFFFFF") }
    var photoData by remember { mutableStateOf(applied?.photoData) }
    var audioData by remember { mutableStateOf(applied?.audioData) }
    var hidden by remember { mutableStateOf(page.keyboardEdits?.get(wordId)?.hidden ?: false) }
    var showSymbols by remember { mutableStateOf(false) }
    val recorder = remember { AudioRecorder(context) }
    LaunchedEffect(recorder.recordedData) { recorder.recordedData?.let { audioData = it } }
    val startRecording = rememberRecordPermission { recorder.start() }
    val spoken = if (tts.isEmpty()) label else tts

    fun save() {
        val base = original ?: return
        var edit = KeyboardKeyEdit(
            label = label.takeIf { it != base.label },
            tts = tts.takeIf { it != base.tts },
            icon = icon.takeIf { it != base.icon },
            colorHex = colorHex.takeIf { it != base.color },
            photoData = photoData,
            audioData = audioData,
            hidden = if (hidden) true else null
        )
        val edits = (page.keyboardEdits ?: emptyMap()).toMutableMap()
        if (edit.isEmpty) edits.remove(wordId) else edits[wordId] = edit
        store.updateCurrentPage { it.copy(keyboardEdits = edits.ifEmpty { null }) }
    }

    ModalSheet(title = label.ifEmpty { "Edit Key" }, onDismiss = onDismiss, trailing = "Save", onTrailing = { save(); onDismiss() }) {
        FormSection("Word & Speech Text", "Changing a word here changes it on this page only. The same word on another keyboard page is left alone.") {
            LabeledField("On the key", "Word shown on the key", label) { label = it }
            LabeledField("Voice says", "Same as the key", tts) { tts = it }
        }
        FormSection("Voice") {
            FormButton("Play Preview", icon = Icons.Default.PlayCircle, enabled = spoken.isNotEmpty()) {
                SpeechManager.shared.speak(spoken, store.settings.speechRate.toFloat(), store.settings.voiceId)
            }
            FormButton(
                if (recorder.isRecording) "Stop Recording" else if (audioData == null) "Record Own Voice" else "Re-record Own Voice",
                tint = if (recorder.isRecording) Color.Red else BoardTheme.green,
                icon = if (recorder.isRecording) Icons.Default.Stop else Icons.Default.Mic
            ) { if (recorder.isRecording) recorder.stop() else startRecording() }
            if (audioData != null) {
                FormButton("Play Recording", icon = Icons.Default.PlayCircle) { audioData?.let { SpeechManager.shared.playAudioData(it) } }
                FormButton("Remove Recording", tint = Color.Red) { audioData = null }
            }
        }
        FormSection("Picture") {
            FormRow {
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(hexColor(colorHex)).border(1.dp, BoardTheme.line, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    KeyPicture(SymbolWord(wordId, label, tts, icon, colorHex, photoData), 46.dp)
                }
                Spacer(Modifier.width(14.dp))
                Text(if (photoData != null) "Photo set" else "Using the built-in picture", fontSize = 14.sp, color = BoardTheme.slate)
            }
            PictureSourceRows(hasPicture = photoData != null, maxDimension = 1024, onPicked = { photoData = it }, onRemove = { photoData = null })
            FormButton("Choose a Different Symbol", icon = Icons.Default.Search) { showSymbols = true }
        }
        FormSection("Colour", "The colour is the Fitzgerald key - yellow people, green actions, orange things. Changing it changes what the key tells a child about the word.") {
            ColorRow("Key Colour", colorHex) { colorHex = it }
        }
        FormSection(footer = if (hidden) "This word is off the page. It still shows here in the editor, greyed, so you can put it back."
                             else "Takes this word off this page. Nothing is deleted and no other page changes.") {
            ToggleRow("Remove from this page", hidden) { hidden = it }
            FormButton("Reset to the Original Word", tint = BoardTheme.danger) {
                val base = original ?: return@FormButton
                label = base.label; tts = base.tts; icon = base.icon; colorHex = base.color
                photoData = null; audioData = null; hidden = false
            }
        }
    }

    if (showSymbols) {
        SymbolPickerSheet(set = store.settings.symbolSet, onDismiss = { showSymbols = false }) { name ->
            icon = name; photoData = null
        }
    }
}
