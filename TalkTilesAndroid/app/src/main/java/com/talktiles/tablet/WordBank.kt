package com.talktiles.tablet

/** One pressable word on the symbol keyboard. */
data class SymbolWord(
    val id: String,
    val label: String,
    val tts: String,
    val icon: String,
    val color: String,
    val photoData: ByteArray? = null,
    val audioData: ByteArray? = null
) {
    /** This word as one page has changed it; null fields leave the bank's value alone. */
    fun applying(edit: KeyboardKeyEdit?): SymbolWord {
        if (edit == null) return this
        return copy(
            label = edit.label?.takeIf { it.isNotEmpty() } ?: label,
            tts = edit.tts?.takeIf { it.isNotEmpty() } ?: tts,
            icon = edit.icon?.takeIf { it.isNotEmpty() } ?: icon,
            color = edit.colorHex?.takeIf { it.isNotEmpty() } ?: color,
            photoData = edit.photoData,
            audioData = edit.audioData
        )
    }

    override fun equals(other: Any?) = other is SymbolWord && other.id == id && other.label == label &&
        other.tts == tts && other.icon == icon && other.color == color
    override fun hashCode() = id.hashCode()
}

/**
 * The symbol keyboard's vocabulary, derived from the template boards. The
 * groups are the Fitzgerald key: the colour a word carries is its part of speech.
 */
object SymbolWordBank {

    data class Group(val id: String, val title: String, val color: String, val words: List<SymbolWord>)

    private val groupOrder = listOf(
        TemplateColor.person to "People",
        TemplateColor.verb to "Actions",
        TemplateColor.descriptor to "Describing",
        TemplateColor.noun to "Things",
        TemplateColor.social to "Social",
        TemplateColor.question to "Questions",
        TemplateColor.negative to "No & Stop"
    )

    val groups: List<Group> by lazy {
        val seen = HashSet<String>()
        val byColor = HashMap<String, ArrayList<SymbolWord>>()
        for (template in PageTemplateCatalog.all) {
            for (tile in template.tiles) {
                val icon = tile.symbol ?: continue
                if (icon.isEmpty()) continue
                val key = tile.label.lowercase()
                if (!seen.add(key)) continue
                byColor.getOrPut(tile.color) { ArrayList() }
                    .add(SymbolWord(key, tile.label, tile.tts, icon, tile.color))
            }
        }
        groupOrder.mapNotNull { (color, title) ->
            val words = byColor[color]
            if (words.isNullOrEmpty()) null else Group(title, title, color, words)
        }
    }

    val totalWords: Int get() = groups.sumOf { it.words.size }

    fun groups(limitedTo: List<String>?): List<Group> {
        if (limitedTo.isNullOrEmpty()) return groups
        val wanted = limitedTo.toSet()
        val kept = groups.filter { it.id in wanted }
        return kept.ifEmpty { groups }
    }

    /** The same sizes a Standard Grid page offers, deliberately. */
    val keyCountOptions = listOf(1, 2, 4, 9, 12, 16, 25, 36, 48)
    const val defaultKeyCount = 16

    fun keyCountLabel(count: Int): String = when {
        count <= 2 -> "Huge"
        count == 4 -> "Very big"
        count == 9 || count == 12 -> "Big"
        count == 16 -> "Medium"
        count == 25 -> "Small"
        count == 36 -> "Very small"
        else -> "Tiny"
    }

    fun nearestKeyCount(count: Int?): Int {
        if (count == null) return defaultKeyCount
        return keyCountOptions.minByOrNull { kotlin.math.abs(it - count) } ?: defaultKeyCount
    }

    fun word(id: String): SymbolWord? = groups.asSequence().flatMap { it.words.asSequence() }.firstOrNull { it.id == id }
}
