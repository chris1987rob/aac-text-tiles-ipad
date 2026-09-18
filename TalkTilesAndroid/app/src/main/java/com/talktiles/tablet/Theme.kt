package com.talktiles.tablet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** "#RRGGBB" / "#RGB" / "#AARRGGBB" -> Color. Anything unreadable is black, like the iPad. */
fun hexColor(hex: String): Color {
    val clean = hex.trim().trimStart('#').filter { it.isLetterOrDigit() }
    val v = clean.toLongOrNull(16) ?: return Color.Black
    return when (clean.length) {
        3 -> Color(((v shr 8) and 0xF).toInt() * 17, ((v shr 4) and 0xF).toInt() * 17, (v and 0xF).toInt() * 17)
        6 -> Color(((v shr 16) and 0xFF).toInt(), ((v shr 8) and 0xFF).toInt(), (v and 0xFF).toInt())
        8 -> Color(((v shr 16) and 0xFF).toInt(), ((v shr 8) and 0xFF).toInt(), (v and 0xFF).toInt(), ((v shr 24) and 0xFF).toInt())
        else -> Color.Black
    }
}

fun Color.toHexString(): String {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}

/** The board's look, in one place. Pastel and rounded - every hex is from the iPad's BoardTheme. */
object BoardTheme {
    val background = hexColor("#F4F7FB")
    val bar = hexColor("#FFFFFF")
    val ink = hexColor("#1F2A44")
    val inkSoft = hexColor("#5B6478")
    val accent = hexColor("#F5893B")
    val sentence = hexColor("#D8ECFA")
    val chip = hexColor("#E4E9F0")
    val clear = hexColor("#E5484D")
    val green = hexColor("#008369")
    val slate = hexColor("#64748B")
    val line = hexColor("#CBD5E1")
    val danger = hexColor("#B91C1C")
    val blue = hexColor("#0284C7")
    val rainbowColors = listOf(
        hexColor("#FF7A7A"), hexColor("#FFB35C"), hexColor("#FFE66D"),
        hexColor("#7AE582"), hexColor("#5CC8FF"), hexColor("#A78BFA"), hexColor("#FF7A7A")
    )
    val rainbow: Brush get() = Brush.sweepGradient(rainbowColors)
    val rainbowBand: Brush get() = Brush.horizontalGradient(rainbowColors.dropLast(1))
}

/** Click with no ripple - the tiles and bar buttons draw their own press state. */
fun Modifier.plainClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = enabled, onClick = onClick)
}

/** A round white bar button with a soft shadow. */
@Composable
fun RoundBarButton(
    icon: ImageVector,
    label: String,
    fill: Color = BoardTheme.bar,
    tint: Color = BoardTheme.inkSoft,
    size: Dp = 46.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(fill)
            .plainClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** The round "PLAY" button with the rainbow ring. */
@Composable
fun SpeakNowButton(size: Dp = 50.dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(5.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .border(size * 0.075f, BoardTheme.rainbow, CircleShape)
            .plainClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = BoardTheme.ink, modifier = Modifier.size(size * 0.36f))
            Text("PLAY", fontSize = (size.value * 0.17f).sp, fontWeight = FontWeight.Black, color = BoardTheme.ink, lineHeight = (size.value * 0.18f).sp)
        }
    }
}

/**
 * The light-blue pill the words collect in: words on the left, the trailing
 * controls, then the red X at the far end so a hand reaching for Play does
 * not wipe the sentence.
 */
@Composable
fun SentencePill(
    words: List<String>,
    placeholder: String,
    onClear: () -> Unit,
    onTap: () -> Unit,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(27.dp))
            .background(BoardTheme.sentence)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .plainClickable(onClick = onTap)
                .horizontalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (words.isEmpty()) {
                Text(placeholder, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = BoardTheme.inkSoft.copy(alpha = 0.7f), maxLines = 1)
            } else {
                Text(words.joinToString("  "), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = BoardTheme.ink, maxLines = 1, overflow = TextOverflow.Clip)
            }
        }
        trailing()
        Spacer(Modifier.width(10.dp))
        RoundBarButton(Icons.Default.Close, "Clear sentence", fill = BoardTheme.clear, tint = Color.White, size = 44.dp, onClick = onClear)
        Spacer(Modifier.width(8.dp))
    }
}

/** A thin rainbow band, the same ring the Play button wears, drawn as a line. */
@Composable
fun RainbowBand() {
    Box(Modifier.fillMaxWidth().height(6.dp).background(BoardTheme.rainbowBand))
}
