package com.talktiles.tablet

/**
 * Fills a grid page's empty spaces with pictures that suit the page.
 *
 * Empty spaces appear when a page is made bigger than the buttons it has:
 * Core Words taken from 9 to 16, or the Food board added from the wizard
 * at 36 when it carries 16. The page's topic is read from the pictures
 * already on it (food, feelings, people...), and the gaps get more of the
 * same from our own set - each with its Bella clip - in the page's colours.
 * Nothing already on the page is moved or changed, and no word is repeated.
 */
object PageFill {

    /** Slots with no button, on a grid page. */
    fun emptySlots(page: PageModel): List<Int> =
        if (page.type != PageType.GRID) emptyList() else (1..page.gridSize).filter { it !in page.tiles }

    /** The catalogue picture a button shows or names, if any: ours, a bare id, an emoji, or its word. */
    fun symbolFor(t: TileModel): TalkTilesSymbol? {
        val name = t.symbolName
        if (name != null) {
            SymbolLibrary.talkTilesSymbol(name)?.let { return it }
            TalkTilesCatalog.byId[name]?.let { return it }
            if (SymbolLibrary.isEmoji(name)) TalkTilesCatalog.symbols.firstOrNull { it.emoji == name }?.let { return it }
        }
        val word = SpokenText.normalisedPhrase(t.label)
        if (word.isEmpty()) return null
        return TalkTilesCatalog.symbols.firstOrNull { SpokenText.normalisedPhrase(it.label) == word }
    }

    /** The catalogue category most of the page's buttons belong to; "core" when nothing says. */
    fun topic(page: PageModel): String =
        page.tiles.values.mapNotNull { symbolFor(it)?.category?.takeIf { c -> c.isNotEmpty() } }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "core"

    /**
     * The page with every empty space filled, or the page unchanged when it
     * has no gaps, is not a grid, or has no buttons at all (a blank page the
     * parent means to build by hand stays blank).
     */
    fun fill(page: PageModel): PageModel {
        val slots = emptySlots(page)
        if (slots.isEmpty() || page.tiles.isEmpty() || !TalkTilesCatalog.isAvailable) return page

        val usedWords = HashSet<String>()
        val usedIds = HashSet<String>()
        for (t in page.tiles.values) {
            usedWords.add(SpokenText.normalisedPhrase(t.label))
            symbolFor(t)?.let { usedIds.add(it.id); usedWords.add(SpokenText.normalisedPhrase(it.label)) }
        }
        val cat = topic(page)
        // The topic first, then core words if the topic runs out.
        val pool = TalkTilesCatalog.symbols.filter { it.category == cat } +
            TalkTilesCatalog.symbols.filter { it.category == "core" && cat != "core" }
        val picks = pool.asSequence()
            .filter { it.id !in usedIds && SpokenText.normalisedPhrase(it.label) !in usedWords }
            .distinctBy { SpokenText.normalisedPhrase(it.label) }
            .take(slots.size)
            .toList()

        // New buttons wear the page's usual colours, so they look like they belong.
        val look = page.tiles.values.groupingBy { Triple(it.bgHex, it.borderHex, it.labelHex) }.eachCount()
            .maxByOrNull { it.value }?.key ?: Triple(colourFor(cat), "#CBD5E1", "#1E293B")

        val tiles = LinkedHashMap(page.tiles)
        for ((slot, s) in slots.zip(picks)) {
            tiles[slot] = TileModel(id = slot, label = s.label, tts = s.tts, symbolName = SymbolLibrary.talkTilesName(s.id),
                bgHex = look.first, borderHex = look.second, labelHex = look.third)
        }
        return page.copy(tiles = tiles.toSortedMap())
    }

    /** Fitzgerald key colour for a catalogue category, for a page with nothing to copy. */
    private fun colourFor(category: String): String = when (category) {
        "people", "feelings" -> TemplateColor.person
        "actions" -> TemplateColor.verb
        "concepts", "colors" -> TemplateColor.descriptor
        "social" -> TemplateColor.social
        "core" -> TemplateColor.plain
        else -> TemplateColor.noun
    }
}
