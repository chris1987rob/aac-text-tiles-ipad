package com.talktiles.tablet

import android.util.Log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The book's files, and the two promises the store relies on:
 *
 * 1. An older snapshot can never overwrite a newer one. Every write carries
 *    the store's generation number; a write that arrives late - a debounce
 *    thread scheduled before an `onPause` save - is dropped.
 * 2. A file that cannot be read is never replaced. It is moved aside as
 *    `<name>.unreadable-<stamp>` where a person (or a later build) can still
 *    get at it, and a notice says so.
 *
 * Pure java.io so it runs in plain JVM tests. Nothing here logs user content:
 * a parse failure is logged by exception class only, because the message
 * kotlinx.serialization builds quotes the JSON around the fault.
 */
class BookStorage(private val dir: File, private val clock: () -> Long = System::currentTimeMillis) {

    sealed class Load<out T> {
        data class Ok<T>(val value: T) : Load<T>()
        object Missing : Load<Nothing>()
        data class Unreadable(val keptAs: File) : Load<Nothing>()
    }

    val pagesFile = File(dir, "aac_pages.json")
    val settingsFile = File(dir, "aac_settings.json")
    val favoritesFile = File(dir, "aac_favorites.json")
    val phrasesFile = File(dir, "aac_phrases.json")
    private val snapshotDir = File(dir, "snapshots")

    /** Plain-language notes about files that had to be moved aside, for Settings to show. */
    val notices = ArrayList<String>()

    private val lock = Any()
    private var lastWrittenGeneration = Long.MIN_VALUE

    // MARK: - Reading

    private fun <T> load(file: File, serializer: KSerializer<T>, json: Json = AppJson): Load<T> {
        if (!file.exists()) return Load.Missing
        val text = try { file.readText() } catch (e: Exception) { return keepAside(file, e) }
        return try { Load.Ok(json.decodeFromString(serializer, text)) }
        catch (e: Exception) { keepAside(file, e) }
    }

    private fun keepAside(file: File, cause: Exception): Load<Nothing> {
        Log.e("TalkTiles", "${file.name} could not be read (${cause.javaClass.simpleName}); keeping it aside")
        val kept = File(dir, "${file.name}.unreadable-${stamp()}")
        if (!file.renameTo(kept)) {
            // Rename refused (odd filesystems): copy, then the original is left where it is.
            try { file.copyTo(kept, overwrite = true) } catch (e: Exception) { Log.e("TalkTiles", "could not keep ${file.name} aside", e) }
        }
        notices.add("${file.name} could not be read and was kept as ${kept.name}. A fresh file was started; nothing was deleted.")
        return Load.Unreadable(kept)
    }

    fun loadPages(): Load<List<PageModel>> = load(pagesFile, pageListSerializer)
    fun loadSettings(): Load<AppSettings> = load(settingsFile, AppSettings.serializer())
    fun loadFavorites(): Load<List<SavedTile>> = load(favoritesFile, ListSerializer(SavedTile.serializer()))
    fun loadPhrases(): Load<List<SavedPhrase>> = load(phrasesFile, ListSerializer(SavedPhrase.serializer()))

    // MARK: - Writing

    private fun writeAtomic(file: File, text: String) {
        val tmp = File(file.path + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) { file.delete(); if (!tmp.renameTo(file)) throw IllegalStateException("rename failed for ${file.name}") }
    }

    /**
     * Writes the book if `generation` is newer than anything written so far.
     * Returns false when the snapshot was stale and dropped.
     */
    fun writePages(pages: List<PageModel>, generation: Long): Boolean {
        synchronized(lock) {
            if (generation <= lastWrittenGeneration) return false
            try {
                writeAtomic(pagesFile, AppJson.encodeToString(pageListSerializer, pages))
                lastWrittenGeneration = generation
                return true
            } catch (e: Exception) {
                Log.e("TalkTiles", "book save failed", e)
                return false
            }
        }
    }

    fun writeSettings(settings: AppSettings) {
        // "Device default" voice is stored as an explicit null so it survives a
        // reload; a file with no key at all predates Bella and gets her.
        try { synchronized(lock) { writeAtomic(settingsFile, SettingsJson.encodeToString(AppSettings.serializer(), settings)) } }
        catch (e: Exception) { Log.e("TalkTiles", "settings save failed", e) }
    }

    fun writeFavorites(items: List<SavedTile>) {
        try { synchronized(lock) { writeAtomic(favoritesFile, AppJson.encodeToString(ListSerializer(SavedTile.serializer()), items)) } }
        catch (e: Exception) { Log.e("TalkTiles", "saved buttons save failed", e) }
    }

    fun writePhrases(items: List<SavedPhrase>) {
        try { synchronized(lock) { writeAtomic(phrasesFile, AppJson.encodeToString(ListSerializer(SavedPhrase.serializer()), items)) } }
        catch (e: Exception) { Log.e("TalkTiles", "saved phrases save failed", e) }
    }

    /** Forgets the generation watermark - only for a restore, which starts a new line of history. */
    fun resetGeneration() { synchronized(lock) { lastWrittenGeneration = Long.MIN_VALUE } }

    // MARK: - Snapshots

    /** Copies the book as it is on disk to `snapshots/aac_pages-<tag>-<stamp>.json` before a restore or reset replaces it. */
    fun snapshotBook(tag: String): File? {
        synchronized(lock) {
            if (!pagesFile.exists()) return null
            return try {
                snapshotDir.mkdirs()
                val out = File(snapshotDir, "aac_pages-$tag-${stamp()}.json")
                pagesFile.copyTo(out, overwrite = true)
                prune()
                out
            } catch (e: Exception) { Log.e("TalkTiles", "snapshot failed", e); null }
        }
    }

    private fun prune() {
        val all = snapshotDir.listFiles()?.filter { it.name.startsWith("aac_pages-") }?.sortedBy { it.name } ?: return
        for (old in all.dropLast(SNAPSHOT_LIMIT)) old.delete()
    }

    private fun stamp(): String {
        val f = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US)
        f.timeZone = TimeZone.getTimeZone("UTC")
        return f.format(Date(clock()))
    }

    companion object {
        const val SNAPSHOT_LIMIT = 5
    }
}
