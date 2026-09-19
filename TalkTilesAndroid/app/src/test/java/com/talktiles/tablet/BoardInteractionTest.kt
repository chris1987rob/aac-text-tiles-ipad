package com.talktiles.tablet

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** The board as a whole: grid presses feed the sentence, keys are buttons, Find navigates silently, phrases come back exactly. */
@RunWith(RobolectricTestRunner::class)
class BoardInteractionTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()

    private val eat = TileModel(id = 1, label = "Eat", tts = "Eat food")
    private val food = PageModel(id = "F", title = "Food", express = true, gridSize = 4, tiles = mapOf(1 to eat, 2 to TileModel(id = 2, label = "Drink", tts = "Drink water")))
    private val keyboard = PageModel(id = "K", title = "Keyboard", type = PageType.KEYBOARD)

    @Test
    fun pressingAGridButtonPutsWhatItSaysInTheSentence() {
        val s = TestBook.store(tmp.root, listOf(food, keyboard))
        rule.setContent { TalkTilesTheme { BoardView(s, {}, {}, {}, {}, {}, {}, {}, {}, {}) } }
        rule.onNode(hasContentDescription("Eat. Says: Eat food")).performClick()
        rule.waitForIdle()
        assertEquals(listOf("Eat food"), s.sentence.items.map { it.spoken })
        assertEquals(listOf("Eat"), s.sentence.items.map { it.label })
        assertEquals("the tile and the word in the bar", 2, rule.onAllNodes(hasText("Eat")).fetchSemanticsNodes().size)
        // Turning the page keeps the sentence and the same bar.
        rule.onNodeWithContentDescription("Next page").performClick()
        rule.waitForIdle()
        assertEquals("K", s.currentPage.id)
        assertEquals(1, s.sentence.items.size)
        rule.onNodeWithContentDescription("Speak sentence").assertIsDisplayed()
    }

    @Test
    fun keyboardKeysAreButtonsAndFeedTheSameSentence() {
        val s = TestBook.store(tmp.root, listOf(keyboard, food))
        rule.setContent { TalkTilesTheme { BoardView(s, {}, {}, {}, {}, {}, {}, {}, {}, {}) } }
        val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        val key = rule.onAllNodes(isButton and hasContentDescription("I", substring = false)).fetchSemanticsNodes()
        assertTrue("a key labelled I on the People group", key.isNotEmpty())
        rule.onNode(isButton and hasContentDescription("I")).assertHasClickAction().assertHeightIsAtLeast(48.dp).performClick()
        rule.waitForIdle()
        assertEquals(listOf("I"), s.sentence.items.map { it.label })
    }

    @Test
    fun findOpensThePageAWordIsOnWithoutSpeaking() {
        val s = TestBook.store(tmp.root, listOf(keyboard, food, PageModel(id = "OFF", title = "Off", enabled = false, tiles = mapOf(1 to TileModel(1, "Secret")))))
        var closed = false
        rule.setContent { TalkTilesTheme { FindSheet(s, onDismiss = { closed = true }) } }
        rule.onNodeWithText("Page name or word").performTextInput("drink")
        rule.onNode(hasText("Drink")).assertHasClickAction().performClick()
        rule.waitForIdle()
        assertEquals("F", s.currentPage.id)
        assertTrue(closed)
        assertFalse("nothing spoken", SpeechManager.shared.isSpeaking)
        assertTrue(s.sentence.isEmpty)
    }

    @Test
    fun findDoesNotOfferSwitchedOffPagesWhileTalking() {
        val s = TestBook.store(tmp.root, listOf(food, PageModel(id = "OFF", title = "Off", enabled = false, tiles = mapOf(1 to TileModel(1, "Secret")))))
        rule.setContent { TalkTilesTheme { FindSheet(s, onDismiss = {}) } }
        assertTrue(rule.onAllNodes(hasText("Off")).fetchSemanticsNodes().isEmpty())
        rule.onNodeWithText("Page name or word").performTextInput("secret")
        rule.onNodeWithText("No button says \"secret\".").assertIsDisplayed()
    }

    @Test
    fun aSavedPhraseComesBackWordForWordWithItsRecording() {
        val s = TestBook.store(tmp.root, listOf(food))
        s.sentence.add(SentenceItem("Mum", "Mum", audioData = byteArrayOf(4, 2)))
        s.sentence.add(SentenceItem.from(eat))
        rule.setContent { TalkTilesTheme { PhrasesSheet(s, onDismiss = {}) } }
        rule.onNodeWithText("Save this sentence").performClick()
        rule.waitForIdle()
        val saved = PhraseLibrary.shared.items.single()
        assertEquals("Mum Eat", saved.name)
        assertEquals(listOf("Mum", "Eat food"), saved.items.map { it.spoken })
        assertTrue(saved.items[0].hasRecording)
        // Survives a reload from disk.
        assertEquals(saved, (s.storage.loadPhrases() as BookStorage.Load.Ok).value.single())
        // Tapping it puts it back in the bar.
        s.sentence.clear()
        rule.onNodeWithText("Mum Eat").performClick()
        rule.waitForIdle()
        assertEquals(listOf("Mum", "Eat"), s.sentence.items.map { it.label })
    }

    @Test
    fun homeStaysUsableAtLargeFontAndInLandscape() {
        val s = TestBook.store(tmp.root, listOf(food))
        rule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale = 1.6f)) {
                TalkTilesTheme { HomeView(s, {}, {}, {}, {}, {}) }
            }
        }
        rule.onNodeWithText("Start talking").assertIsDisplayed().assertHeightIsAtLeast(56.dp)
        for (t in listOf("Edit pages", "Page library", "Settings", "Help")) rule.onNodeWithText(t).assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun homeLaysOutSideBySideInLandscape() {
        RuntimeEnvironment.setQualifiers("w853dp-h533dp-hdpi")
        val s = TestBook.store(tmp.root, listOf(food))
        rule.setContent { TalkTilesTheme { HomeView(s, {}, {}, {}, {}, {}) } }
        rule.onNodeWithText("Start talking").assertIsDisplayed()
        rule.onNodeWithText("Help").assertIsDisplayed()
    }

    @Test
    fun deletingAPageAsksFirstAndOnlyThenRemovesIt() {
        // One stray tap in a scrolling list must not take a page and its recordings with it.
        val s = TestBook.store(tmp.root, listOf(food, PageModel(id = "B", title = "Bedtime", tiles = mapOf(1 to TileModel(1, "Sleep")))))
        s.isEditMode = true
        rule.setContent { TalkTilesTheme { FindSheet(s, onDismiss = {}) } }
        rule.onNodeWithContentDescription("Delete Bedtime").performClick()
        rule.waitForIdle()
        assertEquals(listOf("F", "B"), s.pages.map { it.id })          // still there: a question was asked
        rule.onNodeWithText("Keep it").performClick()
        rule.waitForIdle()
        assertEquals(listOf("F", "B"), s.pages.map { it.id })
        rule.onNodeWithContentDescription("Delete Bedtime").performClick()
        rule.onNodeWithText("Delete page").performClick()
        rule.waitForIdle()
        assertEquals(listOf("F"), s.pages.map { it.id })
    }
}
