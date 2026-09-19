package com.talktiles.tablet

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The saved-book format is the iPad's; these pin the shapes both apps must read. */
class ModelsSerializationTest {

    @Test
    fun tilesRoundTripAsFlatIntKeyedArray() {
        val page = PageModel(id = "P1", title = "Core", gridSize = 4, tiles = mapOf(
            2 to TileModel(id = 2, label = "Eat", tts = "Eat food"),
            1 to TileModel(id = 1, label = "I want")
        ))
        val json = AppJson.encodeToString(PageModel.serializer(), page)
        // Swift writes [key, value, key, value]; keys sorted so the file is stable.
        assertTrue(json, json.contains("\"tiles\":[1,{"))
        val back = AppJson.decodeFromString(PageModel.serializer(), json)
        assertEquals(page, back)
        assertEquals(listOf(1, 2), back.tiles.keys.toList())
    }

    @Test
    fun tilesAlsoLoadFromAnObjectKeyedByStrings() {
        val json = """{"id":"P","title":"T","tiles":{"3":{"id":3,"label":"Go"}}}"""
        val back = AppJson.decodeFromString(PageModel.serializer(), json)
        assertEquals("Go", back.tiles[3]?.label)
    }

    @Test
    fun byteArraysAreBase64AndTolerateLineBreaks() {
        val bytes = ByteArray(300) { (it % 251).toByte() }
        val tile = TileModel(id = 1, label = "Photo", photoData = bytes, audioData = byteArrayOf(1, 2, 3))
        val json = AppJson.encodeToString(TileModel.serializer(), tile)
        assertTrue(json.contains("\"photoData\":\""))
        val back = AppJson.decodeFromString(TileModel.serializer(), json)
        assertArrayEquals(bytes, back.photoData)
        assertArrayEquals(byteArrayOf(1, 2, 3), back.audioData)

        // A mailed file may have been wrapped at 76 columns.
        val wrapped = json.replace("\"photoData\":\"", "\"photoData\":\"\\r\\n").replace("AAEC", "AA\\r\\nEC")
        val fromWrapped = AppJson.decodeFromString(TileModel.serializer(), wrapped)
        assertArrayEquals(bytes, fromWrapped.photoData)
    }

    @Test
    fun nullByteArraysAreOmittedAndReadBackAsNull() {
        val json = AppJson.encodeToString(TileModel.serializer(), TileModel(id = 1, label = "x"))
        assertTrue(json, !json.contains("photoData"))
        val back = AppJson.decodeFromString(TileModel.serializer(), json)
        assertNull(back.photoData)
        assertNull(back.audioData)
    }

    @Test
    fun settingsFileWithoutVoiceKeyGetsBellaAndExplicitNullMeansDevice() {
        val old = AppJson.decodeFromString(AppSettings.serializer(), """{"speechRate":0.5,"childLock":true}""")
        assertEquals(SpokenText.BELLA_VOICE_ID, old.voiceId)
        assertEquals(0.5, old.speechRate, 0.0)
        assertTrue(old.childLock)

        val device = AppJson.decodeFromString(AppSettings.serializer(), """{"voiceId":null}""")
        assertNull(device.voiceId)

        val written = SettingsJson.encodeToString(AppSettings.serializer(), AppSettings(voiceId = null))
        assertTrue(written, written.contains("\"voiceId\":null"))
    }

    @Test
    fun unknownKeysFromANewerBuildAreIgnored() {
        val s = AppJson.decodeFromString(AppSettings.serializer(), """{"voiceId":null,"futureThing":42}""")
        assertNull(s.voiceId)
        val p = AppJson.decodeFromString(PageModel.serializer(), """{"id":"Q","title":"T","somethingNew":{"a":1}}""")
        assertEquals("Q", p.id)
    }

    @Test
    fun enumsUseTheiPadRawStrings() {
        val p = PageModel(id = "S", type = PageType.SCENE, hotspots = listOf(HotspotModel(id = 1, style = HotspotStyle.HIGHLIGHT, action = HotspotAction.JUMP, jumpPageId = "X")))
        val json = AppJson.encodeToString(PageModel.serializer(), p)
        assertTrue(json, json.contains("\"type\":\"Visual Scene Display\""))
        assertTrue(json, json.contains("\"style\":\"Yellow Glow\""))
        assertTrue(json, json.contains("\"action\":\"Jump to Page\""))
        assertEquals(p, AppJson.decodeFromString(PageModel.serializer(), json))
    }
}
