package com.talktiles.tablet

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * The whole board on the tablet's screen (800x1280 @240dpi = 533x853dp, and
 * landscape): the toolbar is at the top and every button is inside the
 * viewport with real size. Guards the bar-takes-the-screen bug found on the
 * device (a fillMaxHeight inside a Row with unbounded height).
 */
@RunWith(RobolectricTestRunner::class)
class BoardLayoutRegressionTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()

    private fun grid(id: String, size: Int, express: Boolean = false) = PageModel(id = id, title = id, gridSize = size, express = express,
        tiles = (1..size).associateWith { TileModel(id = it, label = "W$it", tts = "Word $it") })

    private fun assertBoardFits(store: AACStore, expectedTiles: Int, width: Int, height: Int) {
        val density = rule.density.density
        val root = rule.onRoot().fetchSemanticsNode().size
        val bar = rule.onNodeWithContentDescription("Next page").fetchSemanticsNode().boundsInRoot
        assertTrue("toolbar must sit at the top, was top=${bar.top / density}dp", bar.top / density < 24f)
        assertTrue("toolbar button must have size", bar.height / density >= 48f)
        val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        val tiles = rule.onAllNodes(isButton and hasContentDescription("Says: Word", substring = true)).fetchSemanticsNodes()
        assertEquals("every button of the page is in the tree", expectedTiles, tiles.size)
        for (t in tiles) {
            val b = t.boundsInRoot
            assertTrue("tile ${t.config[SemanticsProperties.ContentDescription]} has no height: $b", b.height / density >= 48f)
            assertTrue("tile has no width: $b", b.width / density >= 48f)
            assertTrue("tile below the viewport: $b vs $root", b.bottom <= root.height + 0.5f)
            assertTrue("tile right of the viewport: $b vs $root", b.right <= root.width + 0.5f)
            assertTrue("tile above/left of the viewport: $b", b.top >= bar.bottom - 0.5f && b.left >= 0f)
        }
        val sentence = rule.onAllNodes(hasContentDescription("Speak sentence")).fetchSemanticsNodes()
        for (s in sentence) assertTrue("sentence bar has no height", s.boundsInRoot.height / density >= 48f)
    }

    @Test
    fun portraitCoreWordsWithSentenceBarFits() {
        val s = TestBook.store(tmp.root, listOf(grid("Core", 9, express = true), grid("More", 4)))
        rule.setContent { TalkTilesTheme { BoardView(s, {}, {}, {}, {}, {}, {}, {}, {}, {}) } }
        assertBoardFits(s, 9, 533, 853)
    }

    @Test
    fun portraitFortyEightGridFits() {
        val s = TestBook.store(tmp.root, listOf(grid("Big", 48)))
        rule.setContent { TalkTilesTheme { BoardView(s, {}, {}, {}, {}, {}, {}, {}, {}, {}) } }
        assertBoardFits(s, 48, 533, 853)
    }

    @Test
    fun landscapeGridFits() {
        RuntimeEnvironment.setQualifiers("w853dp-h533dp-hdpi")
        val s = TestBook.store(tmp.root, listOf(grid("Core", 12, express = true)))
        rule.setContent { TalkTilesTheme { BoardView(s, {}, {}, {}, {}, {}, {}, {}, {}, {}) } }
        assertBoardFits(s, 12, 853, 533)
    }

    @Test
    fun editorBoardFitsToo() {
        val s = TestBook.store(tmp.root, listOf(grid("Core", 9)))
        s.isEditMode = true
        rule.setContent { TalkTilesTheme { BoardView(s, {}, {}, {}, {}, {}, {}, {}, {}, {}) } }
        val density = rule.density.density
        val bar = rule.onNodeWithContentDescription("New page").fetchSemanticsNode().boundsInRoot
        assertTrue("editor toolbar at the top", bar.top / density < 24f)
        val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        val tiles = rule.onAllNodes(isButton and hasContentDescription("Edit button", substring = true)).fetchSemanticsNodes()
        assertEquals(9, tiles.size)
        val root = rule.onRoot().fetchSemanticsNode().size
        for (t in tiles) assertTrue("${t.boundsInRoot}", t.boundsInRoot.height / density >= 48f && t.boundsInRoot.bottom <= root.height + 0.5f)
    }

    @Test
    fun keyboardPageFits() {
        val s = TestBook.store(tmp.root, listOf(PageModel(id = "K", title = "Keys", type = PageType.KEYBOARD)))
        rule.setContent { TalkTilesTheme { BoardView(s, {}, {}, {}, {}, {}, {}, {}, {}, {}) } }
        val density = rule.density.density
        val root = rule.onRoot().fetchSemanticsNode().size
        val bar = rule.onNodeWithContentDescription("Next page").fetchSemanticsNode().boundsInRoot
        assertTrue(bar.top / density < 24f)
        val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        val keys = rule.onAllNodes(isButton and hasContentDescription("I")).fetchSemanticsNodes()
        assertEquals(1, keys.size)
        val b = keys[0].boundsInRoot
        assertTrue("key inside viewport with size: $b", b.height / density >= 48f && b.bottom <= root.height + 0.5f && b.top > bar.bottom)
    }
}
