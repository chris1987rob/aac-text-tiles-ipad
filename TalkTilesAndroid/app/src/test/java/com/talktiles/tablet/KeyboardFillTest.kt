package com.talktiles.tablet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A keyboard group's short last row is completed with more words, never left with a gap. */
@RunWith(RobolectricTestRunner::class)
class KeyboardFillTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun fifteenPeopleGetASixteenthPersonNotAGap() {
        TestBook.store(tmp.root)
        val people = SymbolWordBank.groups.first { it.id == "People" }
        val extra = SymbolWordBank.fillers(people, 16 - people.words.size)
        assertEquals(16 - people.words.size, extra.size)
        val words = people.words.map { it.label.lowercase() }.toSet()
        assertTrue(extra.none { it.label.lowercase() in words })
        assertTrue(extra.all { it.icon.startsWith("tt:") && it.color == people.color })
    }

    @Test
    fun everyGroupCanCompleteAnyRow() {
        TestBook.store(tmp.root)
        // The widest grid is 8 across, so a row is never more than 7 short.
        for (g in SymbolWordBank.groups) assertEquals(g.id, 7, SymbolWordBank.fillers(g, 7).size)
    }

    @Test
    fun aFillerKeyCanBeOpenedInTheKeyEditor() {
        TestBook.store(tmp.root)
        val people = SymbolWordBank.groups.first { it.id == "People" }
        val f = SymbolWordBank.fillers(people, 1).first()
        assertEquals(f.label, SymbolWordBank.word(f.id)?.label)
    }
}
