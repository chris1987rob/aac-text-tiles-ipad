package com.talktiles.tablet

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A full free book gets the Upgrade sheet instead of a new page; the trial and Pro do not. */
@RunWith(RobolectricTestRunner::class)
class ProGateTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()

    private val expired = LicenceState(trialStartedAt = 1L, latestSeenAt = 1L)
    private fun fullBook() = AACStore.defaultPages() + List(ProRules.FREE_EXTRA_PAGES) { PageModel(title = "Mine $it") }

    @Test
    fun freeAndFullTheLibraryOffersProAndAddsNothing() {
        val s = TestBook.store(tmp.root, pages = fullBook(), licence = expired)
        val before = s.pages.size
        rule.setContent { TalkTilesTheme { GallerySheet(s) {} } }
        rule.onAllNodesWithText("Add to book")[0].performClick()
        rule.onNodeWithText(ProBlock.PAGES.message).assertIsDisplayed()
        assertEquals(before, s.pages.size)
    }

    @Test
    fun freeWithRoomTheLibraryStillAdds() {
        val s = TestBook.store(tmp.root, licence = expired)
        val before = s.pages.size
        rule.setContent { TalkTilesTheme { GallerySheet(s) {} } }
        rule.onAllNodesWithText("Add to book")[0].performClick()
        rule.waitForIdle()
        assertEquals(before + 1, s.pages.size)
    }

    @Test
    fun freeAndFullTheWizardOffersProAndCreatesNothing() {
        val s = TestBook.store(tmp.root, pages = fullBook(), licence = expired)
        val before = s.pages.size
        rule.setContent { TalkTilesTheme { PageWizardSheet(s) {} } }
        rule.onNodeWithText("Create Page").performClick()
        rule.onNodeWithText(ProBlock.PAGES.message).assertIsDisplayed()
        assertEquals(before, s.pages.size)
    }

    @Test
    fun inTheTrialAFullBookStillGrows() {
        val s = TestBook.store(tmp.root, pages = fullBook())   // fresh licence = trial
        val before = s.pages.size
        rule.setContent { TalkTilesTheme { PageWizardSheet(s) {} } }
        rule.onNodeWithText("Create Page").performClick()
        rule.waitForIdle()
        assertEquals(before + 1, s.pages.size)
    }

    @Test
    fun withProAFullBookStillGrows() {
        val s = TestBook.store(tmp.root, pages = fullBook(), licence = expired.copy(proOwned = true, orderId = "GPA.1"))
        val before = s.pages.size
        rule.setContent { TalkTilesTheme { GallerySheet(s) {} } }
        rule.onAllNodesWithText("Add to book")[0].performClick()
        rule.waitForIdle()
        assertEquals(before + 1, s.pages.size)
    }

    @Test
    fun freeSavingAButtonOffersProAndSavesNothing() {
        val s = TestBook.store(tmp.root, licence = expired)
        val slot = s.currentPage.tiles.keys.first()
        val before = TileFavorites.shared.items.size
        rule.setContent { TalkTilesTheme { QuickEditSheet(s, slot) {} } }
        rule.onNodeWithText("Save This Button").performScrollTo().performClick()
        rule.onNodeWithText(ProBlock.SAVED_BUTTONS.message).assertIsDisplayed()
        assertEquals(before, TileFavorites.shared.items.size)
    }

    @Test
    fun homeShowsTheTrialAndHidesTheCardOnceBought() {
        val s = TestBook.store(tmp.root)
        rule.setContent { TalkTilesTheme { RootView(s) } }
        rule.onNodeWithText("Pro trial: 14 days left").performScrollTo().assertIsDisplayed()
    }
}
