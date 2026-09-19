package com.talktiles.tablet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
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

// MARK: - Design system

/**
 * The app's palette. Calm and neutral so the person's own tile colours are
 * what carries meaning: a light canvas, strong ink, one teal-blue primary and
 * one restrained warm accent. Every ink on every surface is WCAG AA for
 * normal text in THIS palette (PaletteContrastTest); `highContrast` deepens
 * every value further and takes out the translucency.
 */
data class Palette(
    val canvas: Color,
    val surface: Color,
    val surfaceSunken: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val line: Color,
    val lineStrong: Color,
    val primary: Color,
    val primaryDeep: Color,
    val primarySoft: Color,
    val onPrimary: Color,
    val accent: Color,
    val accentSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val success: Color,
    val sentence: Color,
    val focus: Color,
    val pressed: Color,
    val highContrast: Boolean
) {
    companion object {
        val normal = Palette(
            canvas = hexColor("#F4F6F8"), surface = hexColor("#FFFFFF"), surfaceSunken = hexColor("#EBEFF3"),
            ink = hexColor("#16202B"), inkSoft = hexColor("#4B5867"), inkFaint = hexColor("#556270"),
            line = hexColor("#D6DDE4"), lineStrong = hexColor("#6F7C89"),
            primary = hexColor("#0F6E8C"), primaryDeep = hexColor("#0A536A"), primarySoft = hexColor("#DCEEF4"), onPrimary = Color.White,
            accent = hexColor("#8F5311"), accentSoft = hexColor("#FBEBD6"),
            danger = hexColor("#B3261E"), dangerSoft = hexColor("#FBE4E2"), success = hexColor("#176B43"),
            sentence = hexColor("#E6F0F4"), focus = hexColor("#0F6E8C"), pressed = hexColor("#176B43"),
            highContrast = false
        )
        val highContrast = Palette(
            canvas = Color.White, surface = Color.White, surfaceSunken = hexColor("#E6E9ED"),
            ink = Color.Black, inkSoft = hexColor("#1F2933"), inkFaint = hexColor("#3E4C59"),
            line = hexColor("#7B8794"), lineStrong = hexColor("#3E4C59"),
            primary = hexColor("#084C63"), primaryDeep = hexColor("#04303F"), primarySoft = hexColor("#CFE5EC"), onPrimary = Color.White,
            accent = hexColor("#8A4B0A"), accentSoft = hexColor("#F6E3C8"),
            danger = hexColor("#8C1D18"), dangerSoft = hexColor("#F6D7D5"), success = hexColor("#0F5F38"),
            sentence = hexColor("#D9E7EE"), focus = Color.Black, pressed = hexColor("#0F5F38"),
            highContrast = true
        )
    }
}

val LocalPalette = staticCompositionLocalOf { Palette.normal }
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Type scale. Nothing on a communication screen is under 15sp. */
object TTType {
    val display = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp, letterSpacing = (-0.3).sp)
    val title = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
    val heading = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp)
    val body = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp)
    val bodyStrong = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp)
    val label = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
    val caption = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 19.sp)
    val overline = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp, letterSpacing = 0.6.sp)
}

object TTShape {
    val small = RoundedCornerShape(10.dp)
    val medium = RoundedCornerShape(16.dp)
    val large = RoundedCornerShape(22.dp)
    val pill = RoundedCornerShape(50)
}

object TTSpace {
    val xs = 4.dp; val s = 8.dp; val m = 12.dp; val l = 16.dp; val xl = 24.dp; val xxl = 32.dp
    /** Smallest touch target anywhere; communication chrome is bigger. */
    val touch = 48.dp
    val chrome = 56.dp
}

/** The theme root. Wrap every screen (and every test) in this. */
@Composable
fun TalkTilesTheme(highContrast: Boolean = false, reduceMotion: Boolean = false, content: @Composable () -> Unit) {
    val palette = if (highContrast) Palette.highContrast else Palette.normal
    val scheme = lightColorScheme(
        primary = palette.primary, onPrimary = palette.onPrimary, primaryContainer = palette.primarySoft,
        background = palette.canvas, surface = palette.surface, onSurface = palette.ink, onBackground = palette.ink,
        outline = palette.line, error = palette.danger, secondary = palette.accent
    )
    val typography = Typography(
        displaySmall = TTType.display, titleLarge = TTType.title, titleMedium = TTType.heading,
        bodyLarge = TTType.body, bodyMedium = TTType.body, labelLarge = TTType.label, bodySmall = TTType.caption
    )
    CompositionLocalProvider(LocalPalette provides palette, LocalReduceMotion provides reduceMotion) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}

/** Short hand for the current palette inside composables. */
object TT {
    val colors: Palette @Composable get() = LocalPalette.current
    val reduceMotion: Boolean @Composable get() = LocalReduceMotion.current
}

/**
 * The static palette older code reads. Same values as `Palette.normal`;
 * screens that must honour high contrast read `TT.colors` instead.
 */
object BoardTheme {
    val background get() = Palette.normal.canvas
    val bar get() = Palette.normal.surface
    val ink get() = Palette.normal.ink
    val inkSoft get() = Palette.normal.inkSoft
    val accent get() = Palette.normal.accent
    val sentence get() = Palette.normal.sentence
    val chip get() = Palette.normal.surfaceSunken
    val clear get() = Palette.normal.danger
    val green get() = Palette.normal.primary
    val slate get() = Palette.normal.inkSoft
    val line get() = Palette.normal.line
    val danger get() = Palette.normal.danger
    val blue get() = Palette.normal.primary
}

// MARK: - Interaction

/**
 * A click that shows where keyboard focus is (a ring in the focus colour)
 * and, for chrome, a soft press ripple. Every control that is not a tile uses
 * this, so a D-pad, a switch interface or TalkBack can drive the whole app.
 */
fun Modifier.accessibleClickable(
    label: String? = null,
    role: Role = Role.Button,
    enabled: Boolean = true,
    ripple: Boolean = true,
    shape: Shape = TTShape.small,
    onClick: () -> Unit
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val focus = LocalPalette.current.focus
    this
        .then(if (focused) Modifier.border(3.dp, focus, shape) else Modifier)
        .clickable(
            interactionSource = source,
            indication = if (ripple) rememberRipple(bounded = true) else null,
            enabled = enabled, role = role, onClickLabel = label, onClick = onClick
        )
}

/** Click with no ripple and no role beyond Button - for rows and text links that draw their own state. */
fun Modifier.plainClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    accessibleClickable(enabled = enabled, ripple = false, onClick = onClick)

/** Press state for something that draws its own feedback. */
@Composable
fun rememberPressed(source: MutableInteractionSource): Boolean {
    val pressed by source.collectIsPressedAsState()
    return pressed
}

// MARK: - Buttons

/**
 * A round bar button. 52dp on the communication chrome so it is never the
 * thing a hand misses; `filled` is the one strong action in a row.
 */
@Composable
fun BarButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    tint: Color? = null,
    fill: Color? = null,
    size: Dp = 52.dp,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val c = TT.colors
    val background = fill ?: if (filled) c.primary else c.surface
    val content = tint ?: if (filled) c.onPrimary else c.ink
    Box(
        modifier
            .size(size.coerceAtLeast(TTSpace.touch))
            .shadow(if (c.highContrast) 0.dp else 2.dp, CircleShape)
            .clip(CircleShape)
            .background(if (enabled) background else c.surfaceSunken)
            .border(if (c.highContrast) 2.dp else 1.dp, if (c.highContrast) c.ink else c.line.copy(alpha = if (filled) 0f else 1f), CircleShape)
            .accessibleClickable(label = label, enabled = enabled, shape = CircleShape, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = if (enabled) content else c.inkFaint, modifier = Modifier.size(size * 0.48f))
    }
}

/** Kept for older call sites; the same thing as `BarButton`. */
@Composable
fun RoundBarButton(
    icon: ImageVector,
    label: String,
    fill: Color = Palette.normal.surface,
    tint: Color = Palette.normal.ink,
    size: Dp = 48.dp,
    onClick: () -> Unit
) = BarButton(icon = icon, label = label, fill = fill, tint = tint, size = size, onClick = onClick)

/** The one strong action on a screen. */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    minHeight: Dp = TTSpace.chrome,
    onClick: () -> Unit
) {
    val c = TT.colors
    Row(
        modifier
            .heightIn(min = minHeight)
            .shadow(if (c.highContrast) 0.dp else 3.dp, TTShape.medium)
            .clip(TTShape.medium)
            .background(if (enabled) c.primary else c.surfaceSunken)
            .accessibleClickable(label = text, enabled = enabled, shape = TTShape.medium, onClick = onClick)
            .padding(horizontal = TTSpace.xl, vertical = TTSpace.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) { Icon(icon, null, tint = c.onPrimary, modifier = Modifier.size(26.dp)); Spacer(Modifier.width(TTSpace.m)) }
        Text(text, style = TTType.heading, color = if (enabled) c.onPrimary else c.inkFaint, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** A quiet action beside a primary one. */
@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color? = null,
    enabled: Boolean = true,
    minHeight: Dp = TTSpace.touch,
    onClick: () -> Unit
) {
    val c = TT.colors
    val ink = tint ?: c.primaryDeep
    Row(
        modifier
            .heightIn(min = minHeight)
            .clip(TTShape.medium)
            .background(c.surface)
            .border(if (c.highContrast) 2.dp else 1.dp, if (c.highContrast) c.ink else c.line, TTShape.medium)
            .accessibleClickable(label = text, enabled = enabled, shape = TTShape.medium, onClick = onClick)
            .padding(horizontal = TTSpace.l, vertical = TTSpace.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) { Icon(icon, null, tint = if (enabled) ink else c.inkFaint, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(TTSpace.s)) }
        Text(text, style = TTType.bodyStrong, color = if (enabled) ink else c.inkFaint, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** A tappable text control that still measures 48dp - for sheet headers and inline links. */
@Composable
fun TextAction(text: String, modifier: Modifier = Modifier, tint: Color? = null, enabled: Boolean = true, strong: Boolean = false, onClick: () -> Unit) {
    val c = TT.colors
    Box(
        modifier
            .defaultMinSize(minWidth = TTSpace.touch, minHeight = TTSpace.touch)
            .clip(TTShape.small)
            .accessibleClickable(label = text, enabled = enabled, onClick = onClick)
            .padding(horizontal = TTSpace.m),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = if (strong) TTType.bodyStrong else TTType.body, color = if (!enabled) c.inkFaint else tint ?: c.primary, maxLines = 1)
    }
}

/** An icon-only control that still measures 48dp - list row actions. */
@Composable
fun IconAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, tint: Color? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val c = TT.colors
    Box(
        modifier
            .size(TTSpace.touch)
            .clip(CircleShape)
            .accessibleClickable(label = label, enabled = enabled, shape = CircleShape, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = if (enabled) tint ?: c.inkSoft else c.inkFaint, modifier = Modifier.size(24.dp))
    }
}

/** A small filled note - a count, a state - never smaller than caption size. */
@Composable
fun Badge(text: String, modifier: Modifier = Modifier, color: Color? = null, onColor: Color? = null) {
    val c = TT.colors
    Text(text, style = TTType.caption.copy(fontWeight = FontWeight.SemiBold), color = onColor ?: c.primaryDeep,
        modifier = modifier.clip(TTShape.pill).background(color ?: c.primarySoft).padding(horizontal = 10.dp, vertical = 4.dp))
}

/** A thin rule between things. */
@Composable
fun Divider(modifier: Modifier = Modifier) {
    val c = TT.colors
    Box(modifier.fillMaxWidth().heightIn(min = 1.dp).size(1.dp).background(c.line))
}

/** Older call sites: the Speak control is now `SentenceBar`; this stays as a bare play button. */
@Composable
fun SpeakNowButton(size: Dp = 52.dp, onClick: () -> Unit) {
    BarButton(icon = Icons.Default.PlayArrow, label = "Speak", filled = true, size = size, onClick = onClick)
}

/** Older call sites: the band is gone; nothing is drawn. */
@Composable
fun RainbowBand() {}

/** The system navigation bar height as the activity sees it, handed down to dialogs. */
val LocalNavBarBottom = compositionLocalOf { 0.dp }

/** Row helper: the label column in forms. */
@Composable
fun RowScope.LabelText(text: String) {
    Text(text, style = TTType.label, color = TT.colors.inkSoft, modifier = Modifier.width(118.dp))
}
