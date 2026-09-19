package com.talktiles.tablet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** When a press counts, for every combination of "hold to speak" and "speak when the finger lifts". */
class PressControllerTest {

    private class Probe(delay: Double, onRelease: Boolean) {
        var fired = 0
        val c = PressController(activationDelay = delay, activateOnRelease = onRelease) { fired++ }
    }

    @Test
    fun plainTapFiresOnPressOnce() {
        val p = Probe(0.0, false)
        assertNull(p.c.press())
        assertEquals(1, p.fired)
        p.c.release(completed = true)
        assertEquals(1, p.fired)
    }

    @Test
    fun speakOnLiftFiresOnlyOnACompletedRelease() {
        val p = Probe(0.0, true)
        assertNull(p.c.press())
        assertEquals(0, p.fired)
        p.c.release(completed = true)
        assertEquals(1, p.fired)
    }

    @Test
    fun aCancelledReleaseNeverFires() {
        val p = Probe(0.0, true)
        p.c.press()
        p.c.release(completed = false)         // finger slid off / the gesture was taken by a scroll
        assertEquals(0, p.fired)

        val q = Probe(0.8, true)
        assertEquals(800L, q.c.press())
        q.c.dwellElapsed()
        q.c.release(completed = false)
        assertEquals(0, q.fired)
    }

    @Test
    fun holdToSpeakFiresWhenTheDwellElapsesAndLiftingEarlyCancels() {
        val p = Probe(0.5, false)
        assertEquals(500L, p.c.press())
        assertTrue(p.c.isPressed)
        p.c.release(completed = true)          // lifted before the dwell
        assertEquals(0, p.fired)
        assertFalse(p.c.isPressed)
        p.c.dwellElapsed()                     // a late timer must not fire either
        assertEquals(0, p.fired)

        p.c.press()
        p.c.dwellElapsed()
        assertEquals(1, p.fired)
        p.c.release(completed = true)
        assertEquals(1, p.fired)
    }

    @Test
    fun holdThenLiftNeedsBothTheDwellAndACompletedRelease() {
        val p = Probe(0.5, true)
        p.c.press()
        p.c.dwellElapsed()
        assertEquals("armed, not fired", 0, p.fired)
        assertTrue(p.c.isArmed)
        p.c.release(completed = true)
        assertEquals(1, p.fired)

        p.c.press()
        p.c.release(completed = true)          // too quick
        assertEquals(1, p.fired)
    }

    @Test
    fun onlyOneFirePerGestureEvenIfEventsRepeat() {
        val p = Probe(0.0, false)
        p.c.press(); p.c.press()
        p.c.release(completed = true); p.c.release(completed = true)
        assertEquals(1, p.fired)
    }

    @Test
    fun aSemanticActivationIsOneFireAndDoesNotDisturbAGesture() {
        val p = Probe(0.7, true)
        p.c.activate()
        assertEquals(1, p.fired)
        assertFalse(p.c.isPressed)
    }

    @Test
    fun dwellProgressIsKnownForTheRing() {
        val p = Probe(1.0, false)
        p.c.press()
        assertEquals(1000L, p.c.dwellMs)
        p.c.release(completed = true)
        assertEquals(0L, Probe(0.0, false).c.dwellMs)
    }
}
