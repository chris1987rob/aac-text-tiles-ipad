package com.talktiles.tablet

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** The book: every page, the page on screen, edit mode, settings, the sentence bar. */
class AACStore(context: Context) {
    private val app = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())

    var pages by mutableStateOf<List<PageModel>>(emptyList())
        private set
    var currentPageIndex by mutableStateOf(0)
    var isEditMode by mutableStateOf(false)
    val expressChips = mutableStateListOf<String>()

    var settings by mutableStateOf(AppSettings())
        private set

    var isLocked: Boolean
        get() = settings.childLock
        set(v) { updateSettings { it.copy(childLock = v) } }

    val currentPage: PageModel
        get() = pages.getOrNull(currentPageIndex) ?: PageModel(title = "Default")

    init {
        loadPages()
        loadSettings()
        applySpeechSettings()
    }

    // MARK: - Settings

    fun updateSettings(change: (AppSettings) -> AppSettings) {
        val next = change(settings)
        if (next == settings) return
        settings = next
        applySpeechSettings()
        saveSettings()
    }

    private fun applySpeechSettings() {
        SpeechManager.shared.preferredVoiceId = settings.voiceId
        SpeechManager.shared.defaultRate = settings.speechRate.toFloat()
    }

    // MARK: - Pages

    fun replacePages(list: List<PageModel>) {
        pages = list
        if (currentPageIndex >= list.size) currentPageIndex = maxOf(0, list.size - 1)
        save()
    }

    fun updateCurrentPage(change: (PageModel) -> PageModel) {
        val idx = currentPageIndex
        if (idx !in pages.indices) return
        val next = change(pages[idx])
        if (next == pages[idx]) return
        pages = pages.toMutableList().also { it[idx] = next }
        save()
    }

    fun updatePage(id: String, change: (PageModel) -> PageModel) {
        val idx = pages.indexOfFirst { it.id == id }
        if (idx < 0) return
        pages = pages.toMutableList().also { it[idx] = change(it[idx]) }
        save()
    }

    fun addPage(page: PageModel) {
        pages = pages + page
        currentPageIndex = pages.size - 1
        save()
    }

    fun removePage(index: Int) {
        if (pages.size <= 1 || index !in pages.indices) return
        pages = pages.toMutableList().also { it.removeAt(index) }
        if (currentPageIndex >= pages.size) currentPageIndex = maxOf(0, pages.size - 1)
        save()
    }

    fun nextPage() { currentPageIndex = step(1) }
    fun prevPage() { currentPageIndex = step(-1) }
    private fun step(delta: Int): Int {
        if (pages.isEmpty()) return 0
        val n = pages.size
        return ((currentPageIndex + delta) % n + n) % n
    }

    // MARK: - Sentence bar

    fun addExpressChip(chip: String) { expressChips.add(chip) }
    fun clearExpressChips() { expressChips.clear() }
    fun playExpressSentence() {
        val sentence = expressChips.joinToString(" ")
        if (sentence.isNotEmpty()) SpeechManager.shared.speak(sentence, settings.speechRate.toFloat(), settings.voiceId)
    }

    // MARK: - Persistence

    private val storeFile: File get() = File(app.filesDir, "aac_pages.json")
    private val settingsFile: File get() = File(app.filesDir, "aac_settings.json")

    private var pendingSave: Runnable? = null

    /** Debounced: every mutation calls this, including live hotspot drags. */
    fun save() {
        pendingSave?.let { handler.removeCallbacks(it) }
        val snapshot = pages
        val r = Runnable { Thread { writeToDisk(snapshot) }.start() }
        pendingSave = r
        handler.postDelayed(r, 400)
    }

    /** Immediate write, for when the app is about to stop. */
    fun saveNow() {
        pendingSave?.let { handler.removeCallbacks(it) }
        pendingSave = null
        writeToDisk(pages)
    }

    @Synchronized
    private fun writeToDisk(list: List<PageModel>) {
        try {
            val tmp = File(storeFile.path + ".tmp")
            tmp.writeText(AppJson.encodeToString(pageListSerializer, list))
            if (!tmp.renameTo(storeFile)) { storeFile.delete(); tmp.renameTo(storeFile) }
        } catch (e: Exception) { Log.e("TalkTiles", "save failed", e) }
    }

    private fun loadPages() {
        val restored = loadFromDisk()
        pages = if (!restored.isNullOrEmpty()) restored else defaultPages()
    }

    private fun loadFromDisk(): List<PageModel>? {
        if (!storeFile.exists()) return null
        return try { AppJson.decodeFromString(pageListSerializer, storeFile.readText()) }
        catch (e: Exception) { Log.e("TalkTiles", "load failed", e); null }
    }

    private fun loadSettings() {
        if (!settingsFile.exists()) return
        settings = try { AppJson.decodeFromString(AppSettings.serializer(), settingsFile.readText()) }
        catch (e: Exception) { Log.e("TalkTiles", "settings load failed", e); return }
    }

    fun saveSettings() {
        // "Device default" voice is stored as an explicit null so it survives
        // a reload; a file with no key at all predates Bella and gets her.
        try { settingsFile.writeText(SettingsJson.encodeToString(AppSettings.serializer(), settings)) }
        catch (e: Exception) { Log.e("TalkTiles", "settings save failed", e) }
    }

    fun resetToDefaults() {
        storeFile.delete()
        pages = defaultPages()
        currentPageIndex = 0
        saveNow()
    }

    fun restore(list: List<PageModel>, restoredSettings: AppSettings?) {
        pages = list
        currentPageIndex = 0
        if (restoredSettings != null) { settings = restoredSettings; applySpeechSettings() }
        saveNow(); saveSettings()
    }

    companion object {
        private fun t(id: Int, label: String, tts: String, symbol: String? = null, bg: String, border: String, text: String, size: Double = 1.0) =
            TileModel(id = id, label = label, tts = tts, symbolName = symbol, bgHex = bg, borderHex = border, labelHex = text, labelSize = size)

        /** Built-in starter communication book, used on first launch or after a reset. */
        fun defaultPages(): List<PageModel> {
            val first = listOf(
                PageModel(title = "Colors", type = PageType.GRID, gridSize = 4, tiles = mapOf(
                    1 to t(1, "Red", "Red", null, "#FF4D4D", "#D32F2F", "#FFFFFF"),
                    2 to t(2, "Orange", "Orange", null, "#FFA500", "#E65100", "#FFFFFF"),
                    3 to t(3, "Yellow", "Yellow", null, "#FFEB3B", "#FBC02D", "#1E293B"),
                    4 to t(4, "Green", "Green", null, "#4CAF50", "#2E7D32", "#FFFFFF")
                )),
                PageModel(title = "Yes / No", type = PageType.GRID, gridSize = 2, tiles = mapOf(
                    1 to t(1, "YES", "Yes", "yes", "#C8E6C9", "#2E7D32", "#1B5E20", 1.4),
                    2 to t(2, "NO", "No", "no", "#FFCDD2", "#C62828", "#B71C1C", 1.4)
                )),
                PageModel(title = "Core Words", type = PageType.GRID, gridSize = 9, express = true, tiles = mapOf(
                    1 to t(1, "I want", "I want", "help", "#E1BEE7", "#8E24AA", "#4A148C"),
                    2 to t(2, "Eat", "Eat food", "eat", "#C8E6C9", "#388E3C", "#1B5E20"),
                    3 to t(3, "Drink", "Drink water", "water", "#BBDEFB", "#1976D2", "#0D47A1"),
                    4 to t(4, "Help", "Please help me", "help", "#FFF59D", "#FBC02D", "#1E293B"),
                    5 to t(5, "Play", "Play games", "play", "#FFE0B2", "#F57C00", "#E65100"),
                    6 to t(6, "Bathroom", "I need to go to the bathroom", "bathroom", "#D1C4E9", "#5E35B1", "#311B92"),
                    7 to t(7, "More", "More please", "more", "#C8E6C9", "#388E3C", "#1B5E20"),
                    8 to t(8, "Stop", "Stop now", "stop", "#FFCDD2", "#D32F2F", "#B71C1C"),
                    9 to t(9, "Happy", "I feel happy", "happy", "#FFF9C4", "#FBC02D", "#1E293B")
                )),
                PageModel(title = "Talking Keyboard", type = PageType.KEYBOARD, gridSize = 1, bgHex = "#F8FAFC")
            )
            val existing = setOf("colors", "yes / no", "core words", "talking keyboard")
            val starters = PageTemplateCatalog.popular
                .filter { it.title.lowercase() !in existing }
                .map { it.makePage() }
            return first + starters
        }
    }
}

/** The saved-buttons library, on disk beside the board. */
class TileFavorites(context: Context) {
    private val file = File(context.applicationContext.filesDir, "aac_favorites.json")
    private val serializer = ListSerializer(SavedTile.serializer())

    var items by mutableStateOf<List<SavedTile>>(emptyList())
        private set

    companion object {
        const val LIMIT = 200
        @Volatile private var instance: TileFavorites? = null
        fun init(context: Context): TileFavorites =
            instance ?: synchronized(this) { instance ?: TileFavorites(context).also { instance = it } }
        val shared: TileFavorites get() = instance ?: error("TileFavorites.init first")
    }

    init {
        items = try { if (file.exists()) AppJson.decodeFromString(serializer, file.readText()) else emptyList() }
        catch (e: Exception) { emptyList() }
    }

    private fun save() {
        try { file.writeText(AppJson.encodeToString(serializer, items)) } catch (e: Exception) { }
    }

    /** Saving the same name twice replaces the earlier copy. */
    fun add(saved: SavedTile): SavedTile {
        var entry = saved
        if (entry.name.isBlank()) entry = entry.copy(name = entry.label.ifEmpty { "Button" })
        val list = items.toMutableList()
        val existing = list.indexOfFirst { it.name.equals(entry.name, ignoreCase = true) }
        if (existing >= 0) {
            entry = entry.copy(id = list[existing].id)
            list[existing] = entry
        } else {
            list.add(0, entry)
            while (list.size > LIMIT) list.removeAt(list.size - 1)
        }
        items = list
        save()
        return entry
    }

    fun remove(id: String) { items = items.filter { it.id != id }; save() }

    fun contains(name: String) = items.any { it.name.equals(name, ignoreCase = true) }

    fun search(query: String): List<SavedTile> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return items
        return items.filter { it.name.lowercase().contains(q) || it.label.lowercase().contains(q) || it.tts.lowercase().contains(q) }
    }
}

/** Whole-book backup and restore, in the iPad's `talktiles.book` format. */
object BookBackup {
    data class Summary(val pages: Int, val buttons: Int, val hotspots: Int, val createdAt: String)

    private fun iso(): String {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        f.timeZone = TimeZone.getTimeZone("UTC")
        return f.format(Date())
    }

    fun write(context: Context, pages: List<PageModel>, settings: AppSettings): File {
        val archive = BookArchive(createdAt = iso(), pages = pages, settings = settings)
        val stamp = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val f = File(dir, "TalkTiles-Backup-$stamp.json")
        f.writeText(AppJson.encodeToString(BookArchive.serializer(), archive))
        return f
    }

    fun writePage(context: Context, page: PageModel): File {
        val safe = page.title.lowercase().replace(' ', '_').filter { it.isLetterOrDigit() || it == '_' }
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val f = File(dir, "talk_tiles_page_${safe.ifEmpty { "page" }}.json")
        f.writeText(AppJson.encodeToString(PageModel.serializer(), page))
        return f
    }

    /** Reads an archive, a bare page list, or a single shared page, without applying it. */
    fun read(text: String): Pair<BookArchive, Summary> {
        val archive = try {
            AppJson.decodeFromString(BookArchive.serializer(), text)
        } catch (e: Exception) {
            try {
                BookArchive(createdAt = iso(), pages = AppJson.decodeFromString(pageListSerializer, text))
            } catch (e2: Exception) {
                BookArchive(createdAt = iso(), pages = listOf(AppJson.decodeFromString(PageModel.serializer(), text)))
            }
        }
        if (archive.pages.isEmpty()) throw IllegalArgumentException("That file does not have any pages in it.")
        val summary = Summary(
            pages = archive.pages.size,
            buttons = archive.pages.sumOf { it.tiles.size },
            hotspots = archive.pages.sumOf { it.hotspots.size },
            createdAt = archive.createdAt
        )
        return archive to summary
    }
}
