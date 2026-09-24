package com.talktiles.tablet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** The free / trial / Pro rules, on a fake clock. */
class ProAccessTest {
    @get:Rule val tmp = TemporaryFolder()

    private var now = 1_000_000_000_000L
    private val day = ProRules.DAY_MS
    private fun file() = File(tmp.root, "aac_licence.json")
    private fun access() = ProAccess(file()) { now }

    private fun book(extra: Int, scenes: Int = 0): List<PageModel> =
        AACStore.defaultPages() + List(extra) { PageModel(title = "Mine $it") } + List(scenes) { PageModel(title = "Scene $it", type = PageType.SCENE) }

    @Test
    fun aNewInstallGetsFourteenDaysOfPro() {
        val a = access()
        assertTrue(a.inTrial)
        assertEquals(14, a.trialDaysLeft)
        assertNull(a.blockAddingPage(book(40)))
        assertNull(a.blockSavingButton())
    }

    @Test
    fun theTrialCountsFromFirstLaunchNotFromEachLaunch() {
        access()
        now += 10 * day
        val later = access()
        assertEquals(4, later.trialDaysLeft)
        now += 4 * day
        val ended = access()
        assertFalse(ended.inTrial)
        assertEquals(0, ended.trialDaysLeft)
        assertEquals("Free version", ended.statusLine)
    }

    @Test
    fun theLastAfternoonStillSaysOneDay() {
        access()
        now += 13 * day + day / 2
        assertEquals(1, access().trialDaysLeft)
    }

    @Test
    fun turningTheClockBackDoesNotRestartTheTrial() {
        access()
        now += 20 * day
        access()                 // the app saw day 20
        now -= 15 * day          // clock set back to day 5
        assertFalse(access().inTrial)
    }

    @Test
    fun afterTheTrialTheStarterBookPlusFivePagesIsTheLimit() {
        now += 0; access(); now += 15 * day
        val a = access()
        assertNull(a.blockAddingPage(book(4)))
        assertEquals(ProBlock.PAGES, a.blockAddingPage(book(5)))
        // A book already over the limit (built during the trial) is refused more, never trimmed.
        assertEquals(ProBlock.PAGES, a.blockAddingPage(book(12)))
    }

    @Test
    fun afterTheTrialOneSceneIsFree() {
        access(); now += 15 * day
        val a = access()
        assertNull(a.blockAddingPage(book(0), PageType.SCENE))
        assertEquals(ProBlock.SCENES, a.blockAddingPage(book(0, scenes = 1), PageType.SCENE))
        // A grid page is still fine while pages are left.
        assertNull(a.blockAddingPage(book(0, scenes = 1), PageType.GRID))
        assertEquals(ProBlock.SAVED_BUTTONS, a.blockSavingButton())
    }

    @Test
    fun proLiftsEveryLimitAndSurvivesARestart() {
        access(); now += 30 * day
        access().grantPro("GPA.1234")
        val a = access()
        assertTrue(a.isPro)
        assertFalse(a.inTrial)
        assertNull(a.blockAddingPage(book(50, scenes = 9), PageType.SCENE))
        assertNull(a.blockSavingButton())
    }

    @Test
    fun aRefundTakesProAwayButTheLimitOnlyStopsAddingMore() {
        access(); now += 30 * day
        val a = access()
        a.grantPro("GPA.1")
        a.revokePro()
        assertFalse(access().isPro)
        assertEquals(ProBlock.PAGES, a.blockAddingPage(book(9)))
    }

    @Test
    fun anUnreadableLicenceFileStartsAFreshTrialRatherThanCrashing() {
        file().writeText("{not json")
        assertTrue(access().inTrial)
    }
}
