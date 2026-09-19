package com.talktiles.tablet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Backups carry the whole book - including saved buttons and phrases - and are checked before anything is replaced. */
class BookBackupTest {

    private val pages = listOf(PageModel(id = "A", title = "A", tiles = mapOf(1 to TileModel(id = 1, label = "Hi"))))
    private val favs = listOf(SavedTile(id = "f1", name = "Mum", label = "Mum", tts = "Mum"))
    private val phrases = listOf(SavedPhrase(id = "p1", name = "Snack", items = listOf(SentenceItem("Eat", "Eat food"))))

    @Test
    fun archiveCarriesSavedButtonsAndPhrasesAndReadsThemBack() {
        val text = BookBackup.encode(pages, AppSettings(), favs, phrases, createdAt = "2026-09-18T20:00:00Z")
        assertTrue(text.contains("\"savedTiles\""))
        assertTrue(text.contains("\"phrases\""))
        val (archive, summary) = BookBackup.read(text)
        assertEquals("talktiles.book", archive.format)
        assertEquals(1, archive.pages.size)
        assertEquals("f1", archive.savedTiles?.single()?.id)
        assertEquals("p1", archive.phrases?.single()?.id)
        assertEquals(1, summary.savedButtons)
        assertEquals(1, summary.phrases)
        assertEquals("2026-09-18T20:00:00Z", summary.createdAt)
    }

    @Test
    fun aV12OrIpadArchiveWithoutThoseFieldsStillReadsAndLeavesThemNull() {
        val text = """{"format":"talktiles.book","version":1,"createdAt":"2026-01-01T00:00:00Z","pages":[{"id":"A","title":"A"}],"settings":{"voiceId":null}}"""
        val (archive, summary) = BookBackup.read(text)
        assertNull(archive.savedTiles)
        assertNull(archive.phrases)
        assertEquals(0, summary.savedButtons)
        assertNull(archive.settings?.voiceId)
    }

    @Test
    fun aBarePageListAndASingleSharedPageAreAccepted() {
        assertEquals(2, BookBackup.read("""[{"id":"A"},{"id":"B"}]""").first.pages.size)
        assertEquals("Solo", BookBackup.read("""{"id":"S","title":"Solo"}""").first.pages.single().title)
    }

    private fun refused(text: String, expectedWords: String) {
        try { BookBackup.read(text); fail("expected refusal: $expectedWords") }
        catch (e: IllegalArgumentException) { assertTrue(e.message ?: "", (e.message ?: "").contains(expectedWords, ignoreCase = true)) }
    }

    @Test
    fun malformedContentIsRefusedBeforeAnythingIsApplied() {
        refused("""{"format":"talktiles.book","version":1,"createdAt":"x","pages":[]}""", "any pages")
        refused("""[{"id":"A"},{"id":"A"}]""", "same id")
        refused("""[{"id":"  "}]""", "no id")
        refused("""[{"id":"A","gridSize":0}]""", "grid")
        refused("""[{"id":"A","tiles":[0,{"id":0,"label":"x"}]}]""", "button")
        refused("""[{"id":"A","type":"Visual Scene Display","hotspots":[{"id":1,"x":120,"y":10,"w":10,"h":10}]}]""", "spot")
        refused("""[{"id":"A","type":"Visual Scene Display","hotspots":[{"id":1,"x":10,"y":10,"w":0,"h":10}]}]""", "spot")
        refused("""{"format":"talktiles.book","version":1,"createdAt":"x","pages":[{"id":"A"}],"savedTiles":[{"id":"s","name":"a","label":"a","tts":"a"},{"id":"s","name":"b","label":"b","tts":"b"}]}""", "saved button")
        refused("not json at all", "read")
    }

    @Test
    fun versionFromTheFutureIsRefusedWithAPlainMessage() {
        refused("""{"format":"talktiles.book","version":99,"createdAt":"x","pages":[{"id":"A"}]}""", "newer")
    }

    @Test
    fun summaryCountsWhatIsInside() {
        val text = BookBackup.encode(
            listOf(PageModel(id = "A", tiles = mapOf(1 to TileModel(1, "a"), 2 to TileModel(2, "b"))), PageModel(id = "B", type = PageType.SCENE, hotspots = listOf(HotspotModel(1)))),
            AppSettings(), emptyList(), emptyList(), createdAt = "t")
        val (_, summary) = BookBackup.read(text)
        assertEquals(2, summary.pages); assertEquals(2, summary.buttons); assertEquals(1, summary.hotspots)
    }
}
