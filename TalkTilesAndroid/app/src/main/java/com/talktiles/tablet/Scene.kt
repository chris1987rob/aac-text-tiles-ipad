package com.talktiles.tablet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MIN_SIZE_PCT = 6.0

/** A photo with talking spots on it. In the editor the spots move and resize by dragging. */
@Composable
fun VisualSceneView(store: AACStore, onSelectHotspot: (HotspotModel) -> Unit, onAddHotspot: () -> Unit) {
    val page = store.currentPage
    var activeId by remember { mutableStateOf<Int?>(null) }
    var movingId by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    fun updateHotspot(id: Int, mutate: (HotspotModel) -> HotspotModel) {
        store.updateCurrentPage { p -> p.copy(hotspots = p.hotspots.map { if (it.id == id) mutate(it) else it }) }
    }

    fun trigger(spot: HotspotModel) {
        val live = store.currentPage   // not `page`: gesture blocks outlive one composition
        if (!TouchAccess.shouldFire("${live.id}-hotspot-${spot.id}", store.settings.repeatLockout)) return
        activeId = spot.id
        if (live.express) store.addExpressChip(spot.label.ifEmpty { spot.tts })
        when (spot.action) {
            HotspotAction.RECORDED -> {
                val a = spot.audioData
                if (a != null) SpeechManager.shared.playAudioData(a)
                else SpeechManager.shared.speak(spot.spoken, store.settings.speechRate.toFloat(), store.settings.voiceId)
            }
            HotspotAction.JUMP -> {
                val idx = store.pages.indexOfFirst { it.id == spot.jumpPageId }
                if (idx >= 0) store.currentPageIndex = idx
            }
            HotspotAction.TTS -> SpeechManager.shared.speak(spot.spoken, store.settings.speechRate.toFloat(), store.settings.voiceId)
        }
        scope.launch { delay(500); activeId = null }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val canvasW = with(density) { maxWidth.toPx() }
        val canvasH = with(density) { maxHeight.toPx() }

        SceneBackground(page)

        for (spot in page.hotspots) {
            val wPx = (spot.w / 100.0 * canvasW).toFloat()
            val hPx = (spot.h / 100.0 * canvasH).toFloat()
            val xPx = (spot.x / 100.0 * canvasW).toFloat()
            val yPx = (spot.y / 100.0 * canvasH).toFloat()
            val isActive = activeId == spot.id
            val isMoving = movingId == spot.id

            val gesture = if (store.isEditMode) {
                Modifier.pointerInput(spot.id, canvasW, canvasH) {
                    var origin: Pair<Double, Double>? = null
                    var acc = 0f to 0f
                    detectDragGestures(
                        onDragStart = {
                            // The block outlives the `spot` it captured; start from where it is NOW.
                            val live = store.currentPage.hotspots.firstOrNull { it.id == spot.id } ?: spot
                            origin = live.x to live.y; acc = 0f to 0f; movingId = spot.id
                        },
                        onDragEnd = { origin = null; movingId = null; store.save() },
                        onDragCancel = { origin = null; movingId = null },
                        onDrag = { change, drag ->
                            change.consume()
                            acc = (acc.first + drag.x) to (acc.second + drag.y)
                            val o = origin ?: return@detectDragGestures
                            val dx = acc.first / canvasW * 100.0
                            val dy = acc.second / canvasH * 100.0
                            updateHotspot(spot.id) { h ->
                                h.copy(x = (o.first + dx).coerceIn(0.0, max(0.0, 100.0 - h.w)),
                                       y = (o.second + dy).coerceIn(0.0, max(0.0, 100.0 - h.h)))
                            }
                        }
                    )
                }.pointerInput(spot.id) {
                    detectTapGestures { onSelectHotspot(store.currentPage.hotspots.firstOrNull { it.id == spot.id } ?: spot) }
                }
            } else {
                Modifier.pointerInput(spot.id) { detectTapGestures { trigger(store.currentPage.hotspots.firstOrNull { it.id == spot.id } ?: spot) } }
            }

            Box(
                Modifier
                    .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
                    .size(with(density) { max(wPx, 1f).toDp() }, with(density) { max(hPx, 1f).toDp() })
                    .then(gesture),
                contentAlignment = Alignment.Center
            ) {
                if (store.isEditMode) {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(hexColor("#3B82F6").copy(alpha = if (isMoving) 0.55f else 0.35f))
                        .drawBehind {
                            drawRoundRect(color = hexColor("#1D4ED8"), cornerRadius = CornerRadius(8.dp.toPx()),
                                style = Stroke(width = (if (isMoving) 3 else 2).dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))))
                        })
                    Row(
                        Modifier.clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.75f)).padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.OpenWith, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Text(spot.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    if (isMoving) {
                        Text("${spot.w.toInt()}% x ${spot.h.toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center).offset(y = 26.dp).clip(RoundedCornerShape(5.dp)).background(hexColor("#1D4ED8")).padding(horizontal = 7.dp, vertical = 3.dp))
                    }
                } else {
                    val fill = if (isActive) hexColor("#00E676").copy(alpha = 0.4f) else Color.Transparent
                    val stroke = when {
                        isActive -> hexColor("#00E676")
                        spot.style == HotspotStyle.HIGHLIGHT -> hexColor("#FFEB3B").copy(alpha = 0.6f)
                        else -> Color.Transparent
                    }
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(fill).border(if (isActive) 4.dp else 2.dp, stroke, RoundedCornerShape(8.dp))
                        .drawBehind {
                            if (spot.style == HotspotStyle.OUTLINE) drawRoundRect(color = Color.White.copy(alpha = 0.85f), cornerRadius = CornerRadius(8.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))))
                        })
                }
            }
        }

        // Resize handles, as siblings of the hotspots so the move gesture cannot claim them.
        if (store.isEditMode) {
            for (spot in page.hotspots) {
                val hx = ((spot.x + spot.w) / 100.0 * canvasW).toFloat()
                val hy = ((spot.y + spot.h) / 100.0 * canvasH).toFloat()
                val handlePx = with(density) { 34.dp.toPx() }
                Box(
                    Modifier
                        .offset { IntOffset((hx - handlePx / 2).roundToInt(), (hy - handlePx / 2).roundToInt()) }
                        .size(34.dp)
                        .pointerInput(spot.id, canvasW, canvasH) {
                            var origin: Pair<Double, Double>? = null
                            var acc = 0f to 0f
                            detectDragGestures(
                                onDragStart = {
                                    val live = store.currentPage.hotspots.firstOrNull { it.id == spot.id } ?: spot
                                    origin = live.w to live.h; acc = 0f to 0f; movingId = spot.id
                                },
                                onDragEnd = { origin = null; movingId = null; store.save() },
                                onDragCancel = { origin = null; movingId = null },
                                onDrag = { change, drag ->
                                    change.consume()
                                    acc = (acc.first + drag.x) to (acc.second + drag.y)
                                    val o = origin ?: return@detectDragGestures
                                    val dw = acc.first / canvasW * 100.0
                                    val dh = acc.second / canvasH * 100.0
                                    updateHotspot(spot.id) { h ->
                                        h.copy(w = min(max(o.first + dw, MIN_SIZE_PCT), 100.0 - h.x),
                                               h = min(max(o.second + dh, MIN_SIZE_PCT), 100.0 - h.y))
                                    }
                                }
                            )
                        }
                        .clip(CircleShape).background(Color.White).border(3.dp, hexColor("#1D4ED8"), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SouthEast, null, tint = hexColor("#1D4ED8"), modifier = Modifier.size(14.dp))
                }
            }

            // Editor option bar
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(14.dp)
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.94f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.TouchApp, null, tint = hexColor("#475569"), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                val n = page.hotspots.size
                Text(if (n == 0) "No hotspots yet" else "$n hotspot${if (n == 1) "" else "s"}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = hexColor("#475569"))
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.clip(RoundedCornerShape(9.dp)).background(BoardTheme.green).plainClickable(onClick = onAddHotspot).padding(horizontal = 13.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.AddCircle, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text("Add Hotspot", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SceneBackground(page: PageModel) {
    val img = PhotoCache.bitmap(page.sceneImageData, "scene-${page.id}")
    if (img != null) {
        Image(img.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    } else {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(hexColor("#DBEAFE"), hexColor("#BFDBFE"))))) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(180.dp).background(Brush.verticalGradient(listOf(hexColor("#B45309"), hexColor("#78350F")))))
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Living Room Visual Scene Display", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = hexColor("#1E3A8A"))
                Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                    Box(Modifier.size(320.dp, 160.dp).clip(RoundedCornerShape(16.dp)).background(hexColor("#3B82F6")), contentAlignment = Alignment.Center) {
                        Text("🛋️ Sofa / Couch", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    Box(Modifier.size(220.dp, 140.dp).clip(RoundedCornerShape(10.dp)).background(hexColor("#1E293B")), contentAlignment = Alignment.Center) {
                        Text("📺 Television", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
            }
        }
    }
}
