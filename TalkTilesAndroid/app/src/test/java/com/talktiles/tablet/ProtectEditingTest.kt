package com.talktiles.tablet

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** With protection on, every way to change the book asks for the PIN; talking never does. */
@RunWith(RobolectricTestRunner::class)
class ProtectEditingTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()

    private fun locked() = TestBook.store(tmp.root, settings = AppSettings(childLock = true, lockPIN = "2580"))

    private fun enterPin(pin: String) { for (d in pin) rule.onNodeWithContentDescription("Key $d").performClick() }

    @Test
    fun startTalkingNeverAsksForThePin() {
        val s = locked()
        rule.setContent { TalkTilesTheme { RootView(s) } }
        rule.onNodeWithText("Start talking").performClick()
        rule.onNodeWithContentDescription("Next page").assertIsDisplayed()
        assertFalse(s.isEditMode)
    }

    @Test
    fun editPagesAsksForThePinAndOpensTheEditorOnTheRightOne() {
        val s = locked()
        rule.setContent { TalkTilesTheme { RootView(s) } }
        rule.onNodeWithText("Edit pages").performClick()
        rule.onNodeWithText("Protect editing").assertIsDisplayed()
        enterPin("1111")
        rule.onNodeWithText("That PIN is not right. Try again.").assertIsDisplayed()
        assertFalse(s.isEditMode)
        enterPin("2580")
        rule.waitForIdle()
        assertTrue(s.isEditMode)
    }

    @Test
    fun settingsAndLibraryAreBehindThePinToo() {
        val s = locked()
        rule.setContent { TalkTilesTheme { RootView(s) } }
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithText("Protect editing").assertIsDisplayed()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Page library").performClick()
        rule.onNodeWithText("Protect editing").assertIsDisplayed()
    }

    @Test
    fun withProtectionOffNothingAsks() {
        val s = TestBook.store(tmp.root, settings = AppSettings(childLock = false))
        rule.setContent { TalkTilesTheme { RootView(s) } }
        rule.onNodeWithText("Edit pages").performClick()
        rule.waitForIdle()
        assertTrue(s.isEditMode)
    }
}
