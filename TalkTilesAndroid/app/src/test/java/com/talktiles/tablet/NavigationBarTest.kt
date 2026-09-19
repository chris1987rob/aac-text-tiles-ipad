package com.talktiles.tablet

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Previous / next / where-am-I on the board's bar, honouring switched-off pages. */
@RunWith(RobolectricTestRunner::class)
class NavigationBarTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()

    private val pages = listOf(
        PageModel(id = "A", title = "Core"), PageModel(id = "B", title = "Hidden", enabled = false), PageModel(id = "C", title = "Food"))

    @Test
    fun playerBarStepsOverDisabledPagesAndShowsPosition() {
        val s = TestBook.store(tmp.root, pages)
        s.isEditMode = false
        rule.setContent { TalkTilesTheme { NavigationBarView(s, onOpenFind = {}, onOpenOptions = {}, onOpenNewPage = {}, onGoHome = {}) } }
        rule.onNodeWithText("Core").assertIsDisplayed()
        rule.onNodeWithText("1 of 2").assertIsDisplayed()
        rule.onNodeWithContentDescription("Next page").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        assertEquals("C", s.currentPage.id)
        rule.onNodeWithText("2 of 2").assertIsDisplayed()
        rule.onNodeWithContentDescription("Previous page").performClick()
        assertEquals("A", s.currentPage.id)
    }

    @Test
    fun editorBarSeesEveryPage() {
        val s = TestBook.store(tmp.root, pages)
        s.isEditMode = true
        rule.setContent { TalkTilesTheme { NavigationBarView(s, onOpenFind = {}, onOpenOptions = {}, onOpenNewPage = {}, onGoHome = {}) } }
        rule.onNodeWithContentDescription("Next page").performClick()
        assertEquals("B", s.currentPage.id)
        rule.onNodeWithText("2 of 3").assertIsDisplayed()
        rule.onNodeWithContentDescription("Page options").assertIsDisplayed()
        rule.onNodeWithContentDescription("New page").assertIsDisplayed()
    }

    @Test
    fun thePageNameOpensThePageListInTheEditor() {
        // Help says so, and it is the only door to switching pages off, moving and deleting them.
        val s = TestBook.store(tmp.root, pages)
        s.isEditMode = true
        var opened = 0
        rule.setContent { TalkTilesTheme { NavigationBarView(s, onOpenFind = { opened++ }, onOpenOptions = {}, onOpenNewPage = {}, onGoHome = {}) } }
        rule.onNodeWithText("Core").performClick()
        rule.waitForIdle()
        assertEquals(1, opened)
    }
}
