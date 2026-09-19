package com.talktiles.tablet

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A tile is a button to a screen reader and a keyboard, and one touch is one activation. */
@RunWith(RobolectricTestRunner::class)
class TileViewSemanticsTest {
    @get:Rule val rule = createComposeRule()

    private val eat = TileModel(id = 2, label = "Eat", tts = "Eat food", bgHex = "#C8E6C9")
    private fun isButton() = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    private fun setTile(delay: Double = 0.0, onRelease: Boolean = false, onTap: () -> Unit) {
        rule.setContent {
            TalkTilesTheme {
                TileView(activationDelay = delay, activateOnRelease = onRelease, tile = eat, isEditMode = false,
                    cellWidth = 160.dp, cellHeight = 160.dp, onTap = onTap)
            }
        }
    }

    @Test
    fun exposesRoleButtonAndWhatItSays() {
        setTile {}
        rule.onNode(hasContentDescription("Eat", substring = true) and isButton()).assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun semanticClickActivatesExactlyOnce() {
        var fired = 0
        setTile { fired++ }
        // The screen-reader path: the semantics action, not a touch.
        rule.onNode(isButton()).performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        assertEquals(1, fired)
    }

    @Test
    fun aTouchActivatesExactlyOnce() {
        var fired = 0
        setTile { fired++ }
        rule.onNode(isButton()).performTouchInput { down(center); up() }
        rule.waitForIdle()
        assertEquals(1, fired)
    }

    @Test
    fun aCancelledTouchDoesNotSpeakWhenSpeakingOnLift() {
        var fired = 0
        setTile(onRelease = true) { fired++ }
        rule.onNode(isButton()).performTouchInput { down(center); cancel() }
        rule.waitForIdle()
        assertEquals(0, fired)
        rule.onNode(isButton()).performTouchInput { down(center); up() }
        rule.waitForIdle()
        assertEquals(1, fired)
    }

    @Test
    fun liftingBeforeTheDwellDoesNotSpeak() {
        var fired = 0
        setTile(delay = 1.5) { fired++ }
        rule.onNode(isButton()).performTouchInput { down(center); up() }
        rule.waitForIdle()
        assertEquals(0, fired)
    }

    @Test
    fun emptySlotInTheEditorIsAnAddButton() {
        rule.setContent { TalkTilesTheme { TileView(tile = null, isEditMode = true, cellWidth = 120.dp, cellHeight = 120.dp, slot = 5, onTap = {}) } }
        rule.onNode(hasContentDescription("Empty button 5", substring = true) and isButton()).assertHasClickAction()
    }

    @Test
    fun aTouchCallsTheActionOfThePageNowOnScreen() {
        // Two pages with an identical button in the same slot: the tile keys match, so the
        // press controller is kept across the page turn. It must still fire the new page's action.
        var page by mutableStateOf("Core Words")
        val firedOn = ArrayList<String>()
        rule.setContent {
            TalkTilesTheme {
                val here = page
                TileView(tile = eat, isEditMode = false, cellWidth = 160.dp, cellHeight = 160.dp, onTap = { firedOn.add(here) })
            }
        }
        rule.onNode(isButton()).performTouchInput { down(center); up() }
        rule.waitForIdle()
        page = "Food & Snacks"
        rule.waitForIdle()
        rule.onNode(isButton()).performTouchInput { down(center); up() }
        rule.waitForIdle()
        assertEquals(listOf("Core Words", "Food & Snacks"), firedOn)
    }
}
