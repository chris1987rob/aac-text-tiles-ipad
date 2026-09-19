# Talk Tiles for Android

A native Kotlin / Jetpack Compose AAC app, sharing its book format with the
SwiftUI iPad app in `../AACTextTilesSwiftUI`: Standard Grid / Visual Scene /
Symbol Keyboard pages, the button editor with Saved Buttons, both picture sets
(Talk Tiles pictures + Mulberry), Bella's recorded voice with device-TTS
fallback, touch access, editing protection, whole-book backup and page sharing.

**The book format is the iPad's** (Swift `Codable` JSON: base64 `Data`,
Int-keyed dictionaries as flat arrays, raw-string enums). A backup made on the
iPad restores here and a page shared from here opens there. Fields this app
adds (saved phrases, last page, display preferences) are optional and the iPad
ignores them.

## What is on the screen (2.0-preview)

- **Home** – *Start talking / Continue talking* is the one strong action.
  *Edit pages*, *Page library*, *Settings* and *Help* sit under it.
- **Talking** – top bar: Home · Previous page · Next page · page name with
  "2 of 7" · Find. Pages switched off in the editor are skipped. Find searches
  page names and every button, talking spot and keyboard word; picking a
  result opens the page without speaking.
- **Sentence bar** – one bar for grids and the keyboard: the words in order
  (a button's own recording plays as itself), Remove last word, Saved phrases,
  Speak / Stop, Clear with Undo. It survives page turns.
- **Editing** – the same board with editing on. Previous/Next see every page;
  the page name opens the page list (switch on/off, move, delete); Page
  options; New page.
- **Protect editing** – an optional PIN on Edit pages, Page library and
  Settings. Talking is never behind it.
- **Settings** – voice + speed (Bella or device voices, with preview),
  pictures set, touch (hold-to-speak with a progress ring, pause before
  repeat, speak when the finger lifts), display (high contrast, reduce
  motion, open on last page), protect editing, keep on screen (Android screen
  pinning, soft), backup (save to Files / share / restore with a pre-restore
  snapshot), start over.

Not in this build: an in-app camera (the system camera is used), photo
background removal, eye-gaze or switch scanning.

## Build, test, install

```bash
cd TalkTilesAndroid
./gradlew testDebugUnitTest          # JUnit + Robolectric + Compose semantics tests
./gradlew assembleRelease bundleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

- `applicationId` **`com.talktiles.app`** (the Play id; the code package stays
  `com.talktiles.tablet`), minSdk 26, targetSdk 36, versionCode 4 /
  versionName 2.0-preview.
- Release builds are signed with the Talk Tiles key (`~/aac-board/aac.keystore`,
  alias `aacboard`; password from `KS_PASS` or the gitignored `.keystore-pass`
  beside it). **Without the key the release tasks fail** rather than falling
  back to the debug key.
- `app/src/main/assets/{TalkTilesSymbols,Voices,Symbols}` are **symlinks into
  `../AACTextTilesSwiftUI`**; run `python3 ../Tools/sync-native-assets.py` to
  refresh them.
- Tested on the tablet the app ships on: TCL 9185W, Android 15, 800x1280 @240dpi.

## Layout

| File | What |
|---|---|
| `Models.kt` | PageModel / TileModel / HotspotModel / AppSettings / SavedTile / BookArchive + the Swift-compatible serializers |
| `Sentence.kt` | SentenceItem (label + what it says + recording), SentenceBuilder (remove-last, clear/undo), SavedPhrase |
| `Navigation.kt` | Stepping through the book honouring `enabled`, position, jumps, start page |
| `VocabularySearch.kt` | Offline search over tiles, talking spots and keyboard words |
| `BookStorage.kt` | The files on disk: generation-ordered writes, unreadable files kept aside, snapshots |
| `Store.kt` | AACStore, TileFavorites, PhraseLibrary, BookBackup (encode / read / validate) |
| `Speech.kt` | SpokenText, VoiceClips, SpeechManager (coordinator with a request-generation guard) over `SpeechBackend`, AndroidSpeechBackend, TouchAccess, AudioRecorder |
| `PressController.kt` | When a press counts: dwell, speak-on-lift, cancelled gestures |
| `Theme.kt` | The design system: palette (normal / high contrast), type, shapes, spacing, accessible buttons |
| `Root.kt` | Home and the one-sheet-at-a-time router with editing protection |
| `Board.kt` / `Keyboard.kt` / `Scene.kt` | Navigation bar, sentence bar, the three page kinds, the accessible tile |
| `Editors.kt` / `Pages.kt` / `Settings.kt` / `FormWidgets.kt` | Every sheet: button editor, symbol picker, talking-spot editor, page options, wizard, Find, saved phrases, library, help, PIN, Settings |
| `Templates.kt` / `WordBank.kt` / `Symbols.kt` | The 28 starter boards, the keyboard vocabulary, the picture sets |

Tests live in `app/src/test`. `MODERNIZATION.md` is the audit and acceptance
checklist for the 2.0 work.
