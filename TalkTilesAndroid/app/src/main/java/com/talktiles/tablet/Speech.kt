package com.talktiles.tablet

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
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

/**
 * Everything that makes a sound: Bella's clips, the device's text-to-speech,
 * and playback of a recording made on a button. One player at a time - a new
 * press silences whatever was still speaking.
 */
class SpeechManager private constructor(context: Context) {

    companion object {
        @Volatile private var instance: SpeechManager? = null
        fun init(context: Context): SpeechManager =
            instance ?: synchronized(this) { instance ?: SpeechManager(context.applicationContext).also { instance = it } }
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
    }

    private val app = context
    private val handler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var player: MediaPlayer? = null
    private var clipQueue: ArrayDeque<String> = ArrayDeque()
    private var clipRate = 1.0f
    private var tempFiles = ArrayList<File>()

    var isSpeaking by mutableStateOf(false)
        private set

    /** The voice every utterance uses unless a caller passes one. */
    var preferredVoiceId: String? = SpokenText.BELLA_VOICE_ID
    var defaultRate: Float = 0.45f

    init {
        tts = TextToSpeech(app) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) tts?.language = Locale.US
        }
    }

    /** A device voice, with a name a person can read - the engine's own are like "en-au-x-aua-local". */
    data class SystemVoice(val id: String, val title: String)

    /** Every offline English voice the device's engine offers, the device's own region first. */
    fun availableSystemVoices(): List<SystemVoice> {
        val t = tts ?: return emptyList()
        if (!ttsReady) return emptyList()
        return try {
            val here = Locale.getDefault()
            val voices = t.voices.filter { it.locale.language == "en" && !it.isNetworkConnectionRequired }
                .sortedWith(compareBy({ it.locale.country != here.country }, { it.locale.toString() }, { it.name }))
            val counts = HashMap<String, Int>()
            voices.map { v ->
                val region = v.locale.displayCountry.ifEmpty { "English" }
                val n = (counts[region] ?: 0) + 1
                counts[region] = n
                SystemVoice(v.name, "English ($region) $n")
            }
        } catch (e: Exception) { emptyList() }
    }

    fun speak(text: String, rate: Float? = null, voiceId: String? = null) {
        val r = rate ?: defaultRate
        if (text.isBlank()) return
        stopAll()
        val want = voiceId ?: preferredVoiceId
        if (VoiceClips.isRecordedVoice(want) && playClips(text, want, r)) return
        speakWithEngine(text, r, want)
    }

    private fun speakWithEngine(text: String, rate: Float, voiceId: String?) {
        val t = tts ?: return
        if (!ttsReady) return
        if (voiceId != null && !VoiceClips.isRecordedVoice(voiceId)) {
            val match = try { t.voices.firstOrNull { it.name == voiceId } } catch (e: Exception) { null }
            if (match != null) t.voice = match else t.language = Locale.US
        } else {
            t.language = Locale.US
        }
        t.setSpeechRate(ttsRate(rate))
        isSpeaking = true
        t.speak(SpokenText.forSpeech(text), TextToSpeech.QUEUE_FLUSH, null, "tt-${System.nanoTime()}")
        handler.postDelayed({ if (t.isSpeaking.not()) isSpeaking = false }, 300)
    }

    /** Sound It Out: syllables, then the word. */
    fun soundItOut(word: String) {
        val syllables = SyllableHelper.split(word)
        if (syllables.isEmpty()) { speak(word); return }
        stopAll()
        var delay = 0L
        for (s in syllables) {
            handler.postDelayed({ speakWithEngine(s, 0.45f, preferredVoiceId) }, delay)
            delay += 750
        }
        handler.postDelayed({ speak(word, 0.52f) }, delay)
    }

    // MARK: - Recordings

    fun playAudioData(data: ByteArray) {
        stopAll()
        try {
            val f = File.createTempFile("rec", ".m4a", app.cacheDir)
            f.writeBytes(data)
            tempFiles.add(f)
            val p = MediaPlayer()
            p.setAudioAttributes(attrs())
            p.setDataSource(f.absolutePath)
            p.setOnCompletionListener { isSpeaking = false; f.delete(); tempFiles.remove(f) }
            p.setOnErrorListener { _, _, _ -> isSpeaking = false; true }
            p.prepare()
            player = p
            isSpeaking = true
            p.start()
        } catch (e: Exception) {
            Log.w("TalkTiles", "recording playback failed", e)
            isSpeaking = false
        }
    }

    // MARK: - Recorded voice clips

    private fun playClips(text: String, voiceId: String?, rate: Float): Boolean {
        val urls = VoiceClips.clip(text, voiceId)?.let { listOf(it) }
            ?: VoiceClips.chain(text, voiceId)
            ?: return false
        return playClipSequence(urls, rate)
    }

    /** Plays asset clips back to back. Public so Settings can preview a voice. */
    fun playClipSequence(assets: List<String>, rate: Float? = null): Boolean {
        val first = assets.firstOrNull() ?: return false
        stopAll()
        clipQueue = ArrayDeque(assets.drop(1))
        clipRate = clipRate(rate ?: defaultRate)
        return startClip(first)
    }

    private fun startClip(asset: String): Boolean {
        return try {
            val p = MediaPlayer()
            p.setAudioAttributes(attrs())
            app.assets.openFd(asset).use { fd -> p.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length) }
            p.setOnCompletionListener {
                if (player === p) {
                    val next = clipQueue.removeFirstOrNull()
                    if (next != null) startClip(next) else isSpeaking = false
                }
                p.release()
            }
            p.setOnErrorListener { _, _, _ -> isSpeaking = false; p.release(); true }
            p.prepare()
            if (clipRate != 1.0f) {
                try { p.playbackParams = p.playbackParams.setSpeed(clipRate) } catch (e: Exception) { }
            }
            player = p
            isSpeaking = true
            p.start()
            true
        } catch (e: Exception) {
            Log.w("TalkTiles", "clip failed: $asset", e)
            clipQueue.clear()
            isSpeaking = false
            false
        }
    }

    private fun stopAll() {
        clipQueue.clear()
        player?.let { p ->
            try { if (p.isPlaying) p.stop() } catch (e: Exception) { }
            try { p.release() } catch (e: Exception) { }
        }
        player = null
        try { tts?.stop() } catch (e: Exception) { }
        isSpeaking = false
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
