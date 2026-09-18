package com.talktiles.tablet

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

/** The height of the system navigation bar, as the activity sees it. */
val LocalNavBarBottom = androidx.compose.runtime.compositionLocalOf { 0.dp }

// The iPad's Form/Section/NavigationView, rebuilt: a full-screen sheet with a
// title bar (Cancel on the left, the action on the right) over grouped
// sections on the pale ground. Every modal in the app is one of these.

/** A full-screen sheet. Back closes it the way Cancel does. */
@Composable
fun ModalSheet(
    title: String,
    onDismiss: () -> Unit,
    leading: String? = "Cancel",
    trailing: String? = null,
    onTrailing: (() -> Unit)? = null,
    trailingEnabled: Boolean = true,
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    // A dialog window on this tablet is not told about the navigation bar, so
    // the bar was drawn over the bottom of every sheet. The activity IS told,
    // so its inset is read there and handed down through LocalNavBarBottom.
    val navBottom = LocalNavBarBottom.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        BackHandler { onDismiss() }
        Column(Modifier.fillMaxSize().background(BoardTheme.background).padding(bottom = navBottom).imePadding()) {
            Box(Modifier.fillMaxWidth().background(Color.White).height(56.dp).padding(horizontal = 12.dp)) {
                if (leading != null) {
                    Text(leading, color = BoardTheme.blue, fontSize = 17.sp, modifier = Modifier.align(Alignment.CenterStart).plainClickable(onClick = onDismiss).padding(8.dp))
                }
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BoardTheme.ink,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 80.dp), maxLines = 1)
                if (trailing != null && onTrailing != null) {
                    Text(trailing, color = if (trailingEnabled) BoardTheme.green else BoardTheme.line, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterEnd).plainClickable(enabled = trailingEnabled, onClick = onTrailing).padding(8.dp))
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(BoardTheme.line.copy(alpha = 0.6f)))
            val base = Modifier.fillMaxSize().padding(horizontal = 16.dp)
            Column(if (scroll) base.verticalScroll(rememberScrollState()) else base) {
                Spacer(Modifier.height(12.dp))
                content()
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/** A grouped section: small caps header, white card of rows, footnote under it. */
@Composable
fun FormSection(header: String? = null, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        if (header != null) {
            Text(header.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BoardTheme.slate,
                letterSpacing = 0.5.sp, modifier = Modifier.padding(start = 16.dp, bottom = 6.dp))
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White)) {
            content()
        }
        if (footer != null) {
            Text(footer, fontSize = 13.sp, color = BoardTheme.slate, modifier = Modifier.padding(start = 16.dp, top = 6.dp, end = 16.dp), lineHeight = 17.sp)
        }
    }
}

/** One row inside a section. */
@Composable
fun FormRow(onClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.plainClickable(onClick = onClick) else Modifier)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
    Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(0.5.dp).background(BoardTheme.line.copy(alpha = 0.6f)))
}

/** A tappable row of text, coloured like an iOS button row. */
@Composable
fun FormButton(title: String, tint: Color = BoardTheme.blue, icon: ImageVector? = null, enabled: Boolean = true, onClick: () -> Unit) {
    FormRow(onClick = if (enabled) onClick else null) {
        if (icon != null) {
            Icon(icon, null, tint = if (enabled) tint else BoardTheme.line, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(title, color = if (enabled) tint else BoardTheme.line, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** A text field with a caption that stays put. */
@Composable
fun LabeledField(label: String, placeholder: String, value: String, onChange: (String) -> Unit) {
    FormRow {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BoardTheme.slate, modifier = Modifier.width(118.dp))
        PlainTextField(value, onChange, placeholder, Modifier.weight(1f))
    }
}

@Composable
fun PlainTextField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier,
                   keyboardType: KeyboardType = KeyboardType.Text, fontSize: Int = 16) {
    TextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, color = BoardTheme.line, fontSize = fontSize.sp) },
        singleLine = true,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = fontSize.sp, color = BoardTheme.ink),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            cursorColor = BoardTheme.green
        )
    )
}

@Composable
fun ToggleRow(title: String, checked: Boolean, leading: (@Composable () -> Unit)? = null, onChange: (Boolean) -> Unit) {
    FormRow(onClick = { onChange(!checked) }) {
        if (leading != null) { leading(); Spacer(Modifier.width(10.dp)) }
        Text(title, fontSize = 16.sp, color = BoardTheme.ink, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = BoardTheme.green))
    }
}

@Composable
fun SliderRow(title: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(title, fontSize = 16.sp, color = BoardTheme.ink, modifier = Modifier.weight(1f))
            Text(valueLabel, fontSize = 15.sp, color = BoardTheme.slate)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps,
            colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = BoardTheme.green, activeTrackColor = BoardTheme.green))
    }
    Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(0.5.dp).background(BoardTheme.line.copy(alpha = 0.6f)))
}

/** The iOS segmented picker: one pill per option, the chosen one white on grey. */
@Composable
fun <T> SegmentedPicker(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(hexColor("#EEF0F4"))
            .padding(2.dp)
    ) {
        for (o in options) {
            val on = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (on) Color.White else Color.Transparent)
                    .plainClickable { onSelect(o) },
                contentAlignment = Alignment.Center
            ) {
                Text(label(o), fontSize = 14.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                    color = BoardTheme.ink, maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}

/** A small round chip used for categories and filters. */
@Composable
fun FilterChip(title: String, selected: Boolean, color: Color = BoardTheme.green, onClick: () -> Unit) {
    Text(
        title,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) Color.White else hexColor("#475569"),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) color else hexColor("#F1F5F9"))
            .plainClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

// MARK: - Colours

/** The one colour palette in the app. */
object AppPalette {
    val swatches: List<Pair<String, String>> = listOf(
        "#FFFFFF" to "White", "#F4F5F8" to "Paper", "#B0B7BD" to "Silver", "#6C757D" to "Grey",
        "#111111" to "Black", "#FFF9C4" to "Yellow", "#C8E6C9" to "Green", "#BBDEFB" to "Blue",
        "#FFE0B2" to "Orange", "#F8BBD0" to "Pink", "#D1C4E9" to "Purple", "#FFCDD2" to "Red",
        "#B8E8DB" to "Mint", "#004838" to "Forest", "#00A699" to "Teal", "#F4A261" to "Apricot",
        "#EA695B" to "Coral", "#C4312A" to "Brick", "#1A237E" to "Navy", "#795548" to "Cocoa"
    )

    fun name(hex: String): String = swatches.firstOrNull { it.first.equals(hex, ignoreCase = true) }?.second ?: hex.uppercase()
}

/** The row that opens the colour chooser, with a swatch big enough to read. */
@Composable
fun ColorRow(title: String, hex: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    FormRow(onClick = { open = true }) {
        Text(title, fontSize = 16.sp, color = BoardTheme.ink, modifier = Modifier.weight(1f))
        Text(AppPalette.name(hex), fontSize = 14.sp, color = BoardTheme.slate)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.size(48.dp, 26.dp).clip(RoundedCornerShape(6.dp)).background(hexColor(hex)).border(1.dp, hexColor("#94A3B8"), RoundedCornerShape(6.dp)))
    }
    if (open) {
        ColorChoiceSheet(title, hex, onChange) { open = false }
    }
}

/** Swatches and a custom picker, for every colour in the app. */
@Composable
fun ColorChoiceSheet(title: String, hex: String, onChange: (String) -> Unit, onDismiss: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    ModalSheet(title = title, onDismiss = onDismiss, leading = "Done", scroll = false) {
        SegmentedPicker(listOf(0, 1), tab, { if (it == 0) "Swatches" else "Picker" }, { tab = it })
        Spacer(Modifier.height(16.dp))
        if (tab == 0) {
            LazyVerticalGrid(columns = GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(AppPalette.swatches) { (swatch, name) ->
                    val on = swatch.equals(hex, ignoreCase = true)
                    Column(Modifier.plainClickable { onChange(swatch) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(10.dp)).background(hexColor(swatch))
                            .border(if (on) 4.dp else 1.dp, if (on) BoardTheme.green else BoardTheme.line, RoundedCornerShape(10.dp)))
                        Spacer(Modifier.height(6.dp))
                        Text(name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BoardTheme.slate)
                    }
                }
            }
        } else {
            CustomColorPicker(hex, onChange)
        }
    }
}

/** Three sliders and a live swatch - the custom picker the iPad's ColorPicker offered. */
@Composable
fun CustomColorPicker(hex: String, onChange: (String) -> Unit) {
    val c = hexColor(hex)
    var r by remember(hex) { mutableStateOf(c.red) }
    var g by remember(hex) { mutableStateOf(c.green) }
    var b by remember(hex) { mutableStateOf(c.blue) }
    fun push() = onChange(Color(r, g, b).toHexString())
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Pick any colour", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = BoardTheme.ink)
        for ((name, v, set) in listOf(
            Triple("Red", r, { x: Float -> r = x }),
            Triple("Green", g, { x: Float -> g = x }),
            Triple("Blue", b, { x: Float -> b = x }))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, modifier = Modifier.width(56.dp), color = BoardTheme.slate, fontSize = 14.sp)
                Slider(value = v, onValueChange = { set(it); push() }, modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = BoardTheme.green, activeTrackColor = BoardTheme.green))
            }
        }
        Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).background(Color(r, g, b)).border(1.dp, BoardTheme.line, RoundedCornerShape(12.dp)))
        Text(Color(r, g, b).toHexString(), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = BoardTheme.slate, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

// MARK: - Pictures

/** Reads a picked photo, downscales it, and hands back JPEG bytes. */
fun loadPickedImage(context: Context, uri: Uri, maxDimension: Int): ByteArray? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDimension) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val longest = max(bmp.width, bmp.height)
        val scaled = if (longest > maxDimension) {
            val s = maxDimension.toFloat() / longest
            Bitmap.createScaledBitmap(bmp, (bmp.width * s).toInt().coerceAtLeast(1), (bmp.height * s).toInt().coerceAtLeast(1), true)
        } else bmp
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        out.toByteArray()
    } catch (e: Exception) { null }
}

private fun max(a: Int, b: Int) = if (a > b) a else b

/**
 * The two picture-source rows (camera, library) plus Remove. Owns the
 * launchers, so a caller just says what to do with the bytes.
 */
@Composable
fun PictureSourceRows(hasPicture: Boolean, maxDimension: Int, onPicked: (ByteArray) -> Unit, onRemove: () -> Unit) {
    val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    val pickLibrary = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) loadPickedImage(context, uri, maxDimension)?.let(onPicked)
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val u = cameraUri
        if (ok && u != null) loadPickedImage(context, u, maxDimension)?.let(onPicked)
    }
    fun launchCamera() {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val f = File(dir, "camera-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "com.talktiles.tablet.files", f)
        cameraUri = uri
        takePicture.launch(uri)
    }
    val askCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else pickLibrary.launch("image/*")
    }

    FormButton(
        title = if (hasPicture) "Retake Photo" else "Take Photo with Camera",
        tint = BoardTheme.blue, icon = Icons.Default.CameraAlt
    ) {
        if (!hasCamera) { pickLibrary.launch("image/*"); return@FormButton }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCamera()
        else askCamera.launch(Manifest.permission.CAMERA)
    }
    FormButton(
        title = if (hasPicture) "Choose a Different Picture" else "Choose from Photo Library",
        tint = BoardTheme.green, icon = Icons.Default.PhotoLibrary
    ) { pickLibrary.launch("image/*") }
    if (hasPicture) {
        FormButton("Remove Picture", tint = BoardTheme.danger, icon = Icons.Default.Delete, onClick = onRemove)
    }
}

/** Asks for the microphone once, then records. */
@Composable
fun rememberRecordPermission(onGranted: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) onGranted() }
    return {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) onGranted()
        else ask.launch(Manifest.permission.RECORD_AUDIO)
    }
}

/** A photo, a library picture, an emoji, or the first letters - the thumbnail every list uses. */
@Composable
fun TileThumbnail(photoData: ByteArray?, symbolName: String?, label: String, bgHex: String, borderHex: String, labelHex: String, size: Int = 56, key: String) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(10.dp)).background(hexColor(bgHex)).border(2.dp, hexColor(borderHex), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        val photo = PhotoCache.bitmap(photoData, key)
        when {
            photo != null -> androidx.compose.foundation.Image(photo.asImageBitmapSafe(), null, Modifier.fillMaxSize().padding(2.dp).clip(RoundedCornerShape(8.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            symbolName != null -> SymbolPicture(symbolName, (size * 0.5f).dp)
            else -> Text(label.take(2), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = hexColor(labelHex))
        }
    }
}

fun Bitmap.asImageBitmapSafe() = this.asImageBitmap()
