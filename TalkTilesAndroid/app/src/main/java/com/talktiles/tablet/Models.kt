package com.talktiles.tablet

import android.util.Base64
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

// The saved-board format is the iPad app's: what Swift's JSONEncoder writes
// for the same models. That is deliberate - a backup made on the iPad restores
// here and a page shared from here opens there. The three places Swift is
// peculiar are handled by the serializers at the bottom of this file:
// `Data` is a base64 string, a dictionary keyed by Int is a flat array of
// alternating keys and values, and nil is a missing key.

val AppJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    isLenient = true
}

/** For aac_settings.json only: writes `"voiceId": null` rather than dropping the key. */
val SettingsJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = true
    encodeDefaults = true
    isLenient = true
}

@Serializable
enum class PageType(val raw: String, val displayName: String) {
    @SerialName("Standard Grid") GRID("Standard Grid", "Standard Grid"),
    @SerialName("Visual Scene Display") SCENE("Visual Scene Display", "Visual Scene Display"),
    @SerialName("Talking Keyboard") KEYBOARD("Talking Keyboard", "Symbol Keyboard");
}

@Serializable
data class TileModel(
    val id: Int,
    val label: String = "",
    val tts: String = "",
    val symbolName: String? = null,
    @Serializable(with = Base64Serializer::class) val photoData: ByteArray? = null,
    val bgHex: String = "#FFFFFF",
    val borderHex: String = "#CBD5E1",
    val labelHex: String = "#1E293B",
    val labelSize: Double = 1.0,
    @Serializable(with = Base64Serializer::class) val audioData: ByteArray? = null,
    val isSoundItOut: Boolean = false,
    val labelPositionTop: Boolean = false
) {
    /** What the button speaks: its own phrase, else the word on it. */
    val spoken: String get() = if (tts.isEmpty()) label else tts
    val hasPhoto: Boolean get() = photoData != null

    // ByteArray makes the generated equals() compare references; the store
    // relies on value equality to know whether anything changed.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TileModel) return false
        return id == other.id && label == other.label && tts == other.tts &&
            symbolName == other.symbolName && photoData.contentEquals(other.photoData) &&
            bgHex == other.bgHex && borderHex == other.borderHex && labelHex == other.labelHex &&
            labelSize == other.labelSize && audioData.contentEquals(other.audioData) &&
            isSoundItOut == other.isSoundItOut && labelPositionTop == other.labelPositionTop
    }

    override fun hashCode(): Int = id * 31 + label.hashCode()
}

@Serializable
enum class HotspotStyle(val raw: String) {
    @SerialName("Invisible") INVISIBLE("Invisible"),
    @SerialName("Yellow Glow") HIGHLIGHT("Yellow Glow"),
    @SerialName("Dashed Box") OUTLINE("Dashed Box");
}

@Serializable
enum class HotspotAction(val raw: String) {
    @SerialName("Text-to-Speech") TTS("Text-to-Speech"),
    @SerialName("Recorded Voice") RECORDED("Recorded Voice"),
    @SerialName("Jump to Page") JUMP("Jump to Page");
}

@Serializable
data class HotspotModel(
    val id: Int,
    val x: Double = 30.0,   // percent of the scene, 0..100
    val y: Double = 30.0,
    val w: Double = 25.0,
    val h: Double = 25.0,
    val label: String = "Hotspot",
    val tts: String = "Hotspot",
    val style: HotspotStyle = HotspotStyle.INVISIBLE,
    val action: HotspotAction = HotspotAction.TTS,
    @Serializable(with = Base64Serializer::class) val audioData: ByteArray? = null,
    val jumpPageId: String? = null
) {
    val spoken: String get() = if (tts.isEmpty()) label else tts

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HotspotModel) return false
        return id == other.id && x == other.x && y == other.y && w == other.w && h == other.h &&
            label == other.label && tts == other.tts && style == other.style && action == other.action &&
            audioData.contentEquals(other.audioData) && jumpPageId == other.jumpPageId
    }

    override fun hashCode(): Int = id
}

/** One key on one keyboard page, changed. Every field nil means "as the word bank has it". */
@Serializable
data class KeyboardKeyEdit(
    val label: String? = null,
    val tts: String? = null,
    val icon: String? = null,
    val colorHex: String? = null,
    @Serializable(with = Base64Serializer::class) val photoData: ByteArray? = null,
    @Serializable(with = Base64Serializer::class) val audioData: ByteArray? = null,
    val hidden: Boolean? = null
) {
    val isEmpty: Boolean
        get() = label == null && tts == null && icon == null && colorHex == null &&
            photoData == null && audioData == null && (hidden == null || hidden == false)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KeyboardKeyEdit) return false
        return label == other.label && tts == other.tts && icon == other.icon && colorHex == other.colorHex &&
            photoData.contentEquals(other.photoData) && audioData.contentEquals(other.audioData) && hidden == other.hidden
    }

    override fun hashCode(): Int = (label ?: "").hashCode()
}

@Serializable
data class PageModel(
    val id: String = UUID.randomUUID().toString().uppercase(),
    val title: String = "New Page",
    val type: PageType = PageType.GRID,
    val gridSize: Int = 4,
    val bgHex: String = "#FFFFFF",
    val enabled: Boolean = true,
    val express: Boolean = false,
    @Serializable(with = IntKeyedTilesSerializer::class) val tiles: Map<Int, TileModel> = emptyMap(),
    val hotspots: List<HotspotModel> = emptyList(),
    val scenePresetKey: String? = null,
    @Serializable(with = Base64Serializer::class) val sceneImageData: ByteArray? = null,
    val keyboardKeys: Int? = null,
    val keyboardGroups: List<String>? = null,
    val keyboardEdits: Map<String, KeyboardKeyEdit>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PageModel) return false
        return id == other.id && title == other.title && type == other.type && gridSize == other.gridSize &&
            bgHex == other.bgHex && enabled == other.enabled && express == other.express && tiles == other.tiles &&
            hotspots == other.hotspots && scenePresetKey == other.scenePresetKey &&
            sceneImageData.contentEquals(other.sceneImageData) && keyboardKeys == other.keyboardKeys &&
            keyboardGroups == other.keyboardGroups && keyboardEdits == other.keyboardEdits
    }

    override fun hashCode(): Int = id.hashCode()
}

/** Which bundled picture set the symbol picker offers. See the iPad's SymbolSet. */
@Serializable
enum class SymbolSet(val raw: String, val title: String, val blurb: String) {
    @SerialName("talktiles") TALK_TILES("talktiles", "Talk Tiles pictures",
        "Our own pictures. Every one has Bella's voice behind it."),
    @SerialName("mulberry") MULBERRY("mulberry", "Mulberry Symbols",
        "Mulberry Symbols, a licensed third-party set. Spoken by the device voice.");
}

/** Settings that belong to the app rather than to one page. */
@Serializable
data class AppSettings(
    /** A system TTS voice name, `SpokenText.BELLA_VOICE_ID`, or null for the device default. */
    val voiceId: String? = SpokenText.BELLA_VOICE_ID,
    val speechRate: Double = 0.45,
    val symbolSet: SymbolSet = SymbolSet.TALK_TILES,
    val childLock: Boolean = false,
    /** Deliberately plain text: this stops a child wandering into the editor, nothing more. */
    val lockPIN: String = "1234",
    val activationDelay: Double = 0.0,
    val repeatLockout: Double = 0.0,
    val activateOnRelease: Boolean = false
)

/** A button kept aside so it can be put on another page without building it again. */
@Serializable
data class SavedTile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val label: String,
    val tts: String,
    val symbolName: String? = null,
    @Serializable(with = Base64Serializer::class) val photoData: ByteArray? = null,
    @Serializable(with = Base64Serializer::class) val audioData: ByteArray? = null,
    val bgHex: String = "#FFFFFF",
    val borderHex: String = "#CBD5E1",
    val labelHex: String = "#1E293B",
    val labelSize: Double = 1.0,
    val isSoundItOut: Boolean = false,
    val labelPositionTop: Boolean = false,
    val savedAt: Long = System.currentTimeMillis()
) {
    val hasPhoto: Boolean get() = photoData != null
    val hasRecording: Boolean get() = audioData != null

    fun tile(inSlot: Int) = TileModel(
        id = inSlot, label = label, tts = tts, symbolName = symbolName, photoData = photoData,
        bgHex = bgHex, borderHex = borderHex, labelHex = labelHex, labelSize = labelSize,
        audioData = audioData, isSoundItOut = isSoundItOut, labelPositionTop = labelPositionTop
    )

    override fun equals(other: Any?): Boolean = other is SavedTile && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

// MARK: - Serializers

/** Swift `Data` <-> base64 text. */
object Base64Serializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Base64Data", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ByteArray) =
        encoder.encodeString(Base64.encodeToString(value, Base64.NO_WRAP))
    override fun deserialize(decoder: Decoder): ByteArray =
        Base64.decode(decoder.decodeString(), Base64.DEFAULT)
}

/**
 * Swift encodes `[Int: TileModel]` as `[1, {...}, 2, {...}]` - a flat array of
 * alternating keys and values, not an object. Written that way so the iPad can
 * read it; read either way so a hand-edited or older file still loads.
 */
object IntKeyedTilesSerializer : KSerializer<Map<Int, TileModel>> {
    private val element = JsonElement.serializer()
    override val descriptor: SerialDescriptor = element.descriptor

    override fun serialize(encoder: Encoder, value: Map<Int, TileModel>) {
        val json = (encoder as JsonEncoder).json
        val array = buildJsonArray {
            for ((slot, tile) in value.toSortedMap()) {
                add(JsonPrimitive(slot))
                add(json.encodeToJsonElement(TileModel.serializer(), tile))
            }
        }
        encoder.encodeJsonElement(array)
    }

    override fun deserialize(decoder: Decoder): Map<Int, TileModel> {
        val jd = decoder as JsonDecoder
        val out = LinkedHashMap<Int, TileModel>()
        when (val el = jd.decodeJsonElement()) {
            is JsonArray -> {
                var i = 0
                while (i + 1 < el.size) {
                    val key = el[i].jsonPrimitive.intOrNull
                    val tile = jd.json.decodeFromJsonElement(TileModel.serializer(), el[i + 1])
                    if (key != null) out[key] = tile
                    i += 2
                }
            }
            is JsonObject -> for ((k, v) in el) {
                val key = k.toIntOrNull() ?: continue
                out[key] = jd.json.decodeFromJsonElement(TileModel.serializer(), v)
            }
            else -> {}
        }
        return out
    }
}

/** Kept for symmetry with the iPad's BookBackup.Archive. */
@Serializable
data class BookArchive(
    val format: String = "talktiles.book",
    val version: Int = 1,
    val createdAt: String,
    val pages: List<PageModel>,
    val settings: AppSettings? = null
)

val pageListSerializer = ListSerializer(PageModel.serializer())
