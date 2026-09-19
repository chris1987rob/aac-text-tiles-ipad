package com.talktiles.tablet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Decides when a press on a button counts, from the two touch-access settings:
 *
 * - `activationDelay` > 0 is dwell: the finger has to stay put that long.
 * - `activateOnRelease` fires when the finger lifts, so a child can slide onto
 *   the right button before committing. With dwell as well, the hold must
 *   complete AND the lift must happen.
 *
 * A release that did not complete (the gesture was cancelled - the finger
 * slid off, a scroll took over) never fires. One gesture fires at most once.
 * `activate()` is the accessibility/keyboard path: one fire, no gesture.
 */
class PressController(
    val activationDelay: Double,
    val activateOnRelease: Boolean,
    private val onFire: () -> Unit
) {
    var isPressed by mutableStateOf(false)
        private set

    /** The dwell has completed and the button is waiting for the lift. */
    var isArmed by mutableStateOf(false)
        private set

    private var firedThisGesture = false

    val dwellMs: Long get() = if (activationDelay > 0) (activationDelay * 1000).toLong() else 0L

    /** The finger landed. Returns the dwell to schedule, or null for none. */
    fun press(): Long? {
        if (isPressed) return null
        isPressed = true
        isArmed = false
        firedThisGesture = false
        if (dwellMs > 0) return dwellMs
        if (!activateOnRelease) fire()
        return null
    }

    /** The scheduled dwell ran out while (hopefully) still pressed. */
    fun dwellElapsed() {
        if (!isPressed || dwellMs == 0L) return
        if (activateOnRelease) isArmed = true else fire()
    }

    /** The finger lifted; `completed` is false when the gesture was cancelled. */
    fun release(completed: Boolean) {
        if (!isPressed) return
        val shouldFire = completed && activateOnRelease && (dwellMs == 0L || isArmed)
        isPressed = false
        isArmed = false
        if (shouldFire) fire()
    }

    /** Keyboard, switch or screen-reader activation: counts once, straight away. */
    fun activate() {
        firedThisGesture = false
        fire()
    }

    private fun fire() {
        if (firedThisGesture) return
        firedThisGesture = true
        onFire()
    }
}
