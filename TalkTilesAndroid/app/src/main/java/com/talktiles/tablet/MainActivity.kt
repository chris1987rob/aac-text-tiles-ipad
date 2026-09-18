package com.talktiles.tablet

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.lightColorScheme

class MainActivity : ComponentActivity() {
    private lateinit var store: AACStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Order matters: the store applies the voice to the speech manager as it loads.
        SpeechManager.init(this)
        VoiceClips.init(this)
        TalkTilesCatalog.init(this)
        SymbolLibrary.init(this)
        TileFavorites.init(this)
        store = AACStore(this)

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
            MaterialTheme(colorScheme = lightColorScheme(primary = BoardTheme.green, background = BoardTheme.background)) {
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
        store.saveSettings()
    }
}
