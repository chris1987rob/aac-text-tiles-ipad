package com.talktiles.tablet

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Talking spots are buttons too, and a jump to a switched-off page is refused. */
@RunWith(RobolectricTestRunner::class)
class SceneSemanticsTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun hotspotsAreButtonsAndJumpsHonourEnabled() {
        val scene = PageModel(id = "S", title = "Room", type = PageType.SCENE, hotspots = listOf(
            HotspotModel(id = 1, label = "Sofa", tts = "Sit on the sofa"),
            HotspotModel(id = 2, label = "Go", tts = "", action = HotspotAction.JUMP, jumpPageId = "OFF", x = 60.0),
            HotspotModel(id = 3, label = "Food", tts = "", action = HotspotAction.JUMP, jumpPageId = "ON", y = 60.0)))
        val s = TestBook.store(tmp.root, listOf(scene, PageModel(id = "OFF", title = "Off", enabled = false), PageModel(id = "ON", title = "On")))
        rule.setContent { TalkTilesTheme { VisualSceneView(s, onSelectHotspot = {}, onAddHotspot = {}) } }
        val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        rule.onNode(hasContentDescription("Sofa. Says: Sit on the sofa") and isButton).assertHasClickAction()
        rule.onNode(hasContentDescription("Go. Opens a page")).performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        assertEquals("refused: that page is off", "S", s.currentPage.id)
        rule.onNode(hasContentDescription("Food. Opens a page")).performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        assertEquals("ON", s.currentPage.id)
    }
}
