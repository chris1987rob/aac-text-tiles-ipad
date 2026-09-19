package com.talktiles.tablet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Finding a word anywhere in the book, so the reader can be taken to it - without it being spoken. */
class VocabularySearchTest {

    private val grid = PageModel(id = "G", title = "Food", tiles = mapOf(
        1 to TileModel(id = 1, label = "Apple", tts = "I want an apple", symbolName = "tt:apple"),
        2 to TileModel(id = 2, label = "Drink", tts = "Drink water"),
        3 to TileModel(id = 3, label = "", tts = "")
    ))
    private val scene = PageModel(id = "S", title = "Living room", type = PageType.SCENE, hotspots = listOf(
        HotspotModel(id = 1, label = "Sofa", tts = "Sit on the sofa"),
        HotspotModel(id = 2, label = "Go to food", tts = "", action = HotspotAction.JUMP, jumpPageId = "G")
    ))
    private val off = PageModel(id = "O", title = "Hidden", enabled = false, tiles = mapOf(1 to TileModel(id = 1, label = "Apple pie")))
    private val keyboard = PageModel(id = "K", title = "Keyboard", type = PageType.KEYBOARD,
        keyboardEdits = mapOf("stop" to KeyboardKeyEdit(hidden = true), "go" to KeyboardKeyEdit(label = "Go now")))
    private val book = listOf(grid, scene, off, keyboard)

    @Test
    fun findsTilesByLabelOrSpokenPhraseWithLabelMatchesFirst() {
        // The keyboard page also knows "apple"; the grid button comes first because it matches on its label.
        val hits = VocabularySearch.search(book, "apple", editing = false)
        assertEquals(listOf("Apple"), hits.filter { it.kind == VocabularySearch.Kind.TILE }.map { it.label })
        assertTrue(hits.any { it.kind == VocabularySearch.Kind.KEY && it.pageId == "K" })
        assertEquals("G", hits[0].pageId)
        assertEquals(0, hits[0].pageIndex)
        assertEquals("I want an apple", hits[0].spoken)
        assertEquals(VocabularySearch.Kind.TILE, hits[0].kind)

        // "Drink" is found through what it says; the keyboard's own "Water" key ranks ahead because it matches on its label.
        val water = VocabularySearch.search(book, "water", editing = false)
        assertEquals(listOf("Water", "Drink"), water.map { it.label })
        assertEquals(VocabularySearch.Kind.TILE, water[1].kind)
    }

    @Test
    fun findsHotspotsAndReportsWhereTheyAre() {
        val hits = VocabularySearch.search(book, "sofa", editing = false)
        assertEquals(1, hits.size)
        assertEquals(VocabularySearch.Kind.HOTSPOT, hits[0].kind)
        assertEquals("Living room", hits[0].pageTitle)
    }

    @Test
    fun disabledPagesAreSearchedOnlyInTheEditor() {
        fun tilePages(editing: Boolean) = VocabularySearch.search(book, "apple", editing).filter { it.kind == VocabularySearch.Kind.TILE }.map { it.pageId }
        assertEquals(listOf("G"), tilePages(editing = false))
        assertEquals(listOf("G", "O"), tilePages(editing = true))
    }

    @Test
    fun keyboardWordsAreFoundWithTheirPageEditsApplied() {
        val hits = VocabularySearch.search(book, "go now", editing = false)
        assertTrue(hits.any { it.kind == VocabularySearch.Kind.KEY && it.label == "Go now" && it.pageId == "K" })
        // A key hidden on that page is not offered to the reader.
        assertTrue(VocabularySearch.search(book, "stop", editing = false).none { it.pageId == "K" })
    }

    @Test
    fun blankQueryAndBlankTilesGiveNothing() {
        assertTrue(VocabularySearch.search(book, "   ", editing = false).isEmpty())
        assertTrue(VocabularySearch.search(book, "zzz", editing = true).isEmpty())
    }

    @Test
    fun matchingIgnoresCaseAndPunctuation() {
        assertEquals(1, VocabularySearch.search(book, "  APPLE! ", editing = false).count { it.kind == VocabularySearch.Kind.TILE })
        assertEquals(1, VocabularySearch.search(listOf(PageModel(id = "P", tiles = mapOf(1 to TileModel(id = 1, label = "I don’t know")))), "don't", editing = false).size)
    }

    @Test
    fun pageTitlesMatchTooSoThePageListIsOneSearch() {
        val hits = VocabularySearch.pages(book, "liv", editing = false)
        assertEquals(listOf("S"), hits.map { it.id })
        assertEquals(listOf("G", "S", "K"), VocabularySearch.pages(book, "", editing = false).map { it.id })
        assertEquals(4, VocabularySearch.pages(book, "", editing = true).size)
    }
}
