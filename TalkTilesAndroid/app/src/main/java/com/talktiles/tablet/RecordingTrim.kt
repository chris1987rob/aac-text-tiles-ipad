package com.talktiles.tablet

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Cuts the silence off both ends of a button recording. A parent taps
 * Record, looks at the child, then speaks: the first second is room noise,
 * and every tap of that button used to play it before the word.
 *
 * `speechBounds` is pure (PCM in, times out) so it is tested on the JVM;
 * `trim` does the Android media work: decode to find the speech, then copy
 * the AAC frames inside it into a new .m4a without re-encoding.
 */
object RecordingTrim {
    private const val WINDOW_MS = 10
    /** Kept before the first loud window, so a soft first consonant ("h", "f") is not clipped. */
    const val PRE_ROLL_MS = 60
    /** Kept after the last loud window, so a word's tail fades naturally. */
    const val POST_ROLL_MS = 180
    /** Below this RMS (about -44 dBFS) a window is silence however quiet the recording is. */
    private const val ABS_FLOOR = 200.0
    /** A window counts as speech at 1/10 of the loudest window's RMS (-20 dB). */
    private const val REL_THRESHOLD = 0.10

    /** Start and end of the speech in microseconds, or null when nothing is loud enough to call speech. */
    fun speechBounds(pcm: ShortArray, sampleRate: Int): Pair<Long, Long>? {
        val win = maxOf(1, sampleRate * WINDOW_MS / 1000)
        val count = pcm.size / win
        if (count == 0) return null
        val rms = DoubleArray(count) { w ->
            var sum = 0.0
            for (i in w * win until (w + 1) * win) { val v = pcm[i].toDouble(); sum += v * v }
            sqrt(sum / win)
        }
        val peak = rms.maxOrNull() ?: return null
        val threshold = maxOf(ABS_FLOOR, peak * REL_THRESHOLD)
        val first = rms.indexOfFirst { it >= threshold }
        val last = rms.indexOfLast { it >= threshold }
        if (first < 0) return null
        val totalUs = pcm.size * 1_000_000L / sampleRate
        val startUs = maxOf(0L, first * WINDOW_MS * 1000L - PRE_ROLL_MS * 1000L)
        val endUs = minOf(totalUs, (last + 1) * WINDOW_MS * 1000L + POST_ROLL_MS * 1000L)
        return startUs to endUs
    }

    /** Writes the trimmed recording to `output`. False (and `output` untouched or deleted) when it could not, or when nothing needed cutting. */
    fun trim(input: File, output: File): Boolean {
        return try {
            val decoded = decode(input) ?: return false
            val (pcm, rate, durationUs) = decoded
            val (startUs, endUs) = speechBounds(pcm, rate) ?: return false
            // Not worth a rewrite for a few frames either end.
            if (startUs < 100_000 && durationUs - endUs < 250_000) return false
            copyFrames(input, output, startUs, endUs)
        } catch (e: Exception) {
            Log.w("TalkTiles", "recording trim failed (${e.javaClass.simpleName}); keeping it as recorded")
            output.delete()
            false
        }
    }

    private fun audioTrack(ex: MediaExtractor): Int {
        for (i in 0 until ex.trackCount) {
            if (ex.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) return i
        }
        return -1
    }

    /** First channel as 16-bit PCM, its sample rate and the file's duration. */
    private fun decode(file: File): Triple<ShortArray, Int, Long>? {
        val ex = MediaExtractor()
        try {
            ex.setDataSource(file.absolutePath)
            val track = audioTrack(ex)
            if (track < 0) return null
            ex.selectTrack(track)
            val format = ex.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
            val codec = MediaCodec.createDecoderByType(mime)
            try {
                codec.configure(format, null, null, 0)
                codec.start()
                var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                val out = ShortArrayBuilder()
                val info = MediaCodec.BufferInfo()
                var inputDone = false
                var outputDone = false
                while (!outputDone) {
                    if (!inputDone) {
                        val i = codec.dequeueInputBuffer(10_000)
                        if (i >= 0) {
                            val buf = codec.getInputBuffer(i)!!
                            val n = ex.readSampleData(buf, 0)
                            if (n < 0) {
                                codec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(i, 0, n, ex.sampleTime, 0)
                                ex.advance()
                            }
                        }
                    }
                    val o = codec.dequeueOutputBuffer(info, 10_000)
                    when {
                        o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val f = codec.outputFormat
                            rate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                        o >= 0 -> {
                            val buf = codec.getOutputBuffer(o)!!
                            buf.position(info.offset); buf.limit(info.offset + info.size)
                            val shorts = buf.order(ByteOrder.nativeOrder()).asShortBuffer()
                            var k = 0
                            while (k < shorts.remaining()) { out.add(shorts.get(k)); k += maxOf(1, channels) }
                            codec.releaseOutputBuffer(o, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                        }
                    }
                }
                val pcm = out.toArray()
                val dur = if (durationUs > 0) durationUs else pcm.size * 1_000_000L / rate
                return Triple(pcm, rate, dur)
            } finally {
                try { codec.stop() } catch (e: Exception) { }
                codec.release()
            }
        } finally {
            ex.release()
        }
    }

    /** Copies the AAC frames between the two times into a new .m4a, timestamps starting at zero. */
    private fun copyFrames(input: File, output: File, startUs: Long, endUs: Long): Boolean {
        val ex = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            ex.setDataSource(input.absolutePath)
            val track = audioTrack(ex)
            if (track < 0) return false
            ex.selectTrack(track)
            val format = ex.getTrackFormat(track)
            val m = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = m
            val outTrack = m.addTrack(format)
            m.start()
            val size = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 64 * 1024
            val buf = ByteBuffer.allocate(size)
            val info = MediaCodec.BufferInfo()
            var base = -1L
            var written = 0
            while (true) {
                val n = ex.readSampleData(buf, 0)
                if (n < 0) break
                val t = ex.sampleTime
                if (t > endUs) break
                if (t >= startUs) {
                    if (base < 0) base = t
                    info.set(0, n, t - base, if (ex.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                    m.writeSampleData(outTrack, buf, info)
                    written++
                }
                ex.advance()
            }
            m.stop()
            return written > 0 && output.length() > 0
        } finally {
            try { muxer?.release() } catch (e: Exception) { }
            ex.release()
        }
    }

    private class ShortArrayBuilder {
        private var data = ShortArray(48_000)
        private var size = 0
        fun add(v: Short) {
            if (size == data.size) data = data.copyOf(data.size * 2)
            data[size++] = v
        }
        fun toArray(): ShortArray = data.copyOf(size)
    }
}
