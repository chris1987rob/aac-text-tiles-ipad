package com.talktiles.tablet

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.provider.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity

class MainActivity : ComponentActivity() {
    private lateinit var store: AACStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Order matters: the store applies the voice to the speech manager as it loads.
        SpeechManager.init(this)
        VoiceClips.init(this)
        TalkTilesCatalog.init(this)
        SymbolLibrary.init(this)
        val storage = BookStorage(filesDir)
        TileFavorites.init(storage)
        PhraseLibrary.init(storage)
        store = AACStore(this, storage)

        // Keep the screen awake during communication.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // The root window's insets, not the composition's: the activity fits
        // system windows, so by the time Compose looks the inset is spent.
        val navBottomPx = mutableStateOf(0)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            navBottomPx.value = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            ViewCompat.onApplyWindowInsets(v, insets)
        }

        setContent {
            val density = LocalDensity.current
            val navBottom = with(density) { navBottomPx.value.toDp() }
            // The person's display choices, plus the system's own "no animations" setting.
            val systemReducesMotion = remember {
                try { Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f } catch (e: Exception) { false }
            }
            TalkTilesTheme(highContrast = store.settings.highContrast, reduceMotion = store.settings.reduceMotion || systemReducesMotion) {
                CompositionLocalProvider(LocalNavBarBottom provides navBottom) {
                    RootView(store)
                }
            }
        }
    }

    /** An edit made in the last half-second before the app is swiped away must not be lost. */
    override fun onPause() {
        super.onPause()
        store.saveNow()
    }
}
