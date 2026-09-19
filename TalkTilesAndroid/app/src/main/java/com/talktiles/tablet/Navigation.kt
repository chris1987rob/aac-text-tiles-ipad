package com.talktiles.tablet

/**
 * Moving through the book. The player only ever lands on pages the parent
 * left switched on; the editor sees every page so a switched-off one can be
 * found and switched back. Nothing here mutates the book.
 */
object BookNavigation {

    /** "Page 2 of 5" - `index` is 0 when the current page is not one the reader can reach. */
    data class Position(val index: Int, val total: Int)

    private fun reachable(pages: List<PageModel>, editing: Boolean): List<Int> =
        pages.indices.filter { editing || pages[it].enabled }

    /**
     * The page `delta` steps away in the reading direction, wrapping at either
     * end. Stepping from a page that is now disabled lands on the nearest
     * enabled page in that direction. With nothing reachable, stays put.
     */
    fun step(pages: List<PageModel>, current: Int, delta: Int, editing: Boolean): Int {
        if (pages.isEmpty()) return 0
        val reach = reachable(pages, editing)
        if (reach.isEmpty()) return current.coerceIn(0, pages.size - 1)
        val at = reach.indexOf(current)
        if (at >= 0) {
            val n = reach.size
            return reach[((at + delta) % n + n) % n]
        }
        // Current page is not reachable: the nearest reachable page in the direction of travel.
        return if (delta >= 0) reach.firstOrNull { it > current } ?: reach.first()
        else reach.lastOrNull { it < current } ?: reach.last()
    }

    fun position(pages: List<PageModel>, current: Int, editing: Boolean): Position {
        val reach = reachable(pages, editing)
        return Position(reach.indexOf(current) + 1, reach.size)
    }

    /** More than one page to move between, so the arrows mean something. */
    fun canStep(pages: List<PageModel>, editing: Boolean): Boolean = reachable(pages, editing).size > 1

    /** Where a hotspot or search result may take the reader; null refuses the jump. */
    fun indexForJump(pages: List<PageModel>, pageId: String?, editing: Boolean): Int? {
        if (pageId == null) return null
        val idx = pages.indexOfFirst { it.id == pageId }
        if (idx < 0) return null
        return if (editing || pages[idx].enabled) idx else null
    }

    /** The page to open on: the one remembered from last time if it is still reachable, else the first reachable one, else 0. */
    fun startIndex(pages: List<PageModel>, rememberedId: String?): Int {
        if (pages.isEmpty()) return 0
        indexForJump(pages, rememberedId, editing = false)?.let { return it }
        return reachable(pages, editing = false).firstOrNull() ?: 0
    }
}
