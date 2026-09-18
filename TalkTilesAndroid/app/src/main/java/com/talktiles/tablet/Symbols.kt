package com.talktiles.tablet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

/** One of our own pictures, as listed in `TalkTilesSymbols/catalog.json`. */
@Serializable
data class TalkTilesSymbol(
    val id: String,
    val label: String,
    val tts: String,
    val category: String = "",
    val tags: List<String> = emptyList(),
    val emoji: String = ""
)

/** The Talk Tiles picture set: 2,210 pictures drawn in-house, each with a Bella clip. */
object TalkTilesCatalog {
    const val FOLDER = "TalkTilesSymbols"
    private lateinit var app: Context

    fun init(context: Context) { app = context.applicationContext }

    val symbols: List<TalkTilesSymbol> by lazy {
        try {
            app.assets.open("$FOLDER/catalog.json").bufferedReader().use {
                AppJson.decodeFromString(ListSerializer(TalkTilesSymbol.serializer()), it.readText())
            }
        } catch (e: Exception) { emptyList() }
    }

    val isAvailable: Boolean get() = symbols.isNotEmpty()

    val byId: Map<String, TalkTilesSymbol> by lazy { symbols.associateBy { it.id } }

    val categories: List<String> by lazy {
        val seen = LinkedHashSet<String>()
        for (s in symbols) if (s.category.isNotEmpty()) seen.add(s.category)
        seen.toList()
    }

    fun categoryTitle(category: String): String =
        category.replace('_', ' ').split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

    /** Ranked search over label, spoken text and tags; exact and prefix label matches first. */
    fun search(query: String, category: String? = null, limit: Int = 400): List<TalkTilesSymbol> {
        val q = SpokenText.normalisedPhrase(query)
        val pool = if (category == null) symbols else symbols.filter { it.category == category }
        if (q.isEmpty()) return pool.take(limit)
        val exact = ArrayList<TalkTilesSymbol>(); val prefix = ArrayList<TalkTilesSymbol>()
        val word = ArrayList<TalkTilesSymbol>(); val tag = ArrayList<TalkTilesSymbol>(); val loose = ArrayList<TalkTilesSymbol>()
        for (s in pool) {
            val label = SpokenText.normalisedPhrase(s.label)
            val tts = SpokenText.normalisedPhrase(s.tts)
            when {
                label == q || tts == q -> exact.add(s)
                label.startsWith(q) || tts.startsWith(q) -> prefix.add(s)
                label.split(' ').any { it.startsWith(q) } -> word.add(s)
                s.tags.any { it.lowercase().startsWith(q) } -> tag.add(s)
                label.contains(q) || tts.contains(q) -> loose.add(s)
            }
            if (exact.size + prefix.size + word.size + tag.size + loose.size >= limit * 2) break
        }
        return (exact + prefix + word + tag + loose).take(limit)
    }
}

/**
 * The picture symbols shipped inside the app - two sets behind one name.
 * A tile stores one name string: `tt:<id>` for one of ours, the bare file
 * name for a Mulberry symbol, or an emoji.
 */
object SymbolLibrary {
    const val ATTRIBUTION = "Mulberry Symbols © Garry Paxton and Steve Lee, licensed CC BY-SA 2.0 UK."
    const val TALK_TILES_PREFIX = "tt:"

    private lateinit var app: Context
    fun init(context: Context) { app = context.applicationContext }

    private val cache = object : LruCache<String, Bitmap>(64 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /** Every bundled Mulberry symbol name, sorted. */
    val names: List<String> by lazy {
        try {
            (app.assets.list("Symbols") ?: emptyArray())
                .filter { it.endsWith(".png", ignoreCase = true) }
                .map { it.removeSuffix(".png").removeSuffix(".PNG") }
                .sorted()
        } catch (e: Exception) { emptyList() }
    }

    val isAvailable: Boolean get() = names.isNotEmpty()

    fun has(set: SymbolSet): Boolean = when (set) {
        SymbolSet.TALK_TILES -> TalkTilesCatalog.isAvailable
        SymbolSet.MULBERRY -> isAvailable
    }

    fun count(set: SymbolSet): Int = when (set) {
        SymbolSet.TALK_TILES -> TalkTilesCatalog.symbols.size
        SymbolSet.MULBERRY -> names.size
    }

    val anyAvailable: Boolean get() = SymbolSet.values().any { has(it) }

    fun talkTilesName(id: String) = TALK_TILES_PREFIX + id

    fun talkTilesSymbol(name: String): TalkTilesSymbol? {
        if (!name.startsWith(TALK_TILES_PREFIX)) return null
        return TalkTilesCatalog.byId[name.removePrefix(TALK_TILES_PREFIX)]
    }

    /** True for a name that is an emoji rather than a library picture. */
    fun isEmoji(raw: String?): Boolean {
        if (raw.isNullOrEmpty()) return false
        var i = 0
        while (i < raw.length) {
            val cp = raw.codePointAt(i)
            i += Character.charCount(cp)
            if (cp > 0x238C && (cp >= 0x1F000 || cp in 0x2600..0x27BF || cp in 0x2300..0x23FF || cp in 0x2B00..0x2BFF || cp == 0x3030 || cp == 0x303D || cp in 0x2190..0x21FF)) return true
        }
        return false
    }

    /** The picture behind a stored name, or null. Decoded once and kept. */
    fun image(name: String): Bitmap? {
        cache.get(name)?.let { return it }
        val path = if (name.startsWith(TALK_TILES_PREFIX))
            "${TalkTilesCatalog.FOLDER}/${name.removePrefix(TALK_TILES_PREFIX)}.webp"
        else "Symbols/$name.png"
        val bmp = try {
            app.assets.open(path).use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) { null } ?: return null
        cache.put(name, bmp)
        return bmp
    }

    fun readable(name: String): String {
        talkTilesSymbol(name)?.let { return it.label }
        return name.replace("_,_", " ").replace('_', ' ').trim()
    }

    fun search(query: String, set: SymbolSet, category: String? = null, limit: Int = 300): List<String> = when (set) {
        SymbolSet.TALK_TILES -> TalkTilesCatalog.search(query, category, limit).map { talkTilesName(it.id) }
        SymbolSet.MULBERRY -> searchMulberry(query, limit)
    }

    private fun searchMulberry(query: String, limit: Int): List<String> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return names.take(limit)
        val exact = ArrayList<String>(); val prefix = ArrayList<String>(); val word = ArrayList<String>(); val loose = ArrayList<String>()
        for (name in names) {
            val r = readable(name).lowercase()
            when {
                r == q -> exact.add(name)
                r.startsWith(q) -> prefix.add(name)
                r.split(' ').any { it.startsWith(q) } -> word.add(name)
                r.contains(q) -> loose.add(name)
            }
            if (exact.size + prefix.size + word.size + loose.size >= limit * 2) break
        }
        return (exact + prefix + word + loose).take(limit)
    }
}

/** Photos live in the model as JPEG bytes; decode each once, not on every frame. */
object PhotoCache {
    private val cache = object : LruCache<String, Bitmap>(96 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun bitmap(data: ByteArray?, key: String): Bitmap? {
        if (data == null) return null
        val k = "$key-${data.size}-${data.contentHashCode()}"
        cache.get(k)?.let { return it }
        val bmp = try { BitmapFactory.decodeByteArray(data, 0, data.size) } catch (e: Exception) { null } ?: return null
        cache.put(k, bmp)
        return bmp
    }
}
