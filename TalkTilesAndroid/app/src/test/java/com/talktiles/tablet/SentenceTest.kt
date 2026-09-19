package com.talktiles.tablet

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The sentence bar keeps what each button *says*, in the order it was pressed. */
class SentenceTest {

    private val eat = TileModel(id = 1, label = "Eat", tts = "Eat food", symbolName = "tt:eat")
    private val rec = TileModel(id = 2, label = "Mum", tts = "", audioData = byteArrayOf(9, 9))
    private val want = TileModel(id = 3, label = "I want", tts = "I want")

    @Test
    fun itemsCarryLabelSpokenRecordingAndPicture() {
        val item = SentenceItem.from(eat)
        assertEquals("Eat", item.label)
        assertEquals("Eat food", item.spoken)
        assertEquals("tt:eat", item.symbolName)
        assertNull(item.audioData)

        val recorded = SentenceItem.from(rec)
        assertEquals("Mum", recorded.spoken)          // no phrase: the label is what it says
        assertArrayEquals(byteArrayOf(9, 9), recorded.audioData)
    }

    @Test
    fun orderIsTheOrderOfPressing() {
        val s = SentenceBuilder()
        s.add(SentenceItem.from(want)); s.add(SentenceItem.from(eat)); s.add(SentenceItem.from(rec))
        assertEquals(listOf("I want", "Eat", "Mum"), s.items.map { it.label })
        assertEquals("I want Eat food Mum", s.spokenText)
    }

    @Test
    fun removeLastTakesOnlyTheLastAndIsSafeWhenEmpty() {
        val s = SentenceBuilder()
        s.removeLast()
        assertTrue(s.items.isEmpty())
        s.add(SentenceItem.from(want)); s.add(SentenceItem.from(eat))
        s.removeLast()
        assertEquals(listOf("I want"), s.items.map { it.label })
    }

    @Test
    fun clearCanBeUndoneOnceAndRestoresTheExactList() {
        val s = SentenceBuilder()
        s.add(SentenceItem.from(want)); s.add(SentenceItem.from(eat))
        assertFalse(s.canUndoClear)
        s.clear()
        assertTrue(s.items.isEmpty())
        assertTrue(s.canUndoClear)
        assertTrue(s.undoClear())
        assertEquals(listOf("I want", "Eat"), s.items.map { it.label })
        assertFalse(s.canUndoClear)
        assertFalse(s.undoClear())
    }

    @Test
    fun clearingAnEmptySentenceLeavesNothingToUndo() {
        val s = SentenceBuilder()
        s.clear()
        assertFalse(s.canUndoClear)
    }

    @Test
    fun addingAfterAClearForgetsTheUndo() {
        val s = SentenceBuilder()
        s.add(SentenceItem.from(want))
        s.clear()
        s.add(SentenceItem.from(eat))
        assertFalse(s.canUndoClear)
        assertEquals(listOf("Eat"), s.items.map { it.label })
    }

    @Test
    fun replaceIsUndoableToo() {
        val s = SentenceBuilder()
        s.add(SentenceItem.from(want))
        s.replaceWith(listOf(SentenceItem.from(eat), SentenceItem.from(rec)))
        assertEquals(listOf("Eat", "Mum"), s.items.map { it.label })
        assertTrue(s.undoClear())
        assertEquals(listOf("I want"), s.items.map { it.label })
    }

    @Test
    fun savedPhrasesRoundTripThroughJsonWithRecordings() {
        val phrase = SavedPhrase(id = "p1", name = "Snack", items = listOf(SentenceItem.from(want), SentenceItem.from(rec)))
        val json = AppJson.encodeToString(SavedPhrase.serializer(), phrase)
        val back = AppJson.decodeFromString(SavedPhrase.serializer(), json)
        assertEquals("Snack", back.name)
        assertEquals(listOf("I want", "Mum"), back.items.map { it.label })
        assertArrayEquals(byteArrayOf(9, 9), back.items[1].audioData)
        assertEquals(phrase, back)
    }
}
