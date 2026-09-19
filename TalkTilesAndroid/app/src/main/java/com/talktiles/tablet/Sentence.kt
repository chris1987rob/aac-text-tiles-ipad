package com.talktiles.tablet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * One thing the person put in the sentence bar: what is shown, what it says,
 * and - if the button carries one - the recording that IS what it says.
 */
@Serializable
data class SentenceItem(
    val label: String,
    val spoken: String,
    val symbolName: String? = null,
    @Serializable(with = Base64Serializer::class) val photoData: ByteArray? = null,
    @Serializable(with = Base64Serializer::class) val audioData: ByteArray? = null
) {
    val hasRecording: Boolean get() = audioData != null

    override fun equals(other: Any?): Boolean = other is SentenceItem && other.label == label && other.spoken == spoken &&
        other.symbolName == symbolName && photoData.contentEquals(other.photoData) && audioData.contentEquals(other.audioData)
    override fun hashCode(): Int = label.hashCode() * 31 + spoken.hashCode()

    companion object {
        fun from(tile: TileModel) = SentenceItem(
            label = tile.label.ifEmpty { tile.tts }, spoken = tile.spoken,
            symbolName = tile.symbolName, photoData = tile.photoData, audioData = tile.audioData
        )
        fun from(spot: HotspotModel) = SentenceItem(
            label = spot.label.ifEmpty { spot.tts }, spoken = spot.spoken,
            audioData = if (spot.action == HotspotAction.RECORDED) spot.audioData else null
        )
        fun from(word: SymbolWord) = SentenceItem(
            label = word.label, spoken = word.tts.ifEmpty { word.label },
            symbolName = word.icon, photoData = word.photoData, audioData = word.audioData
        )
    }
}

/**
 * The sentence being built. Lives in the store, not in a page's composable,
 * so turning the page does not lose it, and the grid and the keyboard share
 * one bar with one set of controls.
 */
class SentenceBuilder {
    private val list = mutableStateListOf<SentenceItem>()
    val items: List<SentenceItem> get() = list

    /** What was in the bar before the last clear, until something new is added. */
    private var cleared by mutableStateOf<List<SentenceItem>?>(null)
    val canUndoClear: Boolean get() = cleared != null

    val isEmpty: Boolean get() = list.isEmpty()
    val spokenText: String get() = list.joinToString(" ") { it.spoken }

    fun add(item: SentenceItem) { cleared = null; list.add(item) }

    fun removeLast() { if (list.isNotEmpty()) list.removeAt(list.size - 1) }

    /** Takes one word out - the word the person tapped in the bar. */
    fun removeAt(index: Int) { if (index in list.indices) list.removeAt(index) }

    /** Empties the bar, keeping what was there for one undo. Nothing is confirmed per word. */
    fun clear() {
        if (list.isEmpty()) return
        cleared = list.toList()
        list.clear()
    }

    fun undoClear(): Boolean {
        val back = cleared ?: return false
        cleared = null
        list.clear(); list.addAll(back)
        return true
    }

    /** Swaps in a saved phrase; undo brings the previous sentence back. */
    fun replaceWith(items: List<SentenceItem>) {
        if (list.isNotEmpty()) cleared = list.toList()
        list.clear(); list.addAll(items)
    }
}

/** A sentence kept for later, spoken exactly as it was built. */
@Serializable
data class SavedPhrase(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val items: List<SentenceItem>,
    val savedAt: Long = System.currentTimeMillis()
) {
    val spokenText: String get() = items.joinToString(" ") { it.spoken }
}
