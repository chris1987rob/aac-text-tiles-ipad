package com.talktiles.tablet

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** The book: every page, the page on screen, edit mode, settings, the sentence bar. */
class AACStore(
    context: Context,
    val storage: BookStorage = BookStorage(context.applicationContext.filesDir),
    /** Free / trial / Pro. See Pro.kt. */
    val pro: ProAccess = ProAccess(storage.licenceFile)
) {
    private val handler = Handler(Looper.getMainLooper())

    var pages by mutableStateOf<List<PageModel>>(emptyList())
        private set

    /**
     * The page on screen. In the player the page is remembered so the book
     * opens where it was left; the editor's position is not (a parent
     * fixing page 9 should not strand the child there).
     */
    var currentPageIndex: Int
        get() = pageIndexState
        set(value) {
            pageIndexState = value
            if (!isEditMode) rememberPage(value)
        }
    private var pageIndexState by mutableStateOf(0)

    var isEditMode by mutableStateOf(false)

    /** A keyboard word group the next keyboard page should open on (set by Find). */
    var requestedKeyGroup by mutableStateOf<String?>(null)

    /** One sentence bar for the whole book: the grid and the keyboard share it and turning a page keeps it. */
    val sentence = SentenceBuilder()

    var settings by mutableStateOf(AppSettings())
        private set

    /** Files that could not be read at launch and were kept aside - shown in Settings. */
    val recoveryNotices: List<String> get() = storage.notices

    /** "Protect editing": every way to change the book asks for the PIN. Speaking never does. */
    val isLocked: Boolean get() = settings.childLock

    val currentPage: PageModel
        get() = pages.getOrNull(currentPageIndex) ?: PageModel(title = "Default")

    init {
        loadSettings()
        loadPages()
        pageIndexState = if (settings.openOnLastPage) BookNavigation.startIndex(pages, settings.lastPageId)
            else BookNavigation.startIndex(pages, null)
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

    private fun rememberPage(index: Int) {
        val id = pages.getOrNull(index)?.id ?: return
        if (id == settings.lastPageId) return
        settings = settings.copy(lastPageId = id)
        // Debounced with the book so a run of page turns is one write.
        scheduleSettingsSave()
    }

    // MARK: - Pages

    fun replacePages(list: List<PageModel>) {
        pages = list
        if (currentPageIndex >= list.size) pageIndexState = maxOf(0, list.size - 1)
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
        pageIndexState = pages.size - 1
        save()
    }

    fun removePage(index: Int) {
        if (pages.size <= 1 || index !in pages.indices) return
        pages = pages.toMutableList().also { it.removeAt(index) }
        if (currentPageIndex >= pages.size) pageIndexState = maxOf(0, pages.size - 1)
        save()
    }

    /** Moves a page in the book's order - the editor's reorder control. Slot positions inside pages are untouched. */
    fun movePage(from: Int, to: Int) {
        if (from !in pages.indices || to !in pages.indices || from == to) return
        val currentId = currentPage.id
        pages = pages.toMutableList().also { it.add(to, it.removeAt(from)) }
        pageIndexState = pages.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        save()
    }

    fun nextPage() { currentPageIndex = BookNavigation.step(pages, currentPageIndex, +1, isEditMode) }
    fun prevPage() { currentPageIndex = BookNavigation.step(pages, currentPageIndex, -1, isEditMode) }
    val pagePosition: BookNavigation.Position get() = BookNavigation.position(pages, currentPageIndex, isEditMode)
    val canStep: Boolean get() = BookNavigation.canStep(pages, isEditMode)

    /** Goes to a page by id if the reader is allowed there. Never speaks. Returns false when refused. */
    fun goToPage(id: String?): Boolean {
        val idx = BookNavigation.indexForJump(pages, id, isEditMode) ?: return false
        currentPageIndex = idx
        return true
    }

    /** Leaving the editor: if the page being edited is switched off, the player must not open on it. */
    fun enterPlayer() {
        isEditMode = false
        if (!currentPage.enabled) pageIndexState = BookNavigation.startIndex(pages, settings.lastPageId)
        rememberPage(currentPageIndex)
    }

    // MARK: - Sentence bar

    fun speakSentence() {
        if (sentence.isEmpty) return
        SpeechManager.shared.speakItems(sentence.items, settings.speechRate.toFloat(), settings.voiceId)
    }

    // MARK: - Persistence

    /** Counts every change; a write carries the number it was made at, so a late write can be told from a new one. */
    private var generation = 0L
    private var pendingSave: Runnable? = null
    private var pendingSettingsSave: Runnable? = null

    /** Debounced: every mutation calls this, including live hotspot drags. */
    fun save() {
        generation += 1
        pendingSave?.let { handler.removeCallbacks(it) }
        val snapshot = pages
        val gen = generation
        val r = Runnable { Thread { storage.writePages(snapshot, gen) }.start() }
        pendingSave = r
        handler.postDelayed(r, 400)
    }

    /** Immediate write, for when the app is about to stop. */
    fun saveNow() {
        pendingSave?.let { handler.removeCallbacks(it) }
        pendingSave = null
        generation += 1
        storage.writePages(pages, generation)
        pendingSettingsSave?.let { handler.removeCallbacks(it); pendingSettingsSave = null }
        saveSettings()
    }

    private fun scheduleSettingsSave() {
        pendingSettingsSave?.let { handler.removeCallbacks(it) }
        val r = Runnable { pendingSettingsSave = null; saveSettings() }
        pendingSettingsSave = r
        handler.postDelayed(r, 400)
    }

    private fun loadPages() {
        pages = when (val r = storage.loadPages()) {
            is BookStorage.Load.Ok -> r.value.ifEmpty { defaultPages() }
            BookStorage.Load.Missing -> defaultPages()
            is BookStorage.Load.Unreadable -> defaultPages()   // the unreadable file is kept aside by the storage
        }
    }

    private fun loadSettings() {
        settings = when (val r = storage.loadSettings()) {
            is BookStorage.Load.Ok -> r.value
            else -> AppSettings()
        }
    }

    fun saveSettings() = storage.writeSettings(settings)

    /** Puts the starter book back. The book that was there is snapshotted first, so a slip is not the end of it. */
    fun resetToDefaults() {
        saveNow()
        storage.snapshotBook("pre-reset")
        pages = defaultPages()
        pageIndexState = 0
        sentence.clear()
        saveNow()
    }

    /** Applies an archive that `BookBackup.read` already checked. The current book is snapshotted first. */
    fun restore(archive: BookArchive) {
        saveNow()
        storage.snapshotBook("pre-restore")
        pages = archive.pages
        pageIndexState = BookNavigation.startIndex(pages, null)
        archive.settings?.let { restored ->
            // The archive's voice, touch and lock settings come across; what is remembered about THIS device does not.
            settings = restored.copy(lastPageId = null)
            applySpeechSettings()
        }
        archive.savedTiles?.let { TileFavorites.shared.replaceAll(it) }
        archive.phrases?.let { PhraseLibrary.shared.replaceAll(it) }
        sentence.clear()
        storage.resetGeneration()
        saveNow()
    }

    companion object {
        private fun t(id: Int, label: String, tts: String, symbol: String? = null, bg: String, border: String, text: String, size: Double = 1.0) =
            TileModel(id = id, label = label, tts = tts, symbolName = symbol, bgHex = bg, borderHex = border, labelHex = text, labelSize = size)

        /** Built-in starter communication book, used on first launch or after a reset. */
        fun defaultPages(): List<PageModel> {
            val first = listOf(
                PageModel(title = "Colors", type = PageType.GRID, gridSize = 4, tiles = mapOf(
                    // A picture on every button, on a pale ground so the picture shows.
                    1 to t(1, "Red", "Red", "tt:red", "#FFB4B4", "#D32F2F", "#1E293B"),
                    2 to t(2, "Orange", "Orange", "tt:orange_col", "#FFD1A3", "#E65100", "#1E293B"),
                    3 to t(3, "Yellow", "Yellow", "tt:yellow", "#FFF3A3", "#FBC02D", "#1E293B"),
                    4 to t(4, "Green", "Green", "tt:green", "#BDEBC8", "#2E7D32", "#1E293B")
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
            // Two example scenes, so a new family sees what talking spots do.
            val grids = first + starters
            val ids = grids.associate { it.title to it.id }
            val scenes = ExampleScenes.starterKeys.mapNotNull { ExampleScenes.byKey(it) }.map { ExampleScenes.makePage(it, ids) }
            return grids + scenes
        }
    }
}

/** The saved-buttons library, on disk beside the board. */
class TileFavorites(internal val storage: BookStorage) {

    var items by mutableStateOf<List<SavedTile>>(emptyList())
        private set

    companion object {
        const val LIMIT = 200
        @Volatile private var instance: TileFavorites? = null
        /** Bound to `storage`; a different storage (a restore into another folder, a test) gets a fresh library. */
        fun init(storage: BookStorage): TileFavorites = synchronized(this) {
            instance?.takeIf { it.storage === storage } ?: TileFavorites(storage).also { instance = it }
        }
        val shared: TileFavorites get() = instance ?: error("TileFavorites.init first")
    }

    init {
        items = (storage.loadFavorites() as? BookStorage.Load.Ok)?.value ?: emptyList()
    }

    private fun save() = storage.writeFavorites(items)

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

    /** A restore brings the archive's saved buttons in place of these. */
    fun replaceAll(list: List<SavedTile>) { items = list.take(LIMIT); save() }

    fun contains(name: String) = items.any { it.name.equals(name, ignoreCase = true) }

    fun search(query: String): List<SavedTile> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return items
        return items.filter { it.name.lowercase().contains(q) || it.label.lowercase().contains(q) || it.tts.lowercase().contains(q) }
    }
}

/** Sentences kept for later. Offline, on disk beside the book. */
class PhraseLibrary(internal val storage: BookStorage) {

    var items by mutableStateOf<List<SavedPhrase>>(emptyList())
        private set

    companion object {
        const val LIMIT = 200
        @Volatile private var instance: PhraseLibrary? = null
        fun init(storage: BookStorage): PhraseLibrary = synchronized(this) {
            instance?.takeIf { it.storage === storage } ?: PhraseLibrary(storage).also { instance = it }
        }
        val shared: PhraseLibrary get() = instance ?: error("PhraseLibrary.init first")
    }

    init {
        items = (storage.loadPhrases() as? BookStorage.Load.Ok)?.value ?: emptyList()
    }

    private fun save() = storage.writePhrases(items)

    /** Keeps the sentence exactly as built; the name defaults to its words. */
    fun add(items: List<SentenceItem>, name: String = ""): SavedPhrase? {
        if (items.isEmpty()) return null
        val phrase = SavedPhrase(name = name.trim().ifEmpty { items.joinToString(" ") { it.label } }, items = items.toList())
        this.items = (listOf(phrase) + this.items).take(LIMIT)
        save()
        return phrase
    }

    fun remove(id: String) { items = items.filter { it.id != id }; save() }

    fun replaceAll(list: List<SavedPhrase>) { items = list.take(LIMIT); save() }
}

/** Whole-book backup and restore, in the iPad's `talktiles.book` format. */
object BookBackup {
    /** Bumped only if the shape changes in a way an older app could not read. */
    const val FORMAT_VERSION = 1

    data class Summary(val pages: Int, val buttons: Int, val hotspots: Int, val savedButtons: Int, val phrases: Int, val createdAt: String)

    fun iso(now: Date = Date()): String {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        f.timeZone = TimeZone.getTimeZone("UTC")
        return f.format(now)
    }

    /** The archive as text - the same bytes whether shared or saved to a document. */
    fun encode(pages: List<PageModel>, settings: AppSettings, savedTiles: List<SavedTile>, phrases: List<SavedPhrase>, createdAt: String = iso()): String {
        val archive = BookArchive(createdAt = createdAt, pages = pages, settings = settings,
            savedTiles = savedTiles.ifEmpty { null }, phrases = phrases.ifEmpty { null })
        return AppJson.encodeToString(BookArchive.serializer(), archive)
    }

    fun fileName(now: Date = Date()): String = "TalkTiles-Backup-${SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(now)}.json"

    fun write(context: Context, pages: List<PageModel>, settings: AppSettings, savedTiles: List<SavedTile>, phrases: List<SavedPhrase>): File {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val f = File(dir, fileName())
        f.writeText(encode(pages, settings, savedTiles, phrases))
        return f
    }

    fun writePage(context: Context, page: PageModel): File {
        val safe = page.title.lowercase().replace(' ', '_').filter { it.isLetterOrDigit() || it == '_' }
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val f = File(dir, "talk_tiles_page_${safe.ifEmpty { "page" }}.json")
        f.writeText(AppJson.encodeToString(PageModel.serializer(), page))
        return f
    }

    /**
     * Reads an archive, a bare page list, or a single shared page, without
     * applying it, and refuses anything that would leave the book in a state
     * the app cannot show. Restoring is destructive: check first, change after.
     */
    fun read(text: String): Pair<BookArchive, Summary> {
        val archive = try {
            AppJson.decodeFromString(BookArchive.serializer(), text)
        } catch (e: Exception) {
            try {
                BookArchive(createdAt = iso(), pages = AppJson.decodeFromString(pageListSerializer, text))
            } catch (e2: Exception) {
                try {
                    BookArchive(createdAt = iso(), pages = listOf(AppJson.decodeFromString(PageModel.serializer(), text)))
                } catch (e3: Exception) {
                    throw IllegalArgumentException("That file could not be read as a Talk Tiles book.")
                }
            }
        }
        validate(archive)?.let { throw IllegalArgumentException(it) }
        val summary = Summary(
            pages = archive.pages.size,
            buttons = archive.pages.sumOf { it.tiles.size },
            hotspots = archive.pages.sumOf { it.hotspots.size },
            savedButtons = archive.savedTiles?.size ?: 0,
            phrases = archive.phrases?.size ?: 0,
            createdAt = archive.createdAt
        )
        return archive to summary
    }

    /** The first thing wrong with an archive, in plain words, or null when it is sound. */
    fun validate(archive: BookArchive): String? {
        if (archive.version > FORMAT_VERSION) return "That backup was made by a newer Talk Tiles. Update the app to restore it."
        if (archive.pages.isEmpty()) return "That file does not have any pages in it."
        val ids = HashSet<String>()
        for ((n, p) in archive.pages.withIndex()) {
            val where = "Page ${n + 1}" + if (p.title.isNotBlank()) " (\"${p.title}\")" else ""
            if (p.id.isBlank()) return "$where has no id."
            if (!ids.add(p.id)) return "$where has the same id as an earlier page."
            if (p.gridSize < 1 || p.gridSize > 400) return "$where has a grid size of ${p.gridSize}."
            if (p.tiles.keys.any { it < 1 }) return "$where has a button in slot 0 or below."
            for (h in p.hotspots) {
                val inRange = h.x in 0.0..100.0 && h.y in 0.0..100.0 && h.w > 0.0 && h.w <= 100.0 && h.h > 0.0 && h.h <= 100.0
                if (!inRange) return "$where has a talking spot outside the picture."
            }
            val keys = p.keyboardKeys
            if (keys != null && keys < 1) return "$where has a keyboard with no keys."
        }
        val savedIds = HashSet<String>()
        archive.savedTiles?.forEach { if (!savedIds.add(it.id)) return "Two saved buttons share the id ${it.id}." }
        val phraseIds = HashSet<String>()
        archive.phrases?.forEach { if (!phraseIds.add(it.id)) return "Two saved phrases share the id ${it.id}." }
        return null
    }
}
