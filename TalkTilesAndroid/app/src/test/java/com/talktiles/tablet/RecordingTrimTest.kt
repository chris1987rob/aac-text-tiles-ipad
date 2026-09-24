package com.talktiles.tablet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

/** The silence before a recorded word is what made a tap feel late. */
class RecordingTrimTest {
    private val rate = 16_000

    /** `lead` s of room hiss, `speech` s of a 220 Hz voice-loud tone, `tail` s of hiss. */
    private fun clip(lead: Double, speech: Double, tail: Double, hiss: Int = 60, loud: Int = 8000): ShortArray {
        val n1 = (lead * rate).toInt(); val n2 = (speech * rate).toInt(); val n3 = (tail * rate).toInt()
        val r = java.util.Random(7)
        return ShortArray(n1 + n2 + n3) { i ->
            if (i in n1 until n1 + n2) (loud * sin(2 * PI * 220 * i / rate)).toInt().toShort()
            else (r.nextGaussian() * hiss).toInt().toShort()
        }
    }

    @Test
    fun theSecondOfSilenceBeforeTheWordIsCut() {
        // Like the "CJ" button on the tablet: 0.85 s of nothing, then the name.
        val (start, end) = RecordingTrim.speechBounds(clip(0.85, 0.8, 0.5), rate)!!
        assertEquals((850_000L - RecordingTrim.PRE_ROLL_MS * 1000L).toDouble(), start.toDouble(), 10_000.0)
        assertEquals((1_650_000L + RecordingTrim.POST_ROLL_MS * 1000L).toDouble(), end.toDouble(), 10_000.0)
    }

    @Test
    fun aRecordingThatStartsRightAwayKeepsItsStart() {
        val (start, _) = RecordingTrim.speechBounds(clip(0.0, 1.0, 0.2), rate)!!
        assertEquals(0L, start)
    }

    @Test
    fun aQuietVoiceIsStillFound() {
        val (start, _) = RecordingTrim.speechBounds(clip(0.5, 0.6, 0.3, hiss = 30, loud = 1200), rate)!!
        assertTrue(start in 400_000L..500_000L)
    }

    @Test
    fun onlyRoomNoiseIsLeftAlone() {
        assertNull(RecordingTrim.speechBounds(clip(2.0, 0.0, 0.0), rate))
    }

    @Test
    fun theStarterColoursAllHavePictures() {
        val colors = AACStore.defaultPages().first { it.title == "Colors" }
        assertTrue(colors.tiles.values.all { it.symbolName?.startsWith("tt:") == true })
    }
}
