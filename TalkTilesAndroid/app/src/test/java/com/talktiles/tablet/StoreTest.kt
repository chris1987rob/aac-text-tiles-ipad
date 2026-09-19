package com.talktiles.tablet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import android.os.Looper
import java.io.File

/** The store as the app uses it: where it opens, what it remembers, what a restore keeps. */
@RunWith(RobolectricTestRunner::class)
class StoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun page(id: String, enabled: Boolean = true) = PageModel(id = id, title = id, enabled = enabled)
    private val book = listOf(page("A"), page("B", enabled = false), page("C"))

    private fun store(pages: List<PageModel> = book, settings: AppSettings = AppSettings()) = TestBook.store(tmp.root, pages, settings)
    private fun flush() = shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1))

    /**
     * This class writes Compose state (the store's mutableStateOf fields) with no
     * Compose rule running. Once an earlier Compose test has started the global
     * snapshot manager, each such write posts a flush to the main looper; if the
     * test ends first, Robolectric drops that message and the Compose UI
     * dispatcher stays marked "scheduled" - every later Compose test then waits
     * for idle forever. Running the looper before leaving keeps the JVM sane.
     */
    @After fun drainMainLooper() { shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(2)) }

    @Test
    fun opensOnTheRememberedPageWhenItIsStillOnAndOnTheFirstOtherwise() {
        assertEquals("C", store(settings = AppSettings(lastPageId = "C")).currentPage.id)
        assertEquals("A", store(settings = AppSettings(lastPageId = "B")).currentPage.id)
        assertEquals("A", store(settings = AppSettings(lastPageId = "C", openOnLastPage = false)).currentPage.id)
    }

    @Test
    fun turningPagesWhileTalkingIsRememberedOnDisk() {
        val s = store()
        s.nextPage()
        assertEquals("C", s.currentPage.id)
        flush()
        val reloaded = (s.storage.loadSettings() as BookStorage.Load.Ok).value
        assertEquals("C", reloaded.lastPageId)
    }

    @Test
    fun editorPositionIsNotRememberedAndLeavingItOnAnOffPageMovesToAnOnPage() {
        val s = store()
        s.isEditMode = true
        s.nextPage()                         // A -> B (off)
        assertEquals("B", s.currentPage.id)
        flush()
        assertEquals(null, (s.storage.loadSettings() as BookStorage.Load.Ok).value.lastPageId)
        s.enterPlayer()
        assertFalse(s.isEditMode)
        assertEquals("A", s.currentPage.id)
    }

    @Test
    fun goToPageRefusesAnOffPageWhileTalking() {
        val s = store()
        assertFalse(s.goToPage("B"))
        assertEquals("A", s.currentPage.id)
        s.isEditMode = true
        assertTrue(s.goToPage("B"))
    }

    @Test
    fun restoreKeepsASnapshotAndBringsSavedButtonsAndPhrases() {
        val s = store()
        val archiveText = BookBackup.encode(listOf(page("X"), page("Y")), AppSettings(lockPIN = "9999", lastPageId = "Y"),
            listOf(SavedTile(id = "f", name = "Mum", label = "Mum", tts = "Mum")), listOf(SavedPhrase(id = "p", name = "Hi", items = listOf(SentenceItem("Hi", "Hi")))))
        val (archive, _) = BookBackup.read(archiveText)
        s.restore(archive)
        assertEquals(listOf("X", "Y"), s.pages.map { it.id })
        assertEquals("X", s.currentPage.id)
        assertEquals("9999", s.settings.lockPIN)
        assertEquals("what this device remembers is not the archive's", null, s.settings.lastPageId)
        assertEquals("Mum", TileFavorites.shared.items.single().name)
        assertEquals("Hi", PhraseLibrary.shared.items.single().name)
        val snaps = File(tmp.root, "snapshots").listFiles()!!.filter { it.name.startsWith("aac_pages-pre-restore-") }
        assertEquals(1, snaps.size)
        assertTrue(snaps[0].readText().contains("\"A\""))
        // And the restored book is what is on disk now, not the old one.
        assertEquals(listOf("X", "Y"), (s.storage.loadPages() as BookStorage.Load.Ok).value.map { it.id })
    }

    @Test
    fun anUnreadableBookStartsTheStarterBookAndSaysSo() {
        File(tmp.root, "aac_pages.json").writeText("{{{")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = BookStorage(tmp.root)
        val s = AACStore(context, storage)
        assertTrue(s.pages.isNotEmpty())
        assertEquals(1, s.recoveryNotices.size)
        val kept = tmp.root.listFiles()!!.single { f -> f.name.startsWith("aac_pages.json.unreadable-") }
        assertEquals("{{{", kept.readText())
        // Editing and saving the fresh book leaves the kept bytes exactly where they are.
        s.updateCurrentPage { it.copy(title = "touched") }
        s.saveNow()
        assertEquals("{{{", kept.readText())
        assertTrue((storage.loadPages() as BookStorage.Load.Ok).value.any { it.title == "touched" })
    }

    @Test
    fun theTabletsOwnBookLoadsUnchanged() {
        // Chris's real book, pulled by Hermes before the upgrade. Not copied into the repo; skipped where it is absent.
        val f = File("/home/mike/Desktop/TalkTiles-Backups/20260918-194636-before-modernization-v1.2/TalkTiles-tablet-book-before-upgrade.json")
        assumeTrue(f.exists())
        val (archive, summary) = BookBackup.read(f.readText())
        assertEquals(7, summary.pages)
        assertEquals(52, summary.buttons)
        val s = store(pages = archive.pages, settings = archive.settings ?: AppSettings())
        assertEquals(archive.pages.map { it.id }, s.pages.map { it.id })
        // Every tile stays in its slot with its own colours.
        for ((i, p) in archive.pages.withIndex()) {
            assertEquals(p.tiles.keys.sorted(), s.pages[i].tiles.keys.sorted())
            for ((slot, t) in p.tiles) assertEquals(t, s.pages[i].tiles[slot])
        }
        assertEquals(SpokenText.BELLA_VOICE_ID, s.settings.voiceId)
        // Writing it back and reading it again is the identity.
        s.saveNow()
        assertEquals(archive.pages, (s.storage.loadPages() as BookStorage.Load.Ok).value)
    }
}
