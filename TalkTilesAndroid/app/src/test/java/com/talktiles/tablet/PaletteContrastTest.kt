package com.talktiles.tablet

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Every colour the app puts text in, on every surface it puts it on, meets
 * WCAG AA for normal text (4.5:1) - in the NORMAL palette, so nobody has to
 * find the high-contrast switch to read a hint or a PIN note.
 */
class PaletteContrastTest {

    private fun lum(c: Color): Double {
        fun f(x: Float): Double { val v = x.toDouble(); return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4) }
        return 0.2126 * f(c.red) + 0.7152 * f(c.green) + 0.0722 * f(c.blue)
    }
    private fun ratio(a: Color, b: Color): Double {
        val la = lum(a); val lb = lum(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun check(p: Palette, name: String) {
        val surfaces = mapOf("canvas" to p.canvas, "surface" to p.surface, "surfaceSunken" to p.surfaceSunken,
            "sentence" to p.sentence, "primarySoft" to p.primarySoft, "accentSoft" to p.accentSoft, "dangerSoft" to p.dangerSoft)
        val inks = mapOf("ink" to p.ink, "inkSoft" to p.inkSoft, "inkFaint" to p.inkFaint, "primary" to p.primary,
            "primaryDeep" to p.primaryDeep, "accent" to p.accent, "danger" to p.danger, "success" to p.success)
        val problems = ArrayList<String>()
        for ((sn, s) in surfaces) for ((inName, i) in inks) {
            val r = ratio(i, s)
            if (r < 4.5) problems.add("$name: $inName on $sn = ${"%.2f".format(r)}")
        }
        // Text drawn in onPrimary on the filled buttons.
        for ((fn, f) in mapOf("primary" to p.primary, "primaryDeep" to p.primaryDeep, "accent" to p.accent, "danger" to p.danger, "success" to p.success, "pressed" to p.pressed)) {
            val r = ratio(p.onPrimary, f)
            if (r < 4.5) problems.add("$name: onPrimary on $fn = ${"%.2f".format(r)}")
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test fun normalPaletteIsAAForText() = check(Palette.normal, "normal")
    @Test fun highContrastPaletteIsAAForText() = check(Palette.highContrast, "highContrast")

    @Test
    fun linesAreVisibleAgainstTheirSurfaces() {
        // Non-text UI components need 3:1 (WCAG 1.4.11). The strong line is used for focus/dashed outlines.
        for (p in listOf(Palette.normal, Palette.highContrast)) {
            assertTrue(ratio(p.lineStrong, p.surface) >= 3.0)
            assertTrue(ratio(p.focus, p.surface) >= 3.0)
        }
    }
}
