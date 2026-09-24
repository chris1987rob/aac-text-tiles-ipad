package com.talktiles.tablet

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The promise on the website: when the trial ends, nothing goes. A book built
 * during the trial - far over the free limits - opens, talks, turns every
 * page and can still be edited. Only adding MORE pages or saving buttons asks for Pro.
 */
@RunWith(RobolectricTestRunner::class)
class FreeKeepsEverythingTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()

    private val expired = LicenceState(trialStartedAt = 1L, latestSeenAt = 1L)

    /** Starter book + 12 own grid pages + 3 scenes: well past 5 pages and 1 scene. */
    private fun bigBook(): List<PageModel> =
        AACStore.defaultPages() +
        List(12) { n -> PageModel(title = "Mine $n", tiles = mapOf(1 to TileModel(id = 1, label = "Word $n", tts = "Word $n"))) } +
        List(3) { n -> PageModel(title = "Scene $n", type = PageType.SCENE, hotspots = listOf(HotspotModel(id = 1, label = "Spot", tts = "Spot"))) }

    private fun freeStore(pages: List<PageModel> = bigBook()): AACStore {
        val s = TestBook.store(tmp.root, pages = pages, licence = expired)
        assertFalse("test must run on the free version", s.pro.hasFullAccess)
        return s
    }

    @Test
    fun everyPageIsStillInTheBookAfterTheTrialAndAfterARestart() {
        val book = bigBook()
        val s = freeStore(book)
        assertEquals(book.map { it.id }, s.pages.map { it.id })
        assertEquals(book, s.pages)
        s.saveNow()
        // A fresh launch on the same files, still free.
        val again = AACStore(androidx.test.core.app.ApplicationProvider.getApplicationContext(), BookStorage(tmp.root))
        assertFalse(again.pro.hasFullAccess)
        assertEquals(book, again.pages)
    }

    @Test
    fun theReaderCanTurnToEveryPage() {
        val s = freeStore()
        val seen = HashSet<String>()
        s.currentPageIndex = 0
        repeat(s.pages.size) { seen.add(s.currentPage.id); s.nextPage() }
        assertEquals(s.pages.map { it.id }.toSet(), seen)
    }

    @Test
    fun existingPagesAndScenesCanStillBeEdited() {
        val s = freeStore()
        val mine = s.pages.indexOfFirst { it.title == "Mine 7" }
        s.currentPageIndex = mine
        s.updateCurrentPage { it.copy(title = "Renamed", tiles = it.tiles + (2 to TileModel(id = 2, label = "New button"))) }
        assertEquals("Renamed", s.pages[mine].title)
        assertEquals(2, s.pages[mine].tiles.size)

        val scene = s.pages.indexOfFirst { it.title == "Scene 2" }
        s.currentPageIndex = scene
        s.updateCurrentPage { it.copy(hotspots = it.hotspots + HotspotModel(id = 2)) }
        assertEquals(2, s.pages[scene].hotspots.size)
    }

    @Test
    fun aBigBackupRestoresInFullOnTheFreeVersion() {
        val s = freeStore(AACStore.defaultPages())
        val big = bigBook()
        val (archive, _) = BookBackup.read(BookBackup.encode(big, AppSettings(), emptyList(), emptyList()))
        s.restore(archive)
        assertEquals(big.map { it.id }, s.pages.map { it.id })
    }

    @Test
    fun deletingAPageIsNeverBlocked() {
        val s = freeStore()
        val before = s.pages.size
        s.removePage(before - 1)
        assertEquals(before - 1, s.pages.size)
    }

    @Test
    fun buttonsSavedDuringTheTrialCanStillBePutOnPages() {
        val s = freeStore()
        TileFavorites.shared.add(SavedTile(name = "Grandma", label = "Grandma", tts = "Grandma"))
        val slot = s.currentPage.tiles.keys.first()
        rule.setContent { TalkTilesTheme { QuickEditSheet(s, slot) {} } }
        rule.onNodeWithText("Use a Saved Button").performScrollTo().performClick()
        rule.onNodeWithText("Grandma").assertIsDisplayed()
        assertTrue(TileFavorites.shared.contains("Grandma"))
    }

    @Test
    fun startTalkingNeverMentionsPro() {
        val s = freeStore()
        rule.setContent { TalkTilesTheme { RootView(s) } }
        rule.onNodeWithText("Start talking").performClick()
        rule.onNodeWithText(ProBlock.PAGES.message).assertDoesNotExist()
        rule.onNodeWithText("Talk Tiles Pro").assertDoesNotExist()
    }
}
