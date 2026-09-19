package com.talktiles.tablet

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Rule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Settings speaks plainly and writes through to the store. */
@RunWith(RobolectricTestRunner::class)
class SettingsSheetTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()

    // A Dialog composition must be taken down inside the test: one left behind keeps a Recomposer
    // alive with no window to drive it, and every later Compose test then waits on it forever.
    private var open by mutableStateOf(true)
    @After fun closeSheet() { open = false; rule.waitForIdle() }

    @Test
    fun protectEditingAndDisplayTogglesWriteThrough() {
        val s = TestBook.store(tmp.root)
        rule.setContent { TalkTilesTheme { if (open) SettingsSheet(s, onDismiss = { open = false }) } }
        rule.onNodeWithText("Ask for a PIN before editing").performScrollTo().performClick()
        assertTrue(s.settings.childLock)
        rule.onNodeWithText("High contrast").performScrollTo().performClick()
        assertTrue(s.settings.highContrast)
        rule.onNodeWithText("Reduce motion").performScrollTo().performClick()
        assertTrue(s.settings.reduceMotion)
        rule.onNodeWithText("Open on the last page used").performScrollTo().performClick()
        assertFalse(s.settings.openOnLastPage)
        // The old wording is gone.
        assertTrue(rule.onAllNodes(hasText("Child Lock")).fetchSemanticsNodes().isEmpty())
        assertTrue(rule.onAllNodes(hasText("Lock editing")).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun aFileKeptAsideIsReportedWhereAParentWillSeeIt() {
        File(tmp.root, "aac_favorites.json").writeText("not json")
        val s = TestBook.store(tmp.root)
        rule.setContent { TalkTilesTheme { if (open) SettingsSheet(s, onDismiss = { open = false }) } }
        // Deep in a dialog's scroll column; Robolectric's dialog window cannot vouch for on-screen bounds, so: present and reachable.
        rule.onNodeWithText("NEEDS ATTENTION").performScrollTo().assertExists()
        rule.onNodeWithText("aac_favorites.json could not be read", substring = true).performScrollTo().assertExists()
    }
}
