package com.talktiles.tablet

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The built-in example scenes, and spots that stay on the picture. */
@RunWith(RobolectricTestRunner::class)
class ExampleScenesTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun everySceneHasItsPictureAndItsSpotsSitInsideIt() {
        TestBook.store(tmp.root)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        for (sc in ExampleScenes.all) {
            val bytes = context.assets.open("Scenes/${sc.key}.webp").use { it.readBytes() }
            assertTrue("${sc.key} picture", bytes.size > 10_000)
            for (s in sc.spots) {
                assertTrue("${sc.key}/${s.label} inside", s.x >= 0 && s.y >= 0 && s.x + s.w <= 100.0 && s.y + s.h <= 100.0)
                assertTrue("${sc.key}/${s.label} big enough to tap", s.w >= 6.0 && s.h >= 6.0)
            }
        }
    }

    @Test
    fun everySpotSpeaksInBellasVoice() {
        TestBook.store(tmp.root)
        for (sc in ExampleScenes.all) for (s in sc.spots) {
            if (s.opensPage != null) continue
            val recorded = VoiceClips.clip(s.says, SpokenText.BELLA_VOICE_ID) != null || VoiceClips.chain(s.says, SpokenText.BELLA_VOICE_ID) != null
            assertTrue("\"${s.says}\" (${sc.key}) has a recorded clip", recorded)
        }
    }

    @Test
    fun theStarterBookShowsTwoScenesAndTheFridgeOpensFood() {
        val pages = AACStore.defaultPages()
        val kitchen = pages.first { it.scenePresetKey == "kitchen" }
        assertNotNull(pages.firstOrNull { it.scenePresetKey == "playground" })
        val fridge = kitchen.hotspots.first { it.label == "Fridge" }
        assertEquals(HotspotAction.JUMP, fridge.action)
        assertEquals(pages.first { it.title == "Food & Snacks" }.id, fridge.jumpPageId)
        assertTrue(kitchen.hotspots.filter { it.label != "Fridge" }.all { it.action == HotspotAction.TTS && it.style == HotspotStyle.HIGHLIGHT })
    }

    @Test
    fun exampleScenesDoNotUseUpTheFreeScene() {
        val pages = AACStore.defaultPages()
        assertEquals(0, ProRules.scenes(pages))
        assertEquals(1, ProRules.scenes(pages + PageModel(type = PageType.SCENE, sceneImageData = byteArrayOf(1))))
    }

    @Test
    fun theWholePictureIsFittedInEitherOrientation() {
        // A 4:3 picture on a portrait tablet: full width, centred vertically.
        val p = ScenePictures.fittedRect(1344, 1008, 800f, 1100f)
        assertEquals(0f, p[0], 0.5f); assertEquals(800f, p[2], 0.5f); assertEquals(600f, p[3], 0.5f); assertEquals(250f, p[1], 0.5f)
        // Landscape: full height, centred across.
        val l = ScenePictures.fittedRect(1344, 1008, 1280f, 640f)
        assertEquals(640f, l[3], 0.5f); assertEquals(853.3f, l[2], 0.5f); assertEquals((1280f - 853.3f) / 2, l[0], 0.5f)
        // No picture: the whole box.
        val n = ScenePictures.fittedRect(null, null, 500f, 400f)
        assertEquals(listOf(0f, 0f, 500f, 400f), n.toList())
    }
}
