# Talk Tiles for Android

A native Kotlin / Jetpack Compose port of the SwiftUI iPad app in
`../AACTextTilesSwiftUI`, screen for screen: the pastel board, the Player and
Page Editor, Standard Grid / Visual Scene / Symbol Keyboard pages, the button
editor with Saved Buttons, both picture sets (Talk Tiles pictures + Mulberry),
Bella's recorded voice with device-TTS fallback, touch access, child lock,
whole-book backup and page sharing.

**The book format is the iPad's** (Swift `Codable` JSON: base64 `Data`,
Int-keyed dictionaries as flat arrays, raw-string enums). A backup made on the
iPad restores here and a page shared from here opens there.

## Build and install

```bash
cd TalkTilesAndroid
./gradlew assembleRelease            # app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk
```

- Signed with the Talk Tiles key (`~/aac-board/aac.keystore`, alias `aacboard`);
  the password comes from `KS_PASS` or the gitignored `.keystore-pass` beside
  the keystore, never from this tree. Without a key the debug key is used.
- Package `com.talktiles.tablet`, minSdk 26, targetSdk 34. Tested on an
  Alcatel 9185W (Android 15, 800x1280).
- `app/src/main/assets/{TalkTilesSymbols,Voices,Symbols}` are **symlinks into
  `../AACTextTilesSwiftUI`**, so both apps ship exactly the same pictures and
  clips; run `python3 ../Tools/sync-native-assets.py` to refresh them.

## Layout

| File | What |
|---|---|
| `Models.kt` | PageModel / TileModel / HotspotModel / AppSettings / SavedTile + the Swift-compatible serializers |
| `Store.kt` | AACStore (pages, settings, debounced save), TileFavorites, BookBackup |
| `Templates.kt` | The 28 starter boards, transcribed from `PageTemplateCatalog.swift` by script |
| `WordBank.kt` | The symbol keyboard's vocabulary, derived from the templates |
| `Symbols.kt` | TalkTilesCatalog, SymbolLibrary (`tt:` prefix = ours), PhotoCache |
| `Speech.kt` | SpokenText, VoiceClips, SpeechManager (clip → chained clips → TTS), TouchAccess, AudioRecorder |
| `Theme.kt` / `FormWidgets.kt` | BoardTheme colours, bar buttons, sentence pill, the sheet + form building blocks |
| `Board.kt` / `Keyboard.kt` / `Scene.kt` | The three page kinds and the navigation bar |
| `Editors.kt` / `Pages.kt` / `Settings.kt` | Every sheet: Quick Edit, symbol picker, hotspot editor, Page Options, wizard, pages, gallery, help, PIN, Settings |
| `Root.kt` | Home menu and the one-sheet-at-a-time router |
