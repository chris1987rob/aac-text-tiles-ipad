package com.talktiles.tablet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File

/** A store on a scratch folder with the app's singletons initialised, for Compose tests. */
object TestBook {
    fun store(dir: File, pages: List<PageModel> = AACStore.defaultPages(), settings: AppSettings = AppSettings(), licence: LicenceState? = null): AACStore {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SpeechManager.init(context)
        VoiceClips.init(context)
        TalkTilesCatalog.init(context)
        SymbolLibrary.init(context)
        val storage = BookStorage(dir)
        storage.writePages(pages, generation = 1)
        storage.writeSettings(settings)
        if (licence != null) storage.licenceFile.writeText(AppJson.encodeToString(LicenceState.serializer(), licence))
        TileFavorites.init(storage)
        PhraseLibrary.init(storage)
        return AACStore(context, storage)
    }
}
