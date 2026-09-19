package com.talktiles.tablet

import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsSheet(store: AACStore, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val s = store.settings
    var confirmReset by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }
    var showVoiceMenu by remember { mutableStateOf(false) }
    // What Android says, not what was asked for: startLockTask() first shows a system
    // question ("Got it" / "No thanks"), so the row must follow the real state.
    var pinned by remember { mutableStateOf(context.isInLockTask()) }
    LaunchedEffect(Unit) { while (true) { pinned = context.isInLockTask(); delay(500) } }
    var pendingRestore by remember { mutableStateOf<Pair<BookArchive, BookBackup.Summary>?>(null) }
    var restoreError by remember { mutableStateOf<String?>(null) }
    var backupNote by remember { mutableStateOf<String?>(null) }

    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            store.saveNow()
            val text = BookBackup.encode(store.pages, store.settings, TileFavorites.shared.items, PhraseLibrary.shared.items)
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } ?: throw IllegalStateException("could not open the file")
            backupNote = "Backup saved."
            restoreError = null
        } catch (e: Exception) { restoreError = "Could not save the backup: ${e.message ?: e.javaClass.simpleName}" }
    }

    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: throw IllegalStateException("empty")
            pendingRestore = BookBackup.read(text)
            restoreError = null
        } catch (e: Exception) {
            restoreError = e.message?.takeIf { e is IllegalArgumentException } ?: "Could not read that file."
        }
    }

    val speedLabel = when {
        s.speechRate < 0.38 -> "Slow"
        s.speechRate < 0.52 -> "Normal"
        else -> "Fast"
    }

    ModalSheet(title = "Settings", onDismiss = onDismiss, leading = null, trailing = "Done", onTrailing = onDismiss) {
        // Voice: one button that opens the voice menu, so the section stays short.
        FormSection("Voice", if (VoiceClips.isAvailable)
            "Bella is the app's own recorded voice and speaks every Talk Tiles picture. The device voices read anything typed or edited. More of those can be added under Android Settings › Accessibility › Text-to-speech output."
        else "Pick a voice that fits the person speaking.") {
            val currentVoice = if (s.voiceId == null) "Device default"
                else VoiceClips.voice(s.voiceId)?.let { "${it.name} – Talk Tiles voice" } ?: s.voiceId
            FormRow(onClick = { showVoiceMenu = true }) {
                Icon(Icons.Default.RecordVoiceOver, null, tint = BoardTheme.blue)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Voice", fontSize = 16.sp, color = BoardTheme.ink)
                    Text(currentVoice, fontSize = 13.sp, color = BoardTheme.slate)
                }
                Icon(Icons.Default.KeyboardArrowRight, null, tint = BoardTheme.slate)
            }
            SliderRow("Speaking speed", speedLabel, s.speechRate.toFloat(), 0.3f..0.7f, 7) { v -> store.updateSettings { it.copy(speechRate = (Math.round(v * 20) / 20.0)) } }
            FormButton("Test the voice") {
                val clips = VoiceClips.previewClips(s.voiceId)
                if (clips.isNotEmpty()) SpeechManager.shared.playClipSequence(clips, s.speechRate.toFloat())
                else SpeechManager.shared.speak("Hello. This is how I will sound.", s.speechRate.toFloat(), s.voiceId)
            }
        }

        // Pictures
        FormSection("Pictures", "The set the symbol picker opens on when you choose a picture for a button. You can still switch sets inside the picker, and buttons already made keep their pictures.") {
            val sets = SymbolSet.values().filter { SymbolLibrary.has(it) }
            Box(Modifier.padding(12.dp)) { SegmentedPicker(sets, s.symbolSet, { it.title }, { set -> store.updateSettings { it.copy(symbolSet = set) } }) }
            for (set in sets) {
                FormRow(onClick = { store.updateSettings { it.copy(symbolSet = set) } }) {
                    Row(Modifier.width(114.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val names = if (set == SymbolSet.TALK_TILES) listOf("tt:cat", "tt:dog", "tt:apple") else listOf("cat", "dog", "apple")
                        for (n in names) {
                            val img = remember(n) { SymbolLibrary.image(n) }
                            if (img != null) Image(img.asImageBitmap(), null, Modifier.size(34.dp), contentScale = ContentScale.Fit)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(set.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BoardTheme.ink)
                            if (set == s.symbolSet) Icon(Icons.Default.CheckCircle, null, tint = BoardTheme.green, modifier = Modifier.size(18.dp))
                        }
                        Text("${SymbolLibrary.count(set)} pictures. ${set.blurb}", fontSize = 13.sp, color = BoardTheme.slate)
                    }
                }
            }
        }

        // Touch
        FormSection("Touch", "For a child whose aim is unsteady. Hold-to-speak ignores a hand resting on the screen; the repeat pause stops a tremor turning one press into five.") {
            SliderRow("Hold to speak", if (s.activationDelay == 0.0) "Off" else String.format("%.1fs", s.activationDelay), s.activationDelay.toFloat(), 0f..2f, 19) { v ->
                store.updateSettings { it.copy(activationDelay = Math.round(v * 10) / 10.0) }
            }
            SliderRow("Pause before repeat", if (s.repeatLockout == 0.0) "Off" else String.format("%.1fs", s.repeatLockout), s.repeatLockout.toFloat(), 0f..3f, 29) { v ->
                store.updateSettings { it.copy(repeatLockout = Math.round(v * 10) / 10.0) }
            }
            ToggleRow("Speak when the finger lifts", s.activateOnRelease) { v -> store.updateSettings { it.copy(activateOnRelease = v) } }
        }

        // Display
        FormSection("Display", "High contrast deepens the text and borders and removes the soft shadows. Reduce motion turns off the press animation. The book can open on the page it was last on, or always on its first page.") {
            ToggleRow("High contrast", s.highContrast) { v -> store.updateSettings { it.copy(highContrast = v) } }
            ToggleRow("Reduce motion", s.reduceMotion) { v -> store.updateSettings { it.copy(reduceMotion = v) } }
            ToggleRow("Open on the last page used", s.openOnLastPage) { v -> store.updateSettings { it.copy(openOnLastPage = v) } }
        }

        // Protect editing
        FormSection("Protect editing", "With protection on, Edit pages, Page library and Settings ask for a four-digit PIN. Talking, Find and the sentence bar never do.") {
            ToggleRow("Ask for a PIN before editing", s.childLock) { v -> store.updateSettings { it.copy(childLock = v) } }
            FormButton("Change PIN") { showPin = true }
        }

        // Keep on screen (Android screen pinning, the soft kind: Back + Overview held together leaves it)
        FormSection("Keep on screen", if (pinned) "Talk Tiles is pinned to the screen. To leave, hold Back and Overview together."
            else "Uses Android's screen pinning so Talk Tiles stays in front. Android asks once to confirm. Every button keeps working; to leave, hold Back and Overview together.") {
            FormButton(if (pinned) "Stop keeping on screen" else "Keep Talk Tiles on screen", icon = Icons.Default.PushPin) {
                val activity = context.findActivity()
                try {
                    if (pinned) activity?.stopLockTask() else activity?.startLockTask()
                    pinned = context.isInLockTask()
                } catch (e: Exception) { restoreError = "Screen pinning is not available on this device." }
            }
        }

        // Backup
        FormSection("Backup", "A backup holds every page, button, picture, recording, saved button and saved phrase in one file. Save it to Files or share it. Do this before changing devices - there is no other way to get a book back.") {
            FormButton("Save a backup to Files", tint = BoardTheme.ink, icon = Icons.Default.Save) {
                restoreError = null
                saveBackup.launch(BookBackup.fileName())
            }
            FormButton("Share a backup", tint = BoardTheme.ink, icon = Icons.Default.FileUpload) {
                try {
                    store.saveNow()
                    shareFile(context, BookBackup.write(context, store.pages, store.settings, TileFavorites.shared.items, PhraseLibrary.shared.items))
                    backupNote = null
                } catch (e: Exception) { restoreError = "Could not make a backup: ${e.message}" }
            }
            FormButton("Restore from a backup", tint = BoardTheme.ink, icon = Icons.Default.FileDownload) {
                restoreError = null
                openBackup.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
            }
            backupNote?.let { Text(it, fontSize = 13.sp, color = hexColor("#2C7A57"), modifier = Modifier.padding(16.dp)) }
            restoreError?.let { Text(it, fontSize = 13.sp, color = BoardTheme.danger, modifier = Modifier.padding(16.dp)) }
            pendingRestore?.let { (archive, summary) ->
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Backup found", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BoardTheme.ink)
                    Text("${summary.pages} pages · ${summary.buttons} buttons · ${summary.hotspots} talking spots · ${summary.savedButtons} saved buttons · ${summary.phrases} phrases", style = TTType.caption, color = BoardTheme.slate)
                    Text("Saved ${summary.createdAt}", style = TTType.caption, color = BoardTheme.slate)
                    Text("Restoring replaces the book on this tablet. The current book is kept in a snapshot first.", style = TTType.caption, color = BoardTheme.danger)
                    Row(horizontalArrangement = Arrangement.spacedBy(TTSpace.m)) {
                        PrimaryButton("Restore", minHeight = TTSpace.touch) {
                            store.restore(archive)
                            pendingRestore = null
                            backupNote = "Restored ${archive.pages.size} pages."
                        }
                        SecondaryButton("Cancel") { pendingRestore = null }
                    }
                }
            }
        }

        // This book
        FormSection("This book") {
            InfoRow("Pages", "${store.pages.size}")
            InfoRow("Buttons filled in", "${store.pages.sumOf { it.tiles.size }}")
            InfoRow("Talking spots", "${store.pages.sumOf { it.hotspots.size }}")
            InfoRow("Saved buttons", "${TileFavorites.shared.items.size}")
            InfoRow("Saved phrases", "${PhraseLibrary.shared.items.size}")
        }

        val notices = store.recoveryNotices
        val speechProblem = SpeechManager.shared.lastProblem
        if (notices.isNotEmpty() || speechProblem != null) {
            FormSection("Needs attention") {
                for (n in notices) Text(n, style = TTType.caption, color = BoardTheme.danger, modifier = Modifier.padding(16.dp))
                if (speechProblem != null) Text(speechProblem, style = TTType.caption, color = BoardTheme.danger, modifier = Modifier.padding(16.dp))
            }
        }

        FormSection("Start over", "Puts the original starter book back. The book that is there now is kept in a snapshot inside the app, but a backup file is still the safe way to keep it.") {
            FormButton("Reset to the starter book", tint = BoardTheme.danger) { confirmReset = true }
        }

        FormSection("About") {
            if (SymbolLibrary.has(SymbolSet.TALK_TILES)) AboutRow("${SymbolLibrary.count(SymbolSet.TALK_TILES)} Talk Tiles pictures", "Drawn in-house for Talk Tiles. No third-party licence.")
            if (SymbolLibrary.has(SymbolSet.MULBERRY)) AboutRow("${SymbolLibrary.count(SymbolSet.MULBERRY)} Mulberry Symbols", SymbolLibrary.ATTRIBUTION)
            for (v in VoiceClips.available) AboutRow("${v.name} voice", "Recorded for Talk Tiles: one clip for every picture and built-in phrase.")
            AboutRow("Talk Tiles for Android ${BuildConfig.VERSION_NAME}", "The same book format as Talk Tiles for iPad - backups and shared pages move between the two.")
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset everything?") },
            text = { Text("Every page, button and talking spot you have made is replaced by the starter book. A snapshot of the current book is kept inside the app; a backup file is safer still.") },
            confirmButton = { TextButton({ store.resetToDefaults(); confirmReset = false }) { Text("Reset", color = BoardTheme.danger) } },
            dismissButton = { TextButton({ confirmReset = false }) { Text("Cancel") } }
        )
    }
    if (showPin) {
        PinChangeSheet(onDismiss = { showPin = false }) { pin -> store.updateSettings { it.copy(lockPIN = pin) } }
    }
    if (showVoiceMenu) {
        VoiceMenuSheet(store, onDismiss = { showVoiceMenu = false })
    }
}

/** The voice menu: every voice in one place. Pressing a voice hears it first
 *  (the preview plays, then it is chosen), because picking a voice you have
 *  not heard is guessing. */
@Composable
private fun VoiceMenuSheet(store: AACStore, onDismiss: () -> Unit) {
    val s = store.settings
    val systemVoices = remember { SpeechManager.shared.availableSystemVoices() }
    ModalSheet(title = "Voice", onDismiss = onDismiss, trailing = "Done", onTrailing = onDismiss) {
        FormSection(if (VoiceClips.isAvailable)
            "Press a voice to hear it - it becomes the voice Talk Tiles uses."
        else "Pick a voice that fits the person speaking.") {
            for (v in VoiceClips.available) {
                VoiceRow("${v.name} – Talk Tiles voice", s.voiceId == v.id) {
                    SpeechManager.shared.playClipSequence(VoiceClips.previewClips(v.id), s.speechRate.toFloat())
                    store.updateSettings { it.copy(voiceId = v.id) }
                }
            }
            VoiceRow("Device default", s.voiceId == null) {
                SpeechManager.shared.speak("Hello. This is how I will sound.", s.speechRate.toFloat(), null)
                store.updateSettings { it.copy(voiceId = null) }
            }
            for (v in systemVoices) {
                VoiceRow(v.title, s.voiceId == v.id) {
                    SpeechManager.shared.speak("Hello. This is how I will sound.", s.speechRate.toFloat(), v.id)
                    store.updateSettings { it.copy(voiceId = v.id) }
                }
            }
            VoiceClips.voice(s.voiceId)?.let { v ->
                Text((if (v.description.isEmpty()) "A recorded voice" else v.description) + ". Every built-in picture and phrase has a clip; anything she has no clip for, the device voice reads out.",
                    fontSize = 13.sp, color = BoardTheme.slate, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun VoiceRow(title: String, selected: Boolean, onClick: () -> Unit) {
    FormRow(onClick = onClick) {
        Text(title, fontSize = 16.sp, color = BoardTheme.ink, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Default.CheckCircle, null, tint = BoardTheme.green)
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    FormRow {
        Text(title, fontSize = 16.sp, color = BoardTheme.ink, modifier = Modifier.weight(1f))
        Text(value, fontSize = 16.sp, color = BoardTheme.slate)
    }
}

@Composable
private fun AboutRow(title: String, sub: String) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BoardTheme.ink)
        Text(sub, fontSize = 13.sp, color = BoardTheme.slate)
    }
}

/** The activity behind a Compose context, through any theme wrappers. */
fun android.content.Context.findActivity(): android.app.Activity? {
    var c: android.content.Context = this
    while (c is android.content.ContextWrapper) { if (c is android.app.Activity) return c; c = c.baseContext }
    return null
}

fun android.content.Context.isInLockTask(): Boolean = try {
    val am = getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    am.lockTaskModeState != android.app.ActivityManager.LOCK_TASK_MODE_NONE
} catch (e: Exception) { false }

/** Four digits, entered twice. */
@Composable
fun PinChangeSheet(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    ModalSheet(title = "Change PIN", onDismiss = onDismiss, trailing = "Save", onTrailing = {
        val digits = first.filter { it.isDigit() }
        when {
            digits.length != 4 || digits != first -> message = "The PIN needs to be exactly four digits."
            first != second -> message = "Those two do not match."
            else -> { onSave(digits); onDismiss() }
        }
    }) {
        FormSection(footer = "Four digits. This stops a child getting into the editor; it is not a password and is stored as plain text.") {
            FormRow { PlainTextField(first, { first = it.take(4) }, "New PIN", Modifier.weight(1f), keyboardType = KeyboardType.NumberPassword) }
            FormRow { PlainTextField(second, { second = it.take(4) }, "Enter it again", Modifier.weight(1f), keyboardType = KeyboardType.NumberPassword) }
        }
        message?.let { Text(it, color = BoardTheme.danger, fontSize = 13.sp, modifier = Modifier.padding(16.dp)) }
    }
}
