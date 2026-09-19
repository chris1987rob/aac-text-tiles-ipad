package com.talktiles.tablet

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import java.io.File
import java.util.Locale

/** Turning what a button *says* into what the synthesiser is *given*. */
object SpokenText {

    /** The `AppSettings.voiceId` that means Bella, the app's own recorded voice. */
    const val BELLA_VOICE_ID = "talktiles:bella"

    /** The folder under `Voices/` for a recorded voice id, null for a system voice. */
    fun recordedVoiceFolder(voiceId: String?): String? {
        if (voiceId == null || !voiceId.startsWith("talktiles:")) return null
        val name = voiceId.removePrefix("talktiles:")
        return name.ifEmpty { null }
    }

    /**
     * A lone capital letter would be read as its NAME ("capital I"); lowercasing
     * a single character is enough. Anything longer reaches the voice as typed.
     */
    fun forSpeech(text: String): String {
        val trimmed = text.trim()
        return if (trimmed.length == 1 && trimmed[0].isUpperCase()) trimmed.lowercase() else text
    }

    /**
     * The key a phrase is looked up by in a voice's clip index: lowercase,
     * letters, digits and apostrophes kept, everything else collapsed to one
     * space. Tools/sync-native-assets.py writes the index with the same rule.
     */
    fun normalisedPhrase(text: String): String {
        val out = StringBuilder()
        var pendingSpace = false
        val lower = text.lowercase()
        var i = 0
        while (i < lower.length) {
            val cp = lower.codePointAt(i)
            i += Character.charCount(cp)
            val keep = Character.isLetter(cp) || Character.isDigit(cp) || cp == '\''.code || cp == 0x2019
            if (keep) {
                if (pendingSpace && out.isNotEmpty()) out.append(' ')
                pendingSpace = false
                out.appendCodePoint(if (cp == 0x2019) '\''.code else cp)
            } else {
                pendingSpace = true
            }
        }
        return out.toString()
    }
}

/** Pre-recorded clips for the app's own voices, in assets under `Voices/<voice>/`. */
object VoiceClips {

    data class Voice(val id: String, val folder: String, val name: String, val description: String)

    @Serializable
    private class Index(val voice: String, val name: String, val description: String? = null,
                        val clips: Map<String, String>)

    private lateinit var appContext: Context
    private val loaded = HashMap<String, Index?>()

    fun init(context: Context) { appContext = context.applicationContext }

    private fun index(folder: String): Index? {
        if (loaded.containsKey(folder)) return loaded[folder]
        val result = try {
            appContext.assets.open("Voices/$folder/index.json").bufferedReader().use {
                AppJson.decodeFromString(Index.serializer(), it.readText())
            }
        } catch (e: Exception) { null }
        loaded[folder] = result
        return result
    }

    /** Every recorded voice actually present in this build, Bella first. */
    val available: List<Voice> by lazy {
        val names = try { appContext.assets.list("Voices")?.toList() ?: emptyList() } catch (e: Exception) { emptyList() }
        names.sorted().mapNotNull { folder ->
            val idx = index(folder) ?: return@mapNotNull null
            if (idx.clips.isEmpty()) return@mapNotNull null
            Voice("talktiles:$folder", folder, idx.name, idx.description ?: "")
        }.sortedWith(compareBy({ it.id != SpokenText.BELLA_VOICE_ID }, { it.name }))
    }

    val isAvailable: Boolean get() = available.isNotEmpty()

    fun voice(forId: String?): Voice? {
        val folder = SpokenText.recordedVoiceFolder(forId) ?: return null
        return available.firstOrNull { it.folder == folder }
    }

    fun isRecordedVoice(voiceId: String?): Boolean = SpokenText.recordedVoiceFolder(voiceId) != null

    /** The one clip that says exactly this text, as an asset path. */
    fun clip(text: String, voiceId: String?): String? {
        val folder = SpokenText.recordedVoiceFolder(voiceId) ?: return null
        val idx = index(folder) ?: return null
        val key = SpokenText.normalisedPhrase(text)
        if (key.isEmpty()) return null
        val file = idx.clips[key] ?: return null
        return "Voices/$folder/$file"
    }

    /** The word clips in order, only when every word has one. */
    fun chain(text: String, voiceId: String?): List<String>? {
        val folder = SpokenText.recordedVoiceFolder(voiceId) ?: return null
        val idx = index(folder) ?: return null
        val words = SpokenText.normalisedPhrase(text).split(' ').filter { it.isNotEmpty() }
        if (words.size < 2) return null
        return words.map { w -> idx.clips[w]?.let { "Voices/$folder/$it" } ?: return null }
    }

    fun previewClips(voiceId: String?): List<String> {
        val one = clip("Hello", voiceId) ?: return emptyList()
        return listOf(one) + (chain("I want more please", voiceId) ?: emptyList())
    }
}

/** One thing to play, in order. A request is a list of these. */
sealed class SpeechUnit {
    data class Text(val text: String, val rate: Float, val voiceId: String?) : SpeechUnit()
    data class Clip(val asset: String, val rate: Float) : SpeechUnit()
    class Audio(val bytes: ByteArray) : SpeechUnit()
    data class Pause(val ms: Long) : SpeechUnit()
}

/** What actually makes sound - the device engine and media players - behind an interface the coordinator can be tested against. */
interface SpeechBackend {
    interface Listener {
        fun onUnitFinished(token: Long)
        fun onUnitFailed(token: Long)
    }
    val isTtsReady: Boolean
    /** Starts one unit and later reports `token` to the listener. False if it could not start at all. */
    fun start(unit: SpeechUnit, token: Long, listener: Listener): Boolean
    fun stop()
    fun systemVoices(): List<SpeechManager.SystemVoice>
}

/**
 * Everything that makes a sound: Bella's clips, the device's text-to-speech,
 * and playback of a recording made on a button. One request at a time - a new
 * press silences whatever was still speaking, and a callback from anything
 * older than the current request is ignored (the request-generation guard).
 *
 * Runs on the main thread: the backend's callbacks are handed to `mainThread`
 * first. Nothing here logs what was said.
 */
class SpeechManager(
    private val backend: SpeechBackend,
    private val mainThread: (() -> Unit) -> Unit,
    /** Schedules `action` after `ms`; returns a cancel. */
    private val schedule: (Long, () -> Unit) -> (() -> Unit),
    /** The recorded clips that say `text` in `voiceId`, or null to use the engine. */
    private val clipsFor: (String, String?) -> List<String>? = { text, voice -> VoiceClips.clip(text, voice)?.let { listOf(it) } ?: VoiceClips.chain(text, voice) }
) : SpeechBackend.Listener {

    companion object {
        @Volatile private var instance: SpeechManager? = null
        fun init(context: Context): SpeechManager =
            instance ?: synchronized(this) {
                instance ?: run {
                    val handler = Handler(Looper.getMainLooper())
                    val android = AndroidSpeechBackend(context.applicationContext)
                    SpeechManager(
                        backend = android,
                        mainThread = { block -> if (Looper.myLooper() == Looper.getMainLooper()) block() else handler.post(block) },
                        schedule = { ms, action -> val r = Runnable(action); handler.postDelayed(r, ms); { handler.removeCallbacks(r) } }
                    ).also { m -> android.onReady = { ok -> m.onEngineReady(ok) }; instance = m }
                }
            }
        val shared: SpeechManager get() = instance ?: error("SpeechManager.init(context) first")

        /**
         * The speed slider is the iPad's AVSpeechUtterance rate (0.3-0.7, 0.45
         * normal). A recording takes it as three bands: Slow / Normal / Fast.
         */
        fun clipRate(speechRate: Float): Float = when {
            speechRate < 0.38f -> 0.8f
            speechRate < 0.52f -> 1.0f
            else -> 1.25f
        }

        /** The same slider, as an Android TTS speech rate (1.0 normal). */
        fun ttsRate(speechRate: Float): Float = (speechRate / 0.45f).coerceIn(0.5f, 1.8f)

        const val SYLLABLE_GAP_MS = 350L
    }

    /** A device voice, with a name a person can read - the engine's own are like "en-au-x-aua-local". */
    data class SystemVoice(val id: String, val title: String)

    var isSpeaking by mutableStateOf(false)
        private set

    /** Set when the device has no working text-to-speech; the UI shows it instead of staying silent. */
    var lastProblem by mutableStateOf<String?>(null)
        private set

    /** The voice every utterance uses unless a caller passes one. */
    var preferredVoiceId: String? = SpokenText.BELLA_VOICE_ID
    var defaultRate: Float = 0.45f

    private var generation = 0L
    private var queue: ArrayDeque<SpeechUnit> = ArrayDeque()
    private var cancelPause: (() -> Unit)? = null
    private var waitingForEngine: List<SpeechUnit>? = null
    private var engineFailed = false

    fun availableSystemVoices(): List<SystemVoice> = backend.systemVoices()

    // MARK: - Requests

    fun speak(text: String, rate: Float? = null, voiceId: String? = null) {
        if (text.isBlank()) return
        val r = rate ?: defaultRate
        request(unitsFor(text, r, voiceId ?: preferredVoiceId))
    }

    /** The sentence bar: each item in order, a recording playing as itself. */
    fun speakItems(items: List<SentenceItem>, rate: Float? = null, voiceId: String? = null) {
        val r = rate ?: defaultRate
        val voice = voiceId ?: preferredVoiceId
        val units = items.flatMap { item ->
            val audio = item.audioData
            when {
                audio != null -> listOf(SpeechUnit.Audio(audio))
                item.spoken.isBlank() -> emptyList()
                else -> unitsFor(item.spoken, r, voice)
            }
        }
        if (units.isNotEmpty()) request(units)
    }

    /** Sound It Out: syllables with a breath between, then the word. */
    fun soundItOut(word: String) {
        val syllables = SyllableHelper.split(word)
        if (syllables.isEmpty()) { speak(word); return }
        val voice = preferredVoiceId
        val units = ArrayList<SpeechUnit>()
        for (s in syllables) { units.add(SpeechUnit.Text(s, 0.45f, voice)); units.add(SpeechUnit.Pause(SYLLABLE_GAP_MS)) }
        units.addAll(unitsFor(word, 0.52f, voice))
        request(units)
    }

    fun playAudioData(data: ByteArray) = request(listOf(SpeechUnit.Audio(data)))

    /** Plays asset clips back to back. Public so Settings can preview a voice. */
    fun playClipSequence(assets: List<String>, rate: Float? = null): Boolean {
        if (assets.isEmpty()) return false
        val cr = clipRate(rate ?: defaultRate)
        request(assets.map { SpeechUnit.Clip(it, cr) })
        return true
    }

    /** Silence, now. Anything queued or waiting is forgotten. */
    fun stop() {
        generation += 1
        cancelPause?.invoke(); cancelPause = null
        queue.clear()
        waitingForEngine = null
        backend.stop()
        isSpeaking = false
    }

    // MARK: - Engine

    /** The backend reports the engine coming up (or not). */
    fun onEngineReady(ok: Boolean) = mainThread {
        engineFailed = !ok
        val waiting = waitingForEngine ?: return@mainThread
        waitingForEngine = null
        if (ok) {
            startNext(generation)
        } else {
            queue.clear()
            isSpeaking = false
            lastProblem = "No text-to-speech voice is available on this device. Bella's recorded words and your own recordings still play."
        }
    }

    // MARK: - Internals

    private fun unitsFor(text: String, rate: Float, voiceId: String?): List<SpeechUnit> {
        if (VoiceClips.isRecordedVoice(voiceId)) {
            clipsFor(text, voiceId)?.let { clips -> return clips.map { SpeechUnit.Clip(it, clipRate(rate)) } }
        }
        return listOf(SpeechUnit.Text(text, rate, voiceId))
    }

    private fun request(units: List<SpeechUnit>) {
        stop()
        val gen = generation
        queue = ArrayDeque(units)
        val needsEngine = units.any { it is SpeechUnit.Text }
        if (needsEngine && !backend.isTtsReady) {
            if (engineFailed) {
                lastProblem = "No text-to-speech voice is available on this device. Bella's recorded words and your own recordings still play."
                // Anything that does not need the engine still plays.
                queue = ArrayDeque(units.filter { it !is SpeechUnit.Text })
                if (queue.isEmpty()) { isSpeaking = false; return }
            } else {
                waitingForEngine = units
                isSpeaking = true
                return
            }
        }
        isSpeaking = true
        startNext(gen)
    }

    private fun startNext(gen: Long) {
        if (gen != generation) return
        while (true) {
            val unit = queue.removeFirstOrNull()
            if (unit == null) { isSpeaking = false; return }
            if (unit is SpeechUnit.Pause) {
                cancelPause = schedule(unit.ms) { mainThread { if (gen == generation) { cancelPause = null; startNext(gen) } } }
                return
            }
            if (backend.start(unit, gen, this)) return
            // Could not start: move on to the next unit rather than sit silent.
        }
    }

    override fun onUnitFinished(token: Long) = mainThread { if (token == generation) startNext(token) }
    override fun onUnitFailed(token: Long) = mainThread { if (token == generation) startNext(token) }
}

/** The Android engine and players behind `SpeechBackend`. */
class AndroidSpeechBackend(private val app: Context) : SpeechBackend {
    private var tts: TextToSpeech? = null
    @Volatile override var isTtsReady = false
        private set
    var onReady: ((Boolean) -> Unit)? = null
    private var player: MediaPlayer? = null
    private var listener: SpeechBackend.Listener? = null

    init {
        tts = TextToSpeech(app) { status ->
            isTtsReady = status == TextToSpeech.SUCCESS
            if (isTtsReady) try { tts?.language = Locale.US } catch (e: Exception) { }
            onReady?.invoke(isTtsReady)
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { token(utteranceId)?.let { listener?.onUnitFinished(it) } }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { token(utteranceId)?.let { listener?.onUnitFailed(it) } }
            override fun onError(utteranceId: String?, errorCode: Int) { token(utteranceId)?.let { listener?.onUnitFailed(it) } }
            override fun onStop(utteranceId: String?, interrupted: Boolean) { /* stopped by us: the coordinator already moved on */ }
        })
    }

    private fun token(utteranceId: String?): Long? = utteranceId?.removePrefix("tt-")?.toLongOrNull()

    override fun start(unit: SpeechUnit, token: Long, listener: SpeechBackend.Listener): Boolean {
        this.listener = listener
        return when (unit) {
            is SpeechUnit.Text -> speakWithEngine(unit, token)
            is SpeechUnit.Clip -> playClip(unit, token)
            is SpeechUnit.Audio -> playBytes(unit.bytes, token)
            is SpeechUnit.Pause -> false
        }
    }

    private fun speakWithEngine(unit: SpeechUnit.Text, token: Long): Boolean {
        val t = tts ?: return false
        if (!isTtsReady) return false
        return try {
            val voiceId = unit.voiceId
            if (voiceId != null && !VoiceClips.isRecordedVoice(voiceId)) {
                val match = try { t.voices?.firstOrNull { it.name == voiceId } } catch (e: Exception) { null }
                if (match != null) t.voice = match else t.language = Locale.US
            } else {
                t.language = Locale.US
            }
            t.setSpeechRate(SpeechManager.ttsRate(unit.rate))
            t.speak(SpokenText.forSpeech(unit.text), TextToSpeech.QUEUE_FLUSH, null, "tt-$token") == TextToSpeech.SUCCESS
        } catch (e: Exception) { false }
    }

    private fun playClip(unit: SpeechUnit.Clip, token: Long): Boolean = startPlayer(token) { p ->
        app.assets.openFd(unit.asset).use { fd -> p.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length) }
        p.prepare()
        if (unit.rate != 1.0f) {
            try { p.playbackParams = p.playbackParams.setSpeed(unit.rate) } catch (e: Exception) { }
        }
    }

    private fun playBytes(bytes: ByteArray, token: Long): Boolean {
        // MediaPlayer wants a file; the temp file is deleted as soon as the player has it open.
        val f = try { File.createTempFile("rec", ".m4a", app.cacheDir).also { it.writeBytes(bytes) } } catch (e: Exception) { return false }
        val ok = startPlayer(token) { p ->
            java.io.FileInputStream(f).use { p.setDataSource(it.fd) }
            p.prepare()
        }
        f.delete()
        return ok
    }

    private fun startPlayer(token: Long, prepare: (MediaPlayer) -> Unit): Boolean {
        releasePlayer()
        val p = MediaPlayer()
        return try {
            p.setAudioAttributes(attrs())
            prepare(p)
            p.setOnCompletionListener { done ->
                if (player === done) { player = null; done.release(); listener?.onUnitFinished(token) }
                else done.release()
            }
            p.setOnErrorListener { bad, _, _ ->
                if (player === bad) { player = null; listener?.onUnitFailed(token) }
                bad.release()
                true
            }
            player = p
            p.start()
            true
        } catch (e: Exception) {
            Log.w("TalkTiles", "playback failed: ${e.javaClass.simpleName}")
            try { p.release() } catch (e2: Exception) { }
            if (player === p) player = null
            false
        }
    }

    private fun releasePlayer() {
        val p = player ?: return
        player = null
        try { if (p.isPlaying) p.stop() } catch (e: Exception) { }
        try { p.release() } catch (e: Exception) { }
    }

    override fun stop() {
        releasePlayer()
        try { tts?.stop() } catch (e: Exception) { }
    }

    /** Every offline English voice the device's engine offers, the device's own region first. */
    override fun systemVoices(): List<SpeechManager.SystemVoice> {
        val t = tts ?: return emptyList()
        if (!isTtsReady) return emptyList()
        return try {
            val here = Locale.getDefault()
            val voices = t.voices.filter { it.locale.language == "en" && !it.isNetworkConnectionRequired }
                .sortedWith(compareBy({ it.locale.country != here.country }, { it.locale.toString() }, { it.name }))
            val counts = HashMap<String, Int>()
            voices.map { v ->
                val region = v.locale.displayCountry.ifEmpty { "English" }
                val n = (counts[region] ?: 0) + 1
                counts[region] = n
                SpeechManager.SystemVoice(v.name, "English ($region) $n")
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun attrs() = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
}

object SyllableHelper {
    fun split(word: String): List<String> {
        val w = word.trim().lowercase()
        return when (w) {
            "butterfly" -> listOf("but", "ter", "fly")
            "dinosaur" -> listOf("di", "no", "saur")
            "computer" -> listOf("com", "pu", "ter")
            "telephone" -> listOf("tel", "e", "phone")
            "banana" -> listOf("ba", "nan", "a")
            "elephant" -> listOf("el", "e", "phant")
            "hospital" -> listOf("hos", "pi", "tal")
            "refrigerator" -> listOf("re", "frig", "er", "a", "tor")
            "playground" -> listOf("play", "ground")
            "classroom" -> listOf("class", "room")
            "water" -> listOf("wa", "ter")
            "happy" -> listOf("hap", "py")
            "morning" -> listOf("morn", "ing")
            else -> if (w.length <= 4) listOf(w) else listOf(w.substring(0, w.length / 2), w.substring(w.length / 2))
        }
    }
}

/**
 * Applies the touch-access settings to a press: a per-button lockout so a
 * tremor cannot turn one intended press into five.
 */
object TouchAccess {
    private val lastFired = HashMap<String, Long>()

    fun shouldFire(key: String, lockoutSeconds: Double, now: Long = System.currentTimeMillis()): Boolean {
        if (lockoutSeconds <= 0) return true
        val previous = lastFired[key]
        if (previous != null && now - previous < (lockoutSeconds * 1000).toLong()) return false
        lastFired[key] = now
        return true
    }

    fun reset() = lastFired.clear()
}

/** Records the parent's voice for a button as AAC in an .m4a, the same format the iPad writes. */
class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null

    var isRecording by mutableStateOf(false)
        private set
    var recordedData by mutableStateOf<ByteArray?>(null)

    fun start() {
        try {
            val f = File.createTempFile("tt-rec", ".m4a", context.cacheDir)
            @Suppress("DEPRECATION")
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(44100)
            r.setAudioEncodingBitRate(96000)
            r.setAudioChannels(1)
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            file = f
            isRecording = true
        } catch (e: Exception) {
            Log.w("TalkTiles", "recording start failed", e)
            isRecording = false
        }
    }

    fun stop() {
        try { recorder?.stop() } catch (e: Exception) { }
        try { recorder?.release() } catch (e: Exception) { }
        recorder = null
        isRecording = false
        val f = file
        if (f != null && f.exists() && f.length() > 0) {
            recordedData = f.readBytes()
            f.delete()
        }
        file = null
    }
}
