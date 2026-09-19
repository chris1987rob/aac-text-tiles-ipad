package com.talktiles.tablet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The speech coordinator, driven by a fake backend: what is queued, what
 * supersedes what, and that nothing stale ever comes back to life.
 */
class SpeechManagerTest {

    /** Records every unit it was asked to start; the test decides when each finishes. */
    private class FakeBackend(override var isTtsReady: Boolean = true) : SpeechBackend {
        val started = ArrayList<Pair<SpeechUnit, Long>>()
        var stops = 0
        var listener: SpeechBackend.Listener? = null
        var refuse = false
        override fun start(unit: SpeechUnit, token: Long, listener: SpeechBackend.Listener): Boolean {
            this.listener = listener
            if (refuse) return false
            started.add(unit to token)
            return true
        }
        override fun stop() { stops++ }
        override fun systemVoices(): List<SpeechManager.SystemVoice> = emptyList()
        fun finishLast() { val (_, token) = started.last(); listener!!.onUnitFinished(token) }
        fun failLast() { val (_, token) = started.last(); listener!!.onUnitFailed(token) }
    }

    private class FakeScheduler {
        val pending = ArrayList<Pair<Long, () -> Unit>>()
        val cancelled = ArrayList<Int>()
        fun schedule(ms: Long, action: () -> Unit): () -> Unit {
            val entry = ms to action
            pending.add(entry)
            return { cancelled.add(pending.indexOf(entry)) }
        }
        fun fireAll() { val run = pending.toList(); pending.clear(); run.forEach { it.second() } }
    }

    private fun manager(backend: FakeBackend, scheduler: FakeScheduler = FakeScheduler(), clips: (String, String?) -> List<String>? = { _, _ -> null }) =
        SpeechManager(backend, mainThread = { it() }, schedule = scheduler::schedule, clipsFor = clips)

    @Test
    fun isSpeakingFollowsTheBackendsCompletionNotATimer() {
        val b = FakeBackend()
        val m = manager(b)
        m.preferredVoiceId = null
        m.speak("Hello there")
        assertTrue(m.isSpeaking)
        assertEquals(SpeechUnit.Text("Hello there", 0.45f, null), b.started.single().first)
        b.finishLast()
        assertFalse(m.isSpeaking)
    }

    @Test
    fun stopSilencesTheBackendAndIgnoresTheStaleCallback() {
        val b = FakeBackend()
        val m = manager(b)
        m.preferredVoiceId = null
        m.speak("A long sentence")
        val before = b.stops
        m.stop()
        assertEquals(before + 1, b.stops)
        assertFalse(m.isSpeaking)
        b.finishLast()   // arrives late from the engine
        assertFalse(m.isSpeaking)
        assertEquals(1, b.started.size)
    }

    @Test
    fun aNewerRequestSupersedesAnOlderOneAndItsQueue() {
        val b = FakeBackend()
        val m = manager(b)
        m.preferredVoiceId = null
        m.speakItems(listOf(SentenceItem("I", "I"), SentenceItem("want", "want")), 0.45f, null)
        assertEquals(1, b.started.size)              // second item waits for the first to finish
        m.speak("Stop")
        assertEquals(2, b.started.size)
        assertEquals(SpeechUnit.Text("Stop", 0.45f, null), b.started[1].first)
        b.listener!!.onUnitFinished(b.started[0].second)   // the old sentence's first item finishes late
        assertEquals("the old queue must not continue", 2, b.started.size)
        assertTrue(m.isSpeaking)
        b.finishLast()
        assertFalse(m.isSpeaking)
    }

    @Test
    fun sentenceItemsPlayInOrderAndRecordingsAreTheirOwnVoice() {
        val b = FakeBackend()
        val m = manager(b)
        val items = listOf(SentenceItem("I want", "I want"), SentenceItem("Mum", "Mum", audioData = byteArrayOf(7)), SentenceItem("Eat", "Eat food"))
        m.speakItems(items, 0.6f, "en-us-x-abc")
        assertEquals(SpeechUnit.Text("I want", 0.6f, "en-us-x-abc"), b.started[0].first)
        b.finishLast()
        assertTrue(b.started[1].first is SpeechUnit.Audio)
        b.finishLast()
        assertEquals(SpeechUnit.Text("Eat food", 0.6f, "en-us-x-abc"), b.started[2].first)
        b.finishLast()
        assertFalse(m.isSpeaking)
        assertEquals(3, b.started.size)
    }

    @Test
    fun bellaClipsAreUsedWhenTheVoiceHasThemAndTtsOtherwise() {
        val b = FakeBackend()
        val m = manager(b, clips = { text, voice -> if (voice == SpokenText.BELLA_VOICE_ID && text == "I want") listOf("Voices/bella/i.mp3", "Voices/bella/want.mp3") else null })
        m.speakItems(listOf(SentenceItem("I want", "I want"), SentenceItem("Zebra", "Zebra")), 0.45f, SpokenText.BELLA_VOICE_ID)
        assertEquals(SpeechUnit.Clip("Voices/bella/i.mp3", 1.0f), b.started[0].first)
        b.finishLast()
        assertEquals(SpeechUnit.Clip("Voices/bella/want.mp3", 1.0f), b.started[1].first)
        b.finishLast()
        assertEquals(SpeechUnit.Text("Zebra", 0.45f, SpokenText.BELLA_VOICE_ID), b.started[2].first)
    }

    @Test
    fun soundItOutPausesBetweenSyllablesAndStopCancelsThePendingOnes() {
        val b = FakeBackend()
        val s = FakeScheduler()
        val m = manager(b, s)
        m.preferredVoiceId = null
        m.soundItOut("water")
        assertEquals("wa", (b.started[0].first as SpeechUnit.Text).text)
        b.finishLast()
        assertEquals(1, s.pending.size)      // the gap before "ter"
        m.stop()
        assertEquals(1, s.cancelled.size)
        s.fireAll()                          // even if the timer fired anyway...
        assertEquals("nothing after Stop", 1, b.started.size)
        assertFalse(m.isSpeaking)
    }

    @Test
    fun soundItOutSaysEverySyllableThenTheWholeWord() {
        val b = FakeBackend()
        val s = FakeScheduler()
        val m = manager(b, s)
        m.preferredVoiceId = null
        m.soundItOut("happy")
        val said = ArrayList<String>()
        repeat(6) {
            said.add((b.started.last().first as SpeechUnit.Text).text)
            b.finishLast()
            s.fireAll()
        }
        assertEquals(listOf("hap", "py", "happy"), said.take(3))
        assertFalse(m.isSpeaking)
    }

    @Test
    fun aPhraseAskedForBeforeTheEngineIsReadyIsSpokenOnceItIs() {
        val b = FakeBackend(isTtsReady = false)
        val m = manager(b)
        m.preferredVoiceId = null
        m.speak("Hello")
        assertTrue("shows as busy while waiting", m.isSpeaking)
        assertTrue(b.started.isEmpty())
        b.isTtsReady = true
        m.onEngineReady(true)
        assertEquals(SpeechUnit.Text("Hello", 0.45f, null), b.started.single().first)
    }

    @Test
    fun onlyTheLatestPhraseWaitsForTheEngine() {
        val b = FakeBackend(isTtsReady = false)
        val m = manager(b)
        m.preferredVoiceId = null
        m.speak("First"); m.speak("Second")
        b.isTtsReady = true
        m.onEngineReady(true)
        assertEquals(listOf("Second"), b.started.map { (it.first as SpeechUnit.Text).text })
    }

    @Test
    fun anEngineThatNeverComesUpIsReportedNotSilentlyDropped() {
        val b = FakeBackend(isTtsReady = false)
        val m = manager(b)
        m.preferredVoiceId = null
        assertNull(m.lastProblem)
        m.speak("Hello")
        m.onEngineReady(false)
        assertFalse(m.isSpeaking)
        assertNotNull(m.lastProblem)
        assertTrue(b.started.isEmpty())
        // Recordings and clips do not need the engine and still play.
        m.playAudioData(byteArrayOf(1))
        assertTrue(b.started.single().first is SpeechUnit.Audio)
    }

    @Test
    fun aUnitThatFailsToStartOrErrorsMovesOnToTheNext() {
        val b = FakeBackend()
        val m = manager(b)
        m.preferredVoiceId = null
        m.speakItems(listOf(SentenceItem("A", "A"), SentenceItem("B", "B"), SentenceItem("C", "C")), 0.45f, null)
        b.failLast()
        assertEquals("B", (b.started[1].first as SpeechUnit.Text).text)
        b.refuse = true
        b.finishLast()                       // C cannot even start
        assertFalse(m.isSpeaking)
        assertEquals(2, b.started.size)
    }

    @Test
    fun blankTextAndEmptySentencesDoNothing() {
        val b = FakeBackend()
        val m = manager(b)
        m.speak("   ")
        m.speakItems(emptyList(), 0.45f, null)
        assertTrue(b.started.isEmpty())
        assertFalse(m.isSpeaking)
    }

    @Test
    fun clipRateBandsAndTtsRateAreUnchangedFromV12() {
        assertEquals(0.8f, SpeechManager.clipRate(0.3f), 0f)
        assertEquals(1.0f, SpeechManager.clipRate(0.45f), 0f)
        assertEquals(1.25f, SpeechManager.clipRate(0.7f), 0f)
        assertEquals(1.0f, SpeechManager.ttsRate(0.45f), 0.001f)
    }
}
