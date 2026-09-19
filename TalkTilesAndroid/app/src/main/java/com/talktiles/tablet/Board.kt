package com.talktiles.tablet

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// MARK: - Board

@Composable
fun BoardView(
    store: AACStore,
    onGoHome: () -> Unit,
    onSelectTile: (Int) -> Unit,
    onSelectKey: (String) -> Unit,
    onSelectHotspot: (HotspotModel) -> Unit,
    onAddHotspot: () -> Unit,
    onOpenFind: () -> Unit,
    onOpenOptions: () -> Unit,
    onOpenNewPage: () -> Unit,
    onOpenPhrases: () -> Unit
) {
    val page = store.currentPage
    val c = TT.colors
    // A page that has never had a colour chosen sits on the theme's canvas.
    val ground = if (page.bgHex.equals("#FFFFFF", ignoreCase = true)) c.canvas else hexColor(page.bgHex)

    Column(Modifier.fillMaxSize().background(ground)) {
        NavigationBarView(store, onOpenFind, onOpenOptions, onOpenNewPage, onGoHome)
        // The keyboard always builds a sentence; a grid or scene does when the page asks for it.
        if (page.express || page.type == PageType.KEYBOARD) {
            Box(Modifier.padding(horizontal = TTSpace.m, vertical = TTSpace.s)) { SentenceBar(store, onOpenPhrases = onOpenPhrases) }
        }
        Box(Modifier.fillMaxSize().weight(1f)) {
            when (page.type) {
                PageType.GRID -> TileGridView(store, onSelectTile)
                PageType.SCENE -> VisualSceneView(store, onSelectHotspot, onAddHotspot)
                PageType.KEYBOARD -> KeyboardPageView(store, onSelectKey)
            }
        }
    }
}

// MARK: - Navigation bar

/**
 * Home and the page arrows on the left, the page name and its place in the
 * book in the middle. Player: Find on the right. Editor: the name renames on
 * tap, Page options and New page on the right.
 */
@Composable
fun NavigationBarView(
    store: AACStore,
    onOpenFind: () -> Unit,
    onOpenOptions: () -> Unit,
    onOpenNewPage: () -> Unit,
    onGoHome: () -> Unit
) {
    val c = TT.colors
    var isRenaming by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    LaunchedEffect(store.currentPageIndex, store.isEditMode) { isRenaming = false }

    fun commitRename() {
        if (!isRenaming) return
        isRenaming = false
        val trimmed = draft.trim()
        if (trimmed.isNotEmpty() && trimmed != store.currentPage.title) {
            store.updateCurrentPage { it.copy(title = trimmed) }
        }
    }

    val position = store.pagePosition
    val positionText = if (position.index == 0) "${position.total} pages" else "${position.index} of ${position.total}"
    val canStep = store.canStep

    Row(
        Modifier
            .fillMaxWidth()
            .zIndex(1f)
            .shadow(if (c.highContrast) 0.dp else 3.dp)
            .background(c.surface)
            .border(if (c.highContrast) 1.dp else 0.dp, if (c.highContrast) c.ink else Color.Transparent)
            .heightIn(min = 68.dp)
            .padding(horizontal = TTSpace.m, vertical = TTSpace.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TTSpace.s)
    ) {
        BarButton(Icons.Default.Home, "Home") { commitRename(); onGoHome() }
        BarButton(Icons.Default.ChevronLeft, "Previous page", enabled = canStep) { commitRename(); store.prevPage() }
        BarButton(Icons.Default.ChevronRight, "Next page", enabled = canStep) { commitRename(); store.nextPage() }

        // Title block: shrinks before it ellipsises, never under 17sp.
        // Never fillMaxHeight here: the bar's max height is unbounded and the title would take the screen.
        Box(Modifier.weight(1f).heightIn(min = 52.dp), contentAlignment = Alignment.Center) {
            if (store.isEditMode && isRenaming) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TTSpace.s)) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        textStyle = TTType.heading.copy(color = c.ink, textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { commitRename() }),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .widthIn(min = 120.dp, max = 320.dp)
                            .heightIn(min = TTSpace.touch)
                            .clip(TTShape.small)
                            .background(c.surfaceSunken)
                            .padding(horizontal = TTSpace.m)
                            .focusRequester(focus)
                            .semantics { contentDescription = "Page name" },
                        decorationBox = { inner -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { inner() } }
                    )
                    BarButton(Icons.Default.Check, "Done renaming", filled = true, size = TTSpace.touch) { commitRename() }
                }
                LaunchedEffect(Unit) { focus.requestFocus() }
            } else {
                val title = store.currentPage.title
                val titleSize = when { title.length > 20 -> 17.sp; title.length > 12 -> 19.sp; else -> 22.sp }
                val titleModifier = if (store.isEditMode)
                    Modifier.heightIn(min = TTSpace.touch).clip(TTShape.small)
                        .accessibleClickable(label = "Rename page", ripple = false) { draft = store.currentPage.title; isRenaming = true }
                        .padding(horizontal = TTSpace.s)
                else Modifier.heightIn(min = TTSpace.touch).clip(TTShape.small)
                        .accessibleClickable(label = "Find a page or word", ripple = false, onClick = onOpenFind)
                        .padding(horizontal = TTSpace.s)
                Column(titleModifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TTSpace.xs)) {
                        Text(title, fontSize = titleSize, fontWeight = FontWeight.Bold, color = c.ink, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = (titleSize.value + 4).sp)
                        if (store.isEditMode) Icon(Icons.Default.Edit, null, tint = c.inkSoft, modifier = Modifier.size(16.dp))
                    }
                    Text(positionText, style = TTType.caption, color = c.inkSoft, maxLines = 1)
                }
            }
        }

        if (store.isEditMode) {
            BarButton(Icons.Default.Tune, "Page options") { commitRename(); onOpenOptions() }
            BarButton(Icons.Default.Add, "New page", filled = true) { commitRename(); onOpenNewPage() }
        } else {
            BarButton(Icons.Default.Search, "Find a page or word") { onOpenFind() }
        }
    }
}

// MARK: - Sentence bar

/**
 * The sentence being built, with the same controls wherever it appears:
 * the words (tap to hear them again), remove-last, saved phrases, Speak /
 * Stop, and Clear - which can be undone once, so there is no "are you sure?".
 */
@Composable
fun SentenceBar(store: AACStore, onOpenPhrases: () -> Unit = {}) {
    val c = TT.colors
    val items = store.sentence.items
    val speaking = SpeechManager.shared.isSpeaking
    val canUndo = store.sentence.canUndoClear

    Row(
        Modifier
            .fillMaxWidth()
            .clip(TTShape.large)
            .background(c.sentence)
            .border(if (c.highContrast) 2.dp else 1.dp, if (c.highContrast) c.ink else c.line, TTShape.large)
            .padding(TTSpace.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TTSpace.s)
    ) {
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = TTSpace.chrome)
                .clip(TTShape.medium)
                .accessibleClickable(label = if (items.isEmpty()) "Sentence, empty" else "Speak sentence again", ripple = false) { store.speakSentence() }
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = TTSpace.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TTSpace.s)
        ) {
            if (items.isEmpty()) {
                Text(if (canUndo) "Sentence cleared" else "Tap buttons to build a sentence",
                    style = TTType.body, color = c.inkSoft, maxLines = 1)
            } else {
                for ((i, item) in items.withIndex()) {
                    SentenceWord(item, key = "sentence-$i")
                }
            }
        }
        if (canUndo) {
            BarButton(Icons.Default.Undo, "Undo clear", size = TTSpace.touch, fill = c.accentSoft, tint = c.accent) { store.sentence.undoClear() }
        }
        BarButton(Icons.Default.Backspace, "Remove last word", size = TTSpace.touch, enabled = items.isNotEmpty()) { store.sentence.removeLast() }
        BarButton(Icons.Default.Bookmark, "Saved phrases", size = TTSpace.touch, onClick = onOpenPhrases)
        if (speaking) {
            BarButton(Icons.Default.Stop, "Stop speaking", filled = true, size = TTSpace.chrome, fill = c.accent) { SpeechManager.shared.stop() }
        } else {
            BarButton(Icons.Default.PlayArrow, "Speak sentence", filled = true, size = TTSpace.chrome, enabled = items.isNotEmpty()) { store.speakSentence() }
        }
        BarButton(Icons.Default.Close, "Clear sentence", size = TTSpace.touch, fill = c.dangerSoft, tint = c.danger, enabled = items.isNotEmpty()) { store.sentence.clear() }
    }
}

/** One word in the bar: its picture if it has one, and the label. */
@Composable
fun SentenceWord(item: SentenceItem, key: String) {
    val c = TT.colors
    Row(
        Modifier.clip(TTShape.small).background(c.surface).border(1.dp, c.line, TTShape.small).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val photo = PhotoCache.bitmap(item.photoData, key)
        when {
            photo != null -> Image(photo.asImageBitmap(), null, Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
            item.symbolName != null -> SymbolPicture(item.symbolName, 22.dp)
        }
        Text(item.label, style = TTType.bodyStrong, color = c.ink, maxLines = 1)
    }
}

// MARK: - Grid

fun gridDimensions(size: Int, isLandscape: Boolean): Pair<Int, Int> = when (size) {
    1 -> 1 to 1
    2 -> if (isLandscape) 2 to 1 else 1 to 2
    4 -> 2 to 2
    6 -> if (isLandscape) 3 to 2 else 2 to 3
    8 -> if (isLandscape) 4 to 2 else 2 to 4
    9 -> 3 to 3
    12 -> if (isLandscape) 4 to 3 else 3 to 4
    16 -> 4 to 4
    20 -> if (isLandscape) 5 to 4 else 4 to 5
    25 -> 5 to 5
    30 -> if (isLandscape) 6 to 5 else 5 to 6
    36 -> 6 to 6
    48 -> if (isLandscape) 8 to 6 else 6 to 8
    else -> { val sq = ceil(sqrt(size.toDouble())).toInt(); sq to sq }
}

private fun spacingFor(gridSize: Int): Dp = when {
    gridSize <= 4 -> 16.dp
    gridSize <= 9 -> 12.dp
    gridSize <= 16 -> 10.dp
    gridSize <= 25 -> 8.dp
    else -> 6.dp
}

private fun paddingFor(gridSize: Int): Dp = when {
    gridSize <= 4 -> 16.dp
    gridSize <= 9 -> 12.dp
    gridSize <= 16 -> 10.dp
    else -> 8.dp
}

@Composable
fun TileGridView(store: AACStore, onSelectTile: (Int) -> Unit) {
    val p = store.currentPage
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isLandscape = maxWidth >= maxHeight
        val (cols, rows) = gridDimensions(p.gridSize, isLandscape)
        val spacing = spacingFor(p.gridSize)
        val pad = paddingFor(p.gridSize)
        val cellW = ((maxWidth - pad * 2 - spacing * (cols - 1)) / cols).coerceAtLeast(48.dp)
        val cellH = ((maxHeight - pad * 2 - spacing * (rows - 1)) / rows).coerceAtLeast(48.dp)

        Column(Modifier.fillMaxSize().padding(pad), verticalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterVertically)) {
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally)) {
                    for (c in 0 until cols) {
                        val slot = r * cols + c + 1
                        if (slot <= p.gridSize) {
                            val tile = p.tiles[slot]
                            TileView(
                                activationDelay = store.settings.activationDelay,
                                activateOnRelease = store.settings.activateOnRelease,
                                tile = tile,
                                isEditMode = store.isEditMode,
                                cellWidth = cellW,
                                cellHeight = cellH,
                                slot = slot,
                                onTap = {
                                    if (store.isEditMode) {
                                        onSelectTile(slot)
                                    } else if (tile != null && (tile.label.isNotEmpty() || tile.tts.isNotEmpty())) {
                                        if (!TouchAccess.shouldFire("${p.id}-$slot", store.settings.repeatLockout)) return@TileView
                                        if (p.express) store.sentence.add(SentenceItem.from(tile))
                                        val audio = tile.audioData
                                        when {
                                            audio != null -> SpeechManager.shared.playAudioData(audio)
                                            tile.isSoundItOut -> SpeechManager.shared.soundItOut(tile.spoken)
                                            else -> SpeechManager.shared.speak(tile.spoken, store.settings.speechRate.toFloat(), store.settings.voiceId)
                                        }
                                    }
                                }
                            )
                        } else {
                            Spacer(Modifier.size(cellW, cellH))
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Tile

/**
 * A communication button. Its look is the person's own (colours, photo,
 * word size are never restyled). Touch goes through `PressController` so
 * dwell, speak-on-lift and cancelled presses behave; a screen reader or a
 * keyboard reaches the same action through semantics / Enter.
 */
@Composable
fun TileView(
    activationDelay: Double = 0.0,
    activateOnRelease: Boolean = false,
    tile: TileModel?,
    isEditMode: Boolean,
    cellWidth: Dp,
    cellHeight: Dp,
    slot: Int = 0,
    onTap: () -> Unit
) {
    val c = TT.colors
    val reduceMotion = TT.reduceMotion
    val minDim = min(cellWidth.value, cellHeight.value)
    val corner = min(26f, max(10f, minDim * 0.13f)).dp
    val pad = max(4f, min(14f, minDim * 0.05f)).dp
    val scope = rememberCoroutineScope()

    // The controller outlives recompositions whose keys match (two pages with an
    // identical button in the same slot), so it must call the CURRENT onTap - the
    // one that knows which page is on screen - not the lambda it was created with.
    val currentOnTap by rememberUpdatedState(onTap)
    val controller = remember(activationDelay, activateOnRelease, tile, isEditMode) {
        PressController(if (isEditMode) 0.0 else activationDelay, if (isEditMode) false else activateOnRelease) { currentOnTap() }
    }
    var dwell by remember { mutableStateOf<Job?>(null) }
    var flash by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()

    fun startDwell(ms: Long) {
        dwell?.cancel()
        dwell = scope.launch { delay(ms); controller.dwellElapsed() }
    }
    fun activateFromSemantics() {
        controller.activate()
        flash = true
        scope.launch { delay(250); flash = false }
    }

    val isPressed = controller.isPressed || flash
    val scale by animateFloatAsState(if (isPressed && !reduceMotion) 1.03f else 1f, tween(if (reduceMotion) 0 else 120), label = "press")

    val description = when {
        tile != null && (tile.label.isNotEmpty() || tile.tts.isNotEmpty()) -> {
            val what = tile.label.ifEmpty { tile.tts }
            val says = if (tile.spoken != what) ". Says: ${tile.spoken}" else ""
            if (isEditMode) "Edit button $what" else what + says
        }
        isEditMode -> "Empty button $slot. Tap to add"
        else -> ""
    }
    val interactive = description.isNotEmpty()

    val gesture = Modifier.pointerInput(controller) {
        detectTapGestures(
            onPress = {
                controller.press()?.let { startDwell(it) }
                val completed = tryAwaitRelease()
                dwell?.cancel(); dwell = null
                controller.release(completed)
            }
        )
    }

    Box(
        Modifier
            .size(cellWidth, cellHeight)
            .then(if (interactive) Modifier
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = description
                    onClick { activateFromSemantics(); true }
                }
                .focusable(interactionSource = source)
                .onKeyEvent { e ->
                    if (e.type == KeyEventType.KeyUp && (e.key == Key.Enter || e.key == Key.DirectionCenter || e.key == Key.Spacebar)) { activateFromSemantics(); true } else false
                }
                .then(gesture)
            else Modifier),
        contentAlignment = Alignment.Center
    ) {
        val t = tile
        if (t != null && (t.label.isNotEmpty() || t.symbolName != null || t.photoData != null)) {
            val hasLabel = t.label.isNotEmpty()
            val hasSymbol = t.symbolName != null || t.photoData != null
            val restBorder = hexColor(t.borderHex).copy(alpha = if (c.highContrast) 1f else 0.5f)
            val borderColor = when { isPressed -> c.pressed; focused -> c.focus; else -> restBorder }
            val borderWidth = when { isPressed -> 4.dp; focused -> 4.dp; c.highContrast -> 2.5.dp; else -> 1.5.dp }
            Column(
                Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .shadow(if (c.highContrast) 0.dp else 4.dp, RoundedCornerShape(corner))
                    .clip(RoundedCornerShape(corner))
                    .background(hexColor(t.bgHex))
                    .border(borderWidth, borderColor, RoundedCornerShape(corner))
                    .drawBehind {
                        // Dwell progress: a ring that fills as the hold completes.
                        val ms = controller.dwellMs
                        if (ms > 0 && controller.isPressed) {
                            val stroke = 6.dp.toPx()
                            drawArc(color = c.pressed, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                                topLeft = androidx.compose.ui.geometry.Offset(stroke, stroke), size = Size(size.width - stroke * 2, size.height - stroke * 2),
                                style = Stroke(width = stroke), alpha = 0.55f)
                        }
                    }
                    .padding(pad),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((minDim * 0.03f).dp, Alignment.CenterVertically)
            ) {
                if (t.labelPositionTop && hasLabel) TileLabel(t, hasSymbol, minDim)
                if (hasSymbol) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { TileSymbol(t, hasLabel, minDim) }
                if (!t.labelPositionTop && hasLabel) TileLabel(t, hasSymbol, minDim)
            }
        } else if (isEditMode) {
            Column(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(corner))
                    .background(c.surface.copy(alpha = 0.7f))
                    .border(if (focused) 3.dp else 0.dp, if (focused) c.focus else Color.Transparent, RoundedCornerShape(corner))
                    .drawBehind {
                        drawRoundRect(color = c.lineStrong, cornerRadius = CornerRadius(corner.toPx()),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))))
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                Icon(Icons.Default.AddCircle, null, tint = c.primary, modifier = Modifier.size(min(44f, max(24f, minDim * 0.22f)).dp))
                if (minDim > 70) Text("Add", style = TTType.label, color = c.inkSoft)
            }
        }
    }
}

@Composable
private fun TileLabel(t: TileModel, hasSymbol: Boolean, minDim: Float) {
    val base = if (hasSymbol) min(32f, max(15f, minDim * 0.13f)) else min(44f, max(18f, minDim * 0.22f))
    Text(
        t.label,
        fontSize = (base * t.labelSize).sp,
        fontWeight = FontWeight.SemiBold,
        color = hexColor(t.labelHex),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        lineHeight = (base * t.labelSize * 1.1f).sp
    )
}

private val emojiFallbacks = mapOf(
    "eat" to "🍎", "food" to "🍎", "water" to "💧", "drink" to "💧", "yes" to "✅", "no" to "❌",
    "help" to "🙋", "happy" to "😊", "sad" to "😢", "more" to "➕", "stop" to "🛑", "bathroom" to "🚻",
    "toilet" to "🚻", "play" to "🧸", "toy" to "🧸", "home" to "🏠", "house" to "🏠", "school" to "🏫",
    "sleep" to "😴", "bed" to "😴", "love" to "❤️", "like" to "❤️", "dog" to "🐶", "cat" to "🐱",
    "book" to "📖", "bus" to "🚌", "music" to "🎵"
)

@Composable
fun TileSymbol(t: TileModel, hasLabel: Boolean, minDim: Float) {
    val symSize = if (hasLabel) min(80f, max(28f, minDim * 0.38f)) else min(110f, max(36f, minDim * 0.58f))
    val photo = PhotoCache.bitmap(t.photoData, "tile-${t.id}")
    val name = t.symbolName
    when {
        photo != null -> Image(photo.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        name != null -> SymbolPicture(name, symSize.dp)
    }
}

/** Draws a stored symbol name: the twenty emoji shortcuts, any emoji, a library picture, else a star. */
@Composable
fun SymbolPicture(name: String, size: Dp, modifier: Modifier = Modifier) {
    val emoji = emojiFallbacks[name.lowercase()]
    when {
        emoji != null -> Text(emoji, fontSize = size.value.sp, modifier = modifier, lineHeight = (size.value * 1.2f).sp)
        SymbolLibrary.isEmoji(name) -> Text(name, fontSize = size.value.sp, modifier = modifier, lineHeight = (size.value * 1.2f).sp)
        else -> {
            val img = remember(name) { SymbolLibrary.image(name) }
            if (img != null) {
                Image(img.asImageBitmap(), null, modifier.size(size * 1.5f), contentScale = ContentScale.Fit)
            } else {
                Icon(Icons.Default.Star, null, tint = TT.colors.primary, modifier = modifier.size(size * 0.75f))
            }
        }
    }
}
