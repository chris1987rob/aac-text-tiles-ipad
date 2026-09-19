# Talk Tiles for Android — modernization audit and acceptance checklist

Base: commit `919137a6` (v1.2, versionCode 3, `com.talktiles.app`). Sixteen Kotlin files, 5,003 lines, no test source set, no test dependencies. Asset folders are symlinks into `../AACTextTilesSwiftUI` and resolve in this worktree (Symbols 3,437 · TalkTilesSymbols 2,211 · Voices/bella 2,212).

This file is the audit that scoped the work and the checklist the build is judged by. Everything ticked here has a test or a recorded build behind it; anything not ticked is not claimed.

## 1. Audit — what the v1.2 code actually does

### Shell / design
- `Root.kt` HomeView: orange gradient "PLAYER" card, four saturated colour cards, a rainbow band under the title. Fixed heights (130dp / 110dp), `sp` sizes that clip at large font scale, no landscape variant.
- `Theme.kt` BoardTheme is the iPad's pastel palette (orange accent, rainbow ring on the Play button). `plainClickable` suppresses all indication, so keyboard/D-pad focus is invisible everywhere.
- Navigation bar (`Board.kt`) is 64dp with the title in ALL CAPS and a size table keyed on character count; side paddings are fixed dp so a long title overlaps the buttons on 533dp portrait when the font is enlarged.
- Help text refers to "Player", "Page Editor", "Child Lock", "Downloads" and to Android's own screen pinning; README says package `com.talktiles.tablet`, targetSdk 34 (both stale).

### Navigation
- `AACStore.step()` wraps over **every** page; `PageModel.enabled` is written by the model but never read by the player. A page the parent turned off still appears while stepping.
- The player bar has **Back (= previous page)** and **Home** only — there is no "next page" control; the only way forward is the page list behind the title.
- The page list (`PagesListSheet`) is not searchable and there is no vocabulary search at all.
- Nothing remembers the last page; the app opens on index 0 each launch.
- Hotspot `JUMP` uses the raw index of the target page even if that page is disabled.

### Editing protection ("child lock")
- `requestEditor()` is the only PIN gate. From Home, **Settings** (which contains the "Lock editing" switch, Change PIN, Restore, Reset to starter book) and **Downloads** (which adds pages) open without a PIN. The lock can therefore be switched off by the person it is meant to keep out.
- The PIN prompt is a full sheet titled "Locked".

### Sentence builder
- `expressChips` is `List<String>` of **labels**. `playExpressSentence()` joins labels and re-synthesises them, so a tile whose `tts` differs from its label ("Eat" → "Eat food") or that carries a recording is spoken wrongly in the sentence.
- Grid bar has speak + clear only (no remove-last, no undo, no stop). Keyboard page keeps its own `sentence` list inside the composable — different controls, and it is lost when the page changes.
- Clear is immediate and unrecoverable.

### Touch / accessibility
- `TileView` (player mode) is a raw `pointerInput` with `detectTapGestures`; **no semantics** — TalkBack reads nothing, there is no role, no click action, no keyboard activation.
- `tryAwaitRelease()` result is ignored: with "speak when finger lifts", a press that was **cancelled** (finger slid off / drag) still fires.
- Hotspots (`Scene.kt`) are raw `pointerInput` with no semantics either.
- Under-48dp targets: "Remove last word" 36dp, keyboard page arrows 38dp, `ModalSheet` Cancel/Done text (~33dp), list delete icons, `SegmentedPicker` 34dp, `FilterChip` ~30dp, Restore/Cancel text links in Settings.
- Press/hold progress for dwell is not shown.
- No high-contrast or reduced-motion preference; tile press animates scale regardless.

### Speech
- `isSpeaking` is set true and then reset by **one 300ms `postDelayed` check**, not by utterance completion — the flag is wrong for any phrase longer than 300ms.
- `soundItOut` posts syllables on the main handler; `stopAll()` does **not** cancel those callbacks, so a newer press (or any future Stop) is followed by stale syllables.
- No public stop.
- `speakWithEngine` silently returns while TTS is initialising (`!ttsReady`): the first presses after launch on a slow engine are dropped with no feedback.
- `playAudioData` never releases its `MediaPlayer` on completion (only the next `stopAll` does); `startClip` releases in `onError` while `player` still references it.

### Persistence / book safety
- `save()` starts a `Thread` 400ms later with a snapshot; `saveNow()` writes synchronously. `writeToDisk` is `@Synchronized` but there is **no ordering guard**: if `saveNow()` (onPause) takes the lock before an already-started debounce thread does, the older snapshot is written last and wins.
- A malformed `aac_pages.json` loads as `null` → defaults in memory → the **next save overwrites the unreadable file** with the starter book. Same for settings and favourites. Nothing is preserved.
- `BookBackup.write` contains pages + settings only; **saved buttons are not backed up**.
- `read()` parses but only checks "at least one page"; duplicate/blank ids, non-positive grid sizes, out-of-range hotspots are applied as-is.
- Restore replaces the book without keeping the previous one.
- Export is share-sheet only (`ACTION_SEND`); no "save to Files".

### Feature coverage (v1.2 has; must not regress)
Grids 1–48, scene hotspots (TTS / recorded / jump) with drag-move and resize, symbol keyboard with per-page key edits, two picture sets, Bella clips + device voices with preview, camera via system `TakePicture` + library picker + downscale, m4a recording, page options/wizard/gallery/pages list, saved buttons (200), whole-book backup + page share, PIN prompt.

**Gaps noted accurately:** there is no in-app full-screen camera (the system camera app is launched) and no white-background photo clean-up in v1.2. Neither is implemented in this upgrade; no button pretends to.
There is no in-app screen pinning in v1.2 (Help points at Android's own setting).

## 2. Scope of this upgrade (what is implemented)

| # | Slice | Tests |
|---|---|---|
| 0 | Test harness: JUnit4 + Robolectric + Compose UI test on host; `android.util.Base64` → `java.util.Base64` so models are pure-JVM | build log |
| 1 | Design system (`Theme.kt` → `TalkTilesTheme`): teal/blue primary, neutral canvas, ink, type scale, shape scale, spacing, `PrimaryButton`/`SecondaryButton`/`BarButton` with ≥48dp targets, visible focus, reduced-motion aware | Compose semantics |
| 2 | Home redesign: "Start talking / Continue talking" primary, Edit pages · Library · Settings · Help secondary; adaptive column/row layout; large-font safe | Compose |
| 3 | Navigation: prev/next honouring `enabled`, "n of m" position, page chooser with search, vocabulary search that navigates without speaking, last-page memory, all-disabled fallback | JUnit + Compose |
| 4 | Protect editing: one gate for editor, settings, library, restore/reset; "Protect editing" language; speaking never gated | JUnit + Compose |
| 5 | Sentence model: ordered `SentenceItem(label, spoken, audio, symbol)` shared by grid and keyboard; remove-last, speak, stop, clear-with-undo; persists across pages; saved phrases (offline) | JUnit + Compose |
| 6 | Touch: cancelled press never fires; delay + release combos; semantics + click action on tiles/keys/hotspots; no double-fire; ≥48dp chrome; hold progress ring | JUnit + Compose |
| 7 | Speech: `UtteranceProgressListener`, request generation guard, public `stop()`, queued utterance while TTS initialises (bounded), safe MediaPlayer release, no phrase logging | JUnit (pure `SpeechSession` state machine) + Robolectric |
| 8 | Persistence: generation-ordered writes, unreadable files renamed `*.unreadable-<stamp>` and never overwritten, restore validation, pre-restore snapshot, backup includes saved buttons (optional field), CreateDocument export | JUnit |
| 9 | Settings/editor/sheet chrome restyled on the design system; high-contrast + reduced-motion prefs; Help/README rewritten to match | Compose |
| 10 | Version 2.0-preview / versionCode 4; signed APK + AAB; explicit failure if the key is missing | build log |

## 3. Acceptance checklist

Ticked = a test in `app/src/test` or a build log in `../evidence/` proves it. See STATUS.md for the file names.

### Navigation
- [x] Next/previous skip pages with `enabled = false`; editor still lists them. (BookNavigationTest, NavigationBarTest)
- [x] All pages disabled → player stays on the current page and shows it; nothing is deleted. (BookNavigationTest)
- [x] Page position "3 of 7" counts enabled pages only in player mode. (BookNavigationTest, NavigationBarTest)
- [x] Vocabulary search finds tiles, keyboard words and hotspots by label or spoken text and navigates to the page **without speaking**. (VocabularySearchTest, BoardInteractionTest.findOpens…)
- [x] Last page is remembered across restart; a book without that field opens on the first enabled page. (StoreTest)
- [x] Hotspot jump to a disabled page is refused (no navigation, no crash). (SceneSemanticsTest)

### Protect editing
- [x] With protection on, editor, library, settings (and so restore/reset inside it) require the PIN; speaking and Home do not. (ProtectEditingTest)
- [x] The switch that turns protection off is itself behind the PIN (it lives in Settings, which is gated). (ProtectEditingTest.settingsAndLibrary…)
- [x] Wrong PIN clears and reports; correct PIN unlocks until the person starts talking again. (ProtectEditingTest)

### Sentence
- [x] Items keep the tile's spoken phrase and recording, in order. (SentenceTest, BoardInteractionTest)
- [x] Remove-last removes only the last item; clear can be undone once; undo restores the exact list. (SentenceTest, SentenceBarTest)
- [x] Speak plays items in order; Stop halts mid-sentence; changing pages keeps the sentence. (SpeechManagerTest, BoardInteractionTest)
- [x] Keyboard and grid share the same bar and controls. (BoardInteractionTest.keyboardKeys…)
- [x] Saved phrases persist offline and speak the stored items. (BoardInteractionTest.aSavedPhrase…, BookStorageTest)

### Touch
- [x] Cancelled press (`tryAwaitRelease() == false`) never fires in any delay/release combination. (PressControllerTest, TileViewSemanticsTest)
- [x] Delay > 0: fires at dwell; lifting early cancels. Delay = 0 + release: fires on release only. Delay = 0: fires on press. (PressControllerTest)
- [x] Repeat lockout per tile is honoured (unchanged TouchAccess); semantic click and pointer press each fire exactly once. (TileViewSemanticsTest)
- [x] Tiles, keys, hotspots expose role Button, a content description, and a working `onClick` semantic. (TileViewSemanticsTest, BoardInteractionTest, SceneSemanticsTest)
- [x] Sentence-bar and navigation controls measure ≥ 48dp (SentenceBarTest, NavigationBarTest); form/sheet controls use the same 48dp components (not individually measured).

### Speech
- [x] `isSpeaking` follows utterance done/error callbacks. (SpeechManagerTest)
- [x] `stop()` cancels pending sound-it-out syllables and clip queues. (SpeechManagerTest)
- [x] A newer request supersedes an older one; stale callbacks are ignored. (SpeechManagerTest)
- [x] A phrase requested before TTS is ready is spoken once the engine is ready (or reported unavailable), not dropped. (SpeechManagerTest)

### Persistence
- [x] An older snapshot can never overwrite a newer one, regardless of thread order. (BookStorageTest)
- [x] Unreadable pages/settings/favourites/phrases files are moved aside with a timestamp and reported; defaults are used in memory; the original bytes remain on disk. (BookStorageTest, StoreTest, SettingsSheetTest)
- [x] Backup archive carries `savedTiles` and `phrases`; an archive without them (iPad / v1.2) restores with favourites untouched. (BookBackupTest, StoreTest)
- [x] Malformed restore (blank/duplicate ids, grid < 1, hotspot outside 0–100, future version) is refused before anything changes. (BookBackupTest)
- [x] Restore writes a `pre-restore` snapshot of the current book first. (StoreTest)
- [x] Old-format settings (no voiceId key / explicit null / extra keys) load. (ModelsSerializationTest)
- [x] Export via CreateDocument and the share export both write `BookBackup.encode(...)`. (BookBackupTest covers encode; the two Settings buttons call it — not separately tested)

### Build
- [x] `./gradlew testDebugUnitTest` green from a clean run: 111 tests, 0 failures (evidence/full-suite-final-2.log).
- [x] `./gradlew assembleRelease bundleRelease` signed with the Talk Tiles key (cert SHA-256 `f3db689c…0ab098`); fails loudly if the key is missing. (evidence/release-build.log; guard exercised by hand)
- [x] versionName `2.0-preview`, versionCode 4, applicationId `com.talktiles.app`. (aapt2 badging in STATUS.md)

### Layout and colour (added after device review)
- [x] Full-screen board: toolbar at the top and every button inside the viewport, portrait/landscape/editor/keyboard/48-grid. (BoardLayoutRegressionTest — written after the device showed the bar mid-screen)
- [x] Every ink on every surface is WCAG AA (≥4.5:1) in the normal palette; onPrimary on every filled colour too. (PaletteContrastTest)

### Touch (added after code review, 2026-09-19)
- [x] A tile whose press controller survives a page turn (same button data in the same slot on two pages) fires the action of the page now on screen, not the one it was created on. (TileViewSemanticsTest.aTouchCallsTheActionOfThePageNowOnScreen — RED on candidate 2: evidence/slice12-staletap-red.log)

### Verified on the tablet (candidate 2, 2026-09-18 23:44–00:00, Claude Code after Hermes' review session ended)
- [x] `adb install -r` over candidate 1: `aac_pages.json` and `aac_settings.json` byte-identical before and after (evidence/post-candidate2-data-preservation.json).
- [x] Home, board (toolbar at top, 3×3 grid full height, tile colours/positions identical to v1.2), Talking Keyboard, Feelings — evidence/device/c2-*.png.
- [x] Sentence bar: Eat + Drink → Speak; tablet audio captured and transcribed by Whisper as "Eat food, drink water." (evidence/device/c2-sentence-audio.wav) — v1.2 spoke labels only.
- [x] Remove last, Clear → "Sentence cleared" + Undo, Undo restores; sentence survives Next/Previous page; "n of m" updates (3 of 7 → 4 of 7 → 5 of 7).
- [x] Find: typing "bath" lists the Core Words button and two keyboard words with what they say; choosing one opens Core Words with no playback.
- [x] Saved phrases: save from the bar, tap to speak (audio captured, ~0.7 s "Eat food"). Deleting a phrase is an editor action — the test phrase "Eat" is still on the tablet.
- [x] Stop: five-item sentence, Stop tapped 2.7 s in, audio ended within ~0.1 s of the tap (evidence/device/c2-stop-audio2.json level strip).
- Not reached before the tablet was unplugged: landscape, large font, protect-editing PIN gate, editor sheets, camera/photo/recording, backup save-to-Files + restore, keep-on-screen. Candidate 3 (this source) is NOT yet on the tablet.

### Not claimed
- Hotspots use plain tap (as in v1.2): the hold-to-speak / speak-on-lift settings apply to grid buttons, not talking spots.
- MediaPlayer/TTS lifecycle in `AndroidSpeechBackend` is covered by the coordinator tests through the `SpeechBackend` interface, not by an on-host MediaPlayer test.
- In-app camera, background removal, eye-gaze, switch scanning: not implemented.
