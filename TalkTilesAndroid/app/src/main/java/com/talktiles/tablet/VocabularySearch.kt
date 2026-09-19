package com.talktiles.tablet

/**
 * Offline search over everything the book can say: grid buttons, talking
 * spots and keyboard words. A hit says where the word lives so the reader can
 * be taken to that page. Searching never speaks.
 */
object VocabularySearch {

    enum class Kind { TILE, HOTSPOT, KEY }

    data class Hit(
        val pageId: String,
        val pageIndex: Int,
        val pageTitle: String,
        val label: String,
        val spoken: String,
        val kind: Kind,
        val symbolName: String? = null,
        val photoData: ByteArray? = null,
        /** Keyboard hits also need the word's group so the page can open on it. */
        val keyGroupId: String? = null
    )

    private fun norm(s: String) = SpokenText.normalisedPhrase(s)

    /** Words, label matches ahead of spoken-phrase matches, in book order within each. */
    fun search(pages: List<PageModel>, query: String, editing: Boolean): List<Hit> {
        val q = norm(query)
        if (q.isEmpty()) return emptyList()
        val byLabel = ArrayList<Hit>()
        val bySpoken = ArrayList<Hit>()
        fun offer(hit: Hit) {
            val label = norm(hit.label)
            val spoken = norm(hit.spoken)
            when {
                label.isEmpty() && spoken.isEmpty() -> {}
                label.contains(q) -> byLabel.add(hit)
                spoken.contains(q) -> bySpoken.add(hit)
            }
        }
        pages.forEachIndexed { index, page ->
            if (!editing && !page.enabled) return@forEachIndexed
            when (page.type) {
                PageType.GRID -> for ((slot, t) in page.tiles.toSortedMap()) {
                    if (slot > page.gridSize) continue
                    offer(Hit(page.id, index, page.title, t.label, t.spoken, Kind.TILE, t.symbolName, t.photoData))
                }
                PageType.SCENE -> for (h in page.hotspots) {
                    offer(Hit(page.id, index, page.title, h.label, h.spoken, Kind.HOTSPOT))
                }
                PageType.KEYBOARD -> for (group in SymbolWordBank.groups(page.keyboardGroups)) {
                    for (word in group.words) {
                        val edit = page.keyboardEdits?.get(word.id)
                        if (edit?.hidden == true) continue
                        val w = word.applying(edit)
                        offer(Hit(page.id, index, page.title, w.label, w.tts, Kind.KEY, w.icon, w.photoData, group.id))
                    }
                }
            }
        }
        return byLabel + bySpoken
    }

    /** Pages whose title contains the query; every reachable page for a blank query. */
    fun pages(pages: List<PageModel>, query: String, editing: Boolean): List<PageModel> {
        val q = norm(query)
        return pages.filter { (editing || it.enabled) && (q.isEmpty() || norm(it.title).contains(q)) }
    }
}
