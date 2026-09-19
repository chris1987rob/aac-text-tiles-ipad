package com.talktiles.tablet

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
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

/** The one sentence bar: remove last, clear with undo, speak, all reachable and big enough. */
@RunWith(RobolectricTestRunner::class)
class SentenceBarTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()

    private fun store(): AACStore = TestBook.store(tmp.root)

    @Test
    fun showsWordsInOrderAndTappingAWordTakesItOut() {
        val s = store()
        s.sentence.add(SentenceItem("I want", "I want")); s.sentence.add(SentenceItem("Eat", "Eat food")); s.sentence.add(SentenceItem("More", "More please"))
        rule.setContent { TalkTilesTheme { SentenceBar(s) } }
        rule.onNodeWithText("I want").assertIsDisplayed()
        rule.onNodeWithText("Eat").assertIsDisplayed()
        rule.onNodeWithContentDescription("Remove Eat").performClick()
        assertEquals(listOf("I want", "More"), s.sentence.items.map { it.label })
    }

    @Test
    fun theOnlyControlsAreSpeakAndTheWords() {
        val s = store()
        s.sentence.add(SentenceItem("Hi", "Hi"))
        rule.setContent { TalkTilesTheme { SentenceBar(s) } }
        rule.onNodeWithContentDescription("Speak sentence").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        for (gone in listOf("Remove last word", "Clear sentence", "Saved phrases", "Undo clear")) {
            assertEquals(0, rule.onAllNodesWithContentDescription(gone).fetchSemanticsNodes().size)
        }
    }
}
