package com.talktiles.tablet

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
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
    onOpenPages: () -> Unit,
    onOpenOptions: () -> Unit,
    onOpenNewPage: () -> Unit
) {
    val page = store.currentPage
    // A page that has never had a colour chosen sits on the theme's pale ground.
    val ground = if (page.bgHex.equals("#FFFFFF", ignoreCase = true)) BoardTheme.background else hexColor(page.bgHex)

    Column(Modifier.fillMaxSize().background(ground)) {
        NavigationBarView(store, onOpenPages, onOpenOptions, onOpenNewPage, onGoHome)
        if (page.express && page.type != PageType.KEYBOARD) {
            ExpressBarView(store)
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
 * Player: Back and Home on the left, the page name large in the middle (tap
 * for the page list). Editor: Back, Home, Options on the left, New Page on
 * the right, and the name is tap-to-rename.
 */
@Composable
fun NavigationBarView(
    store: AACStore,
    onOpenPages: () -> Unit,
    onOpenOptions: () -> Unit,
    onOpenNewPage: () -> Unit,
    onGoHome: () -> Unit
) {
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

    Box(
        Modifier
            .fillMaxWidth()
            .zIndex(1f)
            .shadow(4.dp)
            .background(BoardTheme.bar)
            .height(64.dp)
            .padding(horizontal = 16.dp)
    ) {
        // The name is centred on the screen, not on the gap between button
        // groups. This tablet is narrower than an iPad, so a long name shrinks
        // rather than being cut off.
        val title = store.currentPage.title.uppercase()
        val sidePad = if (store.isEditMode) 180.dp else 120.dp
        val titleSize = when {
            store.isEditMode -> when { title.length > 16 -> 14.sp; title.length > 10 -> 17.sp; else -> 21.sp }
            title.length > 18 -> 17.sp
            title.length > 12 -> 22.sp
            else -> 30.sp
        }
        val titleSpacing = if (title.length > 12 || store.isEditMode) 1.sp else 3.5.sp
        Box(Modifier.fillMaxSize().padding(horizontal = sidePad), contentAlignment = Alignment.Center) {
            if (store.isEditMode) {
                if (isRenaming) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BoardTheme.ink, textAlign = TextAlign.Center),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { commitRename() }),
                            modifier = Modifier
                                .width(280.dp)
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(BoardTheme.sentence)
                                .padding(horizontal = 12.dp)
                                .focusRequester(focus),
                            decorationBox = { inner -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { inner() } }
                        )
                        RoundBarButton(Icons.Default.Check, "Done renaming", fill = hexColor("#3CC47C"), tint = Color.White, size = 40.dp) { commitRename() }
                    }
                    LaunchedEffect(Unit) { focus.requestFocus() }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            Modifier.heightIn(min = 44.dp).plainClickable { draft = store.currentPage.title; isRenaming = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(title, fontSize = titleSize, fontWeight = FontWeight.Bold,
                                letterSpacing = titleSpacing, color = BoardTheme.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.Edit, "Rename page", tint = BoardTheme.inkSoft, modifier = Modifier.size(18.dp))
                        }
                        Box(Modifier.width(36.dp).height(44.dp).plainClickable(onClick = onOpenPages), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.KeyboardArrowDown, "Pages in this book", tint = BoardTheme.inkSoft)
                        }
                    }
                }
            } else {
                Row(
                    Modifier.plainClickable(onClick = onOpenPages),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(title, fontSize = titleSize, fontWeight = FontWeight.Bold,
                        letterSpacing = titleSpacing, color = BoardTheme.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = BoardTheme.inkSoft)
                }
            }
        }

        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundBarButton(Icons.Default.ArrowBack, "Back") { commitRename(); store.prevPage() }
                RoundBarButton(Icons.Default.Home, "Home", fill = BoardTheme.accent, tint = Color.White) { commitRename(); onGoHome() }
                if (store.isEditMode) {
                    RoundBarButton(Icons.Default.Tune, "Page options") { commitRename(); onOpenOptions() }
                }
            }
            Spacer(Modifier.weight(1f))
            if (store.isEditMode) {
                RoundBarButton(Icons.Default.Add, "New page", fill = BoardTheme.accent, tint = Color.White) { commitRename(); onOpenNewPage() }
            }
        }
    }
}

// MARK: - Sentence bar

@Composable
fun ExpressBarView(store: AACStore) {
    Box(Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp)) {
        SentencePill(
            words = store.expressChips.toList(),
            placeholder = "Tap tiles to build a sentence",
            onClear = { store.clearExpressChips() },
            onTap = { store.playExpressSentence() }
        ) {
            SpeakNowButton { store.playExpressSentence() }
        }
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
        val cellW = ((maxWidth - pad * 2 - spacing * (cols - 1)) / cols).coerceAtLeast(50.dp)
        val cellH = ((maxHeight - pad * 2 - spacing * (rows - 1)) / rows).coerceAtLeast(50.dp)

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
                                onTap = {
                                    if (store.isEditMode) {
                                        onSelectTile(slot)
                                    } else if (tile != null && (tile.label.isNotEmpty() || tile.tts.isNotEmpty())) {
                                        if (!TouchAccess.shouldFire("${p.id}-$slot", store.settings.repeatLockout)) return@TileView
                                        if (p.express) store.addExpressChip(tile.label.ifEmpty { tile.tts })
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

@Composable
fun TileView(
    activationDelay: Double = 0.0,
    activateOnRelease: Boolean = false,
    tile: TileModel?,
    isEditMode: Boolean,
    cellWidth: Dp,
    cellHeight: Dp,
    onTap: () -> Unit
) {
    val minDim = min(cellWidth.value, cellHeight.value)
    val corner = min(26f, max(10f, minDim * 0.13f)).dp
    val pad = max(4f, min(14f, minDim * 0.05f)).dp
    var isPressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var dwell by remember { mutableStateOf<Job?>(null) }
    var didFire by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 1.03f else 1f, tween(120), label = "press")

    fun fire() {
        if (didFire) return
        didFire = true
        onTap()
        scope.launch { delay(300); isPressed = false }
    }

    val pressModifier = if (isEditMode) {
        Modifier.plainClickable(onClick = onTap)
    } else {
        Modifier.pointerInput(activationDelay, activateOnRelease, tile) {
            detectTapGestures(
                onPress = {
                    didFire = false
                    isPressed = true
                    if (activationDelay > 0) {
                        // Dwell: the finger has to stay put. Lifting early cancels.
                        dwell = scope.launch { delay((activationDelay * 1000).toLong()); fire() }
                    } else if (!activateOnRelease) {
                        fire()
                    }
                    tryAwaitRelease()
                    dwell?.cancel(); dwell = null
                    if (activationDelay == 0.0 && activateOnRelease) fire()
                    isPressed = false
                }
            )
        }
    }

    Box(Modifier.size(cellWidth, cellHeight).then(pressModifier), contentAlignment = Alignment.Center) {
        val t = tile
        if (t != null && (t.label.isNotEmpty() || t.symbolName != null || t.photoData != null)) {
            val hasLabel = t.label.isNotEmpty()
            val hasSymbol = t.symbolName != null || t.photoData != null
            val borderColor by animateColorAsState(
                if (isPressed) hexColor("#00E676") else hexColor(t.borderHex).copy(alpha = 0.35f), tween(120), label = "border")
            Column(
                Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .shadow(6.dp, RoundedCornerShape(corner))
                    .clip(RoundedCornerShape(corner))
                    .background(hexColor(t.bgHex))
                    .border(if (isPressed) 4.dp else 1.5.dp, borderColor, RoundedCornerShape(corner))
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
                    .background(hexColor("#F8FAFC"))
                    .drawBehind {
                        drawRoundRect(color = hexColor("#CBD5E1"), cornerRadius = CornerRadius(corner.toPx()),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))))
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                Icon(Icons.Default.AddCircle, "Add", tint = BoardTheme.green, modifier = Modifier.size(min(44f, max(24f, minDim * 0.22f)).dp))
                if (minDim > 70) Text("Tap to Add", fontSize = min(17f, max(12f, minDim * 0.11f)).sp, fontWeight = FontWeight.Bold, color = BoardTheme.slate)
            }
        }
    }
}

@Composable
private fun TileLabel(t: TileModel, hasSymbol: Boolean, minDim: Float) {
    val base = if (hasSymbol) min(32f, max(14f, minDim * 0.13f)) else min(44f, max(18f, minDim * 0.22f))
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
                Image(img.asImageBitmap(), SymbolLibrary.readable(name), modifier.size(size * 1.5f), contentScale = ContentScale.Fit)
            } else {
                Icon(Icons.Default.Star, null, tint = BoardTheme.green, modifier = modifier.size(size * 0.75f))
            }
        }
    }
}
