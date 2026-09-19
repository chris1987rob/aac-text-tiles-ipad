package com.talktiles.tablet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CountDownLatch

/** The book on disk: newer always wins, unreadable is kept, nothing is silently replaced. */
class BookStorageTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun storage(clock: () -> Long = { 1_700_000_000_000L }) = BookStorage(tmp.root, clock)
    private fun pages(vararg titles: String) = titles.map { PageModel(id = it, title = it) }

    @Test
    fun missingFileIsReportedAsMissingNotAsAnError() {
        assertEquals(BookStorage.Load.Missing, storage().loadPages())
        assertEquals(BookStorage.Load.Missing, storage().loadSettings())
        assertEquals(BookStorage.Load.Missing, storage().loadFavorites())
        assertEquals(BookStorage.Load.Missing, storage().loadPhrases())
    }

    @Test
    fun pagesRoundTrip() {
        val s = storage()
        assertTrue(s.writePages(pages("A", "B"), generation = 1))
        val back = s.loadPages() as BookStorage.Load.Ok
        assertEquals(listOf("A", "B"), back.value.map { it.id })
    }

    @Test
    fun anOlderSnapshotNeverOverwritesANewerOne() {
        val s = storage()
        assertTrue(s.writePages(pages("new"), generation = 2))
        assertFalse(s.writePages(pages("old"), generation = 1))
        assertFalse(s.writePages(pages("same"), generation = 2))
        assertEquals(listOf("new"), (s.loadPages() as BookStorage.Load.Ok).value.map { it.id })
    }

    @Test
    fun olderWriteArrivingFromAnotherThreadIsStillDropped() {
        val s = storage()
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        // A debounce thread that was created with generation 1 but only gets to run after generation 2 was written.
        val late = Thread {
            started.countDown()
            release.await()
            s.writePages(pages("late"), generation = 1)
        }
        late.start(); started.await()
        assertTrue(s.writePages(pages("current"), generation = 2))
        release.countDown(); late.join()
        assertEquals(listOf("current"), (s.loadPages() as BookStorage.Load.Ok).value.map { it.id })
    }

    @Test
    fun unreadableBookIsMovedAsideAndTheBytesSurvive() {
        val s = storage { 1_726_700_000_000L }
        val f = File(tmp.root, "aac_pages.json")
        f.writeText("""[{"id":"A","title":"Half a page""")
        val result = s.loadPages()
        assertTrue(result is BookStorage.Load.Unreadable)
        val kept = (result as BookStorage.Load.Unreadable).keptAs
        assertTrue(kept.name, kept.name.startsWith("aac_pages.json.unreadable-"))
        assertEquals("""[{"id":"A","title":"Half a page""", kept.readText())
        assertFalse("original path must be free for a fresh book", f.exists())
        assertEquals(1, s.notices.size)

        // The fresh book written afterwards does not touch the kept file.
        s.writePages(pages("fresh"), generation = 1)
        assertTrue(kept.exists())
        assertEquals(listOf("fresh"), (s.loadPages() as BookStorage.Load.Ok).value.map { it.id })
    }

    @Test
    fun unreadableSettingsAndFavouritesAreKeptTheSameWay() {
        val s = storage()
        File(tmp.root, "aac_settings.json").writeText("{ not json")
        File(tmp.root, "aac_favorites.json").writeText("<xml/>")
        File(tmp.root, "aac_phrases.json").writeText("nope")
        assertTrue(s.loadSettings() is BookStorage.Load.Unreadable)
        assertTrue(s.loadFavorites() is BookStorage.Load.Unreadable)
        assertTrue(s.loadPhrases() is BookStorage.Load.Unreadable)
        assertEquals(3, tmp.root.listFiles()!!.count { it.name.contains(".unreadable-") })
    }

    @Test
    fun emptyPagesFileIsNotCorruptJustEmpty() {
        File(tmp.root, "aac_pages.json").writeText("[]")
        val r = storage().loadPages()
        assertTrue(r is BookStorage.Load.Ok && r.value.isEmpty())
    }

    @Test
    fun settingsFavouritesAndPhrasesRoundTrip() {
        val s = storage()
        s.writeSettings(AppSettings(voiceId = null, childLock = true, lockPIN = "4321"))
        val settings = (s.loadSettings() as BookStorage.Load.Ok).value
        assertEquals(null, settings.voiceId)
        assertTrue(settings.childLock)
        assertEquals("4321", settings.lockPIN)

        s.writeFavorites(listOf(SavedTile(id = "f1", name = "Mum", label = "Mum", tts = "Mum", audioData = byteArrayOf(1))))
        assertEquals("f1", (s.loadFavorites() as BookStorage.Load.Ok).value.single().id)

        s.writePhrases(listOf(SavedPhrase(id = "p1", name = "Snack", items = listOf(SentenceItem("Eat", "Eat food")))))
        assertEquals("Snack", (s.loadPhrases() as BookStorage.Load.Ok).value.single().name)
    }

    @Test
    fun snapshotCopiesTheCurrentBookBeforeSomethingReplacesIt() {
        val s = storage { 1_726_700_000_000L }
        s.writePages(pages("keep-me"), generation = 1)
        val snap = s.snapshotBook("pre-restore")
        assertNotNull(snap)
        assertTrue(snap!!.name, snap.name.startsWith("aac_pages-pre-restore-"))
        assertTrue(snap.readText().contains("keep-me"))
        assertEquals(null, BookStorage(tmp.newFolder("empty")).snapshotBook("nothing-there-yet"))
    }

    @Test
    fun onlyAHandfulOfSnapshotsAreKept() {
        var t = 1_726_700_000_000L
        val s = storage { t }
        s.writePages(pages("A"), generation = 1)
        repeat(8) { s.snapshotBook("pre-restore"); t += 60_000 }
        val kept = File(tmp.root, "snapshots").listFiles()!!.filter { it.name.startsWith("aac_pages-") }
        assertEquals(BookStorage.SNAPSHOT_LIMIT, kept.size)
    }

    @Test
    fun writeIsAtomicNoHalfWrittenFileIsLeftBehind() {
        val s = storage()
        s.writePages(pages("A"), generation = 1)
        assertFalse(File(tmp.root, "aac_pages.json.tmp").exists())
    }
}
