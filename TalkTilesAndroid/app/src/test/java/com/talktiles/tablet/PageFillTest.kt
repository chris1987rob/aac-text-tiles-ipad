package com.talktiles.tablet

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A page made bigger than its buttons gets matching pictures, not empty spaces. */
@RunWith(RobolectricTestRunner::class)
class PageFillTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()

    private fun food() = PageTemplateCatalog.all.first { it.id == "food" }

    @Test
    fun theFoodBoardAt36GetsTwentyMoreFoodsAndKeepsItsOwnSixteen() {
        TestBook.store(tmp.root)
        val before = food().makePage().copy(gridSize = 36)
        val after = PageFill.fill(before)
        assertEquals((1..36).toList(), after.tiles.keys.sorted())
        for (slot in 1..16) assertEquals(before.tiles[slot], after.tiles[slot])
        val added = (17..36).map { after.tiles[it]!! }
        assertTrue(added.all { it.symbolName!!.startsWith("tt:") })
        assertTrue("added pictures should be food", added.count { PageFill.symbolFor(it)?.category == "food" } >= 18)
        val words = after.tiles.values.map { SpokenText.normalisedPhrase(it.label) }
        assertEquals("no word twice", words.size, words.toSet().size)
    }

    @Test
    fun newButtonsWearThePagesUsualColours() {
        TestBook.store(tmp.root)
        val after = PageFill.fill(food().makePage().copy(gridSize = 25))
        assertEquals(TemplateColor.noun, after.tiles[20]!!.bgHex)
    }

    @Test
    fun aBlankPageStaysBlankAndAFullPageIsUntouched() {
        TestBook.store(tmp.root)
        val blank = PageModel(title = "Mine", gridSize = 9)
        assertEquals(blank, PageFill.fill(blank))
        val full = food().makePage()
        assertEquals(full, PageFill.fill(full))
        val scene = PageModel(type = PageType.SCENE, gridSize = 4)
        assertEquals(scene, PageFill.fill(scene))
    }

    @Test
    fun theWizardFillsABoardAddedAtABiggerSize() {
        val s = TestBook.store(tmp.root)
        rule.setContent { TalkTilesTheme { PageWizardSheet(s) {} } }
        rule.onNodeWithText("36").performClick()
        rule.onNodeWithText("Food & Snacks").performClick()
        rule.onNodeWithText("Create Page").performClick()
        rule.waitForIdle()
        val page = s.pages.last()
        assertEquals(36, page.gridSize)
        assertTrue(PageFill.emptySlots(page).isEmpty())
    }

    @Test
    fun makingCoreWordsBiggerInPageOptionsFillsTheNewSpaces() {
        val s = TestBook.store(tmp.root)
        s.currentPageIndex = s.pages.indexOfFirst { it.title == "Core Words" }
        rule.setContent { TalkTilesTheme { PageOptionsSheet(s) {} } }
        rule.onNodeWithText("16").performClick()
        rule.onNodeWithText("Fill the 7 empty spaces with matching pictures").assertExists()
        rule.onNodeWithText("Save").performClick()
        rule.waitForIdle()
        assertEquals((1..16).toList(), s.currentPage.tiles.keys.sorted())
    }
}
