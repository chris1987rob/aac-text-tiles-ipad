package com.talktiles.tablet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Stepping through the book in the player honours `enabled`; the editor sees everything. */
class BookNavigationTest {

    private fun page(id: String, enabled: Boolean = true) = PageModel(id = id, title = id, enabled = enabled)

    private val book = listOf(page("A"), page("B", enabled = false), page("C"), page("D", enabled = false), page("E"))

    @Test
    fun playerNextSkipsDisabledPagesAndWraps() {
        assertEquals(2, BookNavigation.step(book, 0, +1, editing = false))   // A -> C
        assertEquals(4, BookNavigation.step(book, 2, +1, editing = false))   // C -> E
        assertEquals(0, BookNavigation.step(book, 4, +1, editing = false))   // E -> A (wrap)
    }

    @Test
    fun playerPreviousSkipsDisabledPagesAndWraps() {
        assertEquals(4, BookNavigation.step(book, 0, -1, editing = false))   // A -> E
        assertEquals(2, BookNavigation.step(book, 4, -1, editing = false))   // E -> C
    }

    @Test
    fun editorStepsThroughEveryPage() {
        assertEquals(1, BookNavigation.step(book, 0, +1, editing = true))
        assertEquals(3, BookNavigation.step(book, 2, +1, editing = true))
        assertEquals(4, BookNavigation.step(book, 0, -1, editing = true))
    }

    @Test
    fun steppingFromADisabledPageLandsOnTheNextEnabledOne() {
        // The parent turned off the page the child was on: forward goes to C, back goes to A.
        assertEquals(2, BookNavigation.step(book, 1, +1, editing = false))
        assertEquals(0, BookNavigation.step(book, 1, -1, editing = false))
    }

    @Test
    fun allPagesDisabledStaysPutWithoutLosingAnything() {
        val dark = book.map { it.copy(enabled = false) }
        assertEquals(3, BookNavigation.step(dark, 3, +1, editing = false))
        assertEquals(3, BookNavigation.step(dark, 3, -1, editing = false))
        assertEquals(5, dark.size)
    }

    @Test
    fun emptyBookStepsToZero() {
        assertEquals(0, BookNavigation.step(emptyList(), 0, +1, editing = false))
    }

    @Test
    fun positionCountsOnlyWhatThePlayerCanReach() {
        assertEquals(BookNavigation.Position(1, 3), BookNavigation.position(book, 0, editing = false))
        assertEquals(BookNavigation.Position(2, 3), BookNavigation.position(book, 2, editing = false))
        assertEquals(BookNavigation.Position(3, 3), BookNavigation.position(book, 4, editing = false))
        // On a disabled page (just switched off under you) the position is unknown but the total still counts.
        assertEquals(BookNavigation.Position(0, 3), BookNavigation.position(book, 1, editing = false))
        assertEquals(BookNavigation.Position(4, 5), BookNavigation.position(book, 3, editing = true))
    }

    @Test
    fun jumpsToDisabledPagesAreRefusedInThePlayer() {
        assertEquals(2, BookNavigation.indexForJump(book, "C", editing = false))
        assertNull(BookNavigation.indexForJump(book, "B", editing = false))
        assertEquals(1, BookNavigation.indexForJump(book, "B", editing = true))
        assertNull(BookNavigation.indexForJump(book, "nope", editing = true))
    }

    @Test
    fun startIndexPrefersTheRememberedPageThenTheFirstEnabledOne() {
        assertEquals(2, BookNavigation.startIndex(book, rememberedId = "C"))
        assertEquals(0, BookNavigation.startIndex(book, rememberedId = "B"))      // remembered page now off
        assertEquals(0, BookNavigation.startIndex(book, rememberedId = null))
        val firstOff = listOf(page("A", enabled = false), page("B"))
        assertEquals(1, BookNavigation.startIndex(firstOff, rememberedId = null))
        assertEquals(0, BookNavigation.startIndex(firstOff.map { it.copy(enabled = false) }, rememberedId = null))
        assertEquals(0, BookNavigation.startIndex(emptyList(), rememberedId = "A"))
    }

    @Test
    fun hasMoreThanOnePageToShowDrivesTheArrows() {
        assertTrue(BookNavigation.canStep(book, editing = false))
        assertFalse(BookNavigation.canStep(listOf(page("A"), page("B", enabled = false)), editing = false))
        assertTrue(BookNavigation.canStep(listOf(page("A"), page("B", enabled = false)), editing = true))
    }
}
