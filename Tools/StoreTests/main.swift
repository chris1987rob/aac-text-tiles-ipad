import Foundation

var fails: [String] = []
func check(_ name: String, _ ok: Bool, _ detail: String = "") {
    if !ok { fails.append(detail.isEmpty ? name : "\(name) — \(detail)") }
}
func eq<T: Equatable>(_ name: String, _ got: T, _ want: T) {
    check(name, got == want, "got \(got), want \(want)")
}

// Never touch the real ~/Documents/aac_pages.json.
let realStore = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
    .appendingPathComponent("aac_pages.json")
let backup = realStore.appendingPathExtension("testbackup")
let hadExisting = FileManager.default.fileExists(atPath: realStore.path)
if hadExisting { try? FileManager.default.moveItem(at: realStore, to: backup) }
func restore() {
    try? FileManager.default.removeItem(at: realStore)
    if hadExisting { try? FileManager.default.moveItem(at: backup, to: realStore) }
}

// ---------------------------------------------------------------- starter book
let store = AACStore()
eq("starter book has 7 pages", store.pages.count, 7)
eq("first page is Colors", store.pages[0].title, "Colors")
check("every page has a non-empty title", store.pages.allSatisfy { !$0.title.isEmpty })
check("every page grid size is a supported layout",
      store.pages.allSatisfy { [1,2,4,6,8,9,12,16,20,25,30,36,48].contains($0.gridSize) },
      "sizes: \(store.pages.map(\.gridSize))")
check("no page declares more tiles than its grid holds",
      store.pages.allSatisfy { p in p.tiles.keys.allSatisfy { $0 >= 1 && $0 <= p.gridSize } },
      "offending: \(store.pages.filter { p in p.tiles.keys.contains { $0 < 1 || $0 > p.gridSize } }.map(\.title))")

// ------------------------------------------------------- toolbar: prev / next
store.currentPageIndex = 0
store.nextPage(); eq("next advances", store.currentPageIndex, 1)
store.prevPage(); eq("prev goes back", store.currentPageIndex, 0)
store.prevPage(); eq("prev wraps to last", store.currentPageIndex, store.pages.count - 1)
store.nextPage(); eq("next wraps to first", store.currentPageIndex, 0)

// ------------------------------------------------------ express bar behaviour
SpeechManager.shared.reset()
store.clearExpressChips()
store.playExpressSentence()
eq("empty sentence stays silent", SpeechManager.shared.spoken.count, 0)
store.addExpressChip("I")
store.addExpressChip("want")
store.addExpressChip("more")
eq("chips accumulate", store.expressChips, ["I", "want", "more"])
store.playExpressSentence()
eq("sentence joins with spaces", SpeechManager.shared.spoken.last ?? "", "I want more")
store.clearExpressChips()
eq("clear empties the strip", store.expressChips.count, 0)

// ------------------------------------------ currentPage setter must persist
store.currentPageIndex = 0
var edited = store.currentPage
edited.title = "Edited Title"
store.currentPage = edited
eq("setter writes through to pages", store.pages[0].title, "Edited Title")

// ------------------------------------------------- persistence round-trip
store.saveNow()
check("a store file was written", FileManager.default.fileExists(atPath: realStore.path))
let reloaded = AACStore()
eq("edit survives a relaunch", reloaded.pages[0].title, "Edited Title")
eq("page count survives", reloaded.pages.count, 7)

// tiles and hotspots must survive too, not just the title
var p0 = reloaded.currentPage
p0.tiles[1] = TileModel(id: 1, label: "TESTTILE", tts: "test tile")
reloaded.currentPage = p0
reloaded.saveNow()
let reloaded2 = AACStore()
eq("tile edit survives", reloaded2.pages[0].tiles[1]?.label ?? "", "TESTTILE")

// Scene hotspots. The starter book no longer ships a demo scene (the Living
// Room page was dropped when the template boards landed), so the test makes
// its own rather than depending on starter content that can be removed again.
if reloaded2.pages.allSatisfy({ $0.hotspots.isEmpty }) {
    var made = PageModel(title: "Test Scene", type: .scene)
    made.hotspots = [HotspotModel(id: 1, x: 10, y: 10, w: 20, h: 20, label: "Spot")]
    reloaded2.pages.append(made)
    reloaded2.saveNow()
}
if let sceneIdx = reloaded2.pages.firstIndex(where: { !$0.hotspots.isEmpty }) {
    reloaded2.currentPageIndex = sceneIdx
    var sp = reloaded2.currentPage
    let beforeId = sp.hotspots[0].id
    sp.hotspots[0].x = 77.7      // percentages, 0..100
    sp.hotspots[0].y = 22.2
    reloaded2.currentPage = sp
    reloaded2.saveNow()
    let reloaded3 = AACStore()
    let moved = reloaded3.pages[sceneIdx].hotspots[0]
    eq("dragged hotspot position survives a relaunch", moved.x, 77.7)
    eq("dragged hotspot y survives", moved.y, 22.2)
    eq("hotspot identity preserved", moved.id, beforeId)
    check("hotspot stays inside the picture",
          reloaded3.pages.allSatisfy { p in p.hotspots.allSatisfy {
              $0.x >= 0 && $0.y >= 0 && $0.x <= 100 && $0.y <= 100 &&
              $0.w > 0 && $0.h > 0 } },
          "a hotspot is off-picture or zero-sized")
} else {
    fails.append("no page with hotspots in the starter book")
}

// ------------------------- a scene page's picture must survive being created
// The New Page Wizard now attaches the picture at creation time, so the bytes
// go through the same JSON round-trip as everything else (Data becomes base64).
do {
    let s3 = AACStore()
    let jpegish = Data((0..<2048).map { UInt8($0 % 251) })
    var made = PageModel(title: "Beach", type: .scene)
    made.sceneImageData = jpegish
    s3.pages.append(made)
    s3.saveNow()

    let back = AACStore()
    let scene = back.pages.first { $0.title == "Beach" }
    check("wizard scene page survives", scene != nil)
    eq("scene picture bytes survive a relaunch", scene?.sceneImageData, jpegish)
    eq("scene page keeps its type", scene?.type, PageType.scene)
}

// ---------------------------------------- page navigation visits every page
// The "Include in page navigation" switch was removed 2026-09-13. A page saved
// with enabled=false by an older build must still be reachable, because
// nothing in the app can switch it back on.
do {
    let s4 = AACStore()
    s4.isEditMode = false
    s4.pages = [
        PageModel(title: "A", enabled: true),
        PageModel(title: "B", enabled: false),
        PageModel(title: "C", enabled: true)
    ]
    s4.currentPageIndex = 0
    s4.nextPage()
    eq("next page visits a page saved as disabled", s4.pages[s4.currentPageIndex].title, "B")
    s4.currentPageIndex = 0
    s4.prevPage()
    eq("previous page wraps to the end", s4.pages[s4.currentPageIndex].title, "C")
    s4.nextPage()
    eq("next page wraps to the start", s4.pages[s4.currentPageIndex].title, "A")
}

// ------------------------------------------------- repeat lockout for tremor
do {
    let access = TouchAccess.shared
    access.reset()
    let t0 = Date()
    check("first press always fires", access.shouldFire(key: "tile-1", lockout: 1.0, now: t0))
    check("a second press inside the lockout is swallowed",
          !access.shouldFire(key: "tile-1", lockout: 1.0, now: t0.addingTimeInterval(0.3)))
    check("a different button is not blocked",
          access.shouldFire(key: "tile-2", lockout: 1.0, now: t0.addingTimeInterval(0.3)))
    check("the same button fires again once the lockout passes",
          access.shouldFire(key: "tile-1", lockout: 1.0, now: t0.addingTimeInterval(1.2)))
    check("lockout of zero never blocks",
          access.shouldFire(key: "tile-3", lockout: 0, now: t0)
          && access.shouldFire(key: "tile-3", lockout: 0, now: t0))
}

// ------------------------------------------------------ whole-book backup
// The highest-consequence gap in the app: everything a parent builds lived in
// one file with no way to get it out.
do {
    let s5 = AACStore()
    var settings = AppSettings()
    settings.voiceId = "com.example.voice"
    settings.childLock = true
    settings.repeatLockout = 0.8

    var page = PageModel(title: "Backup Me", type: .scene)
    page.hotspots = [HotspotModel(id: 7, label: "Spot")]
    page.sceneImageData = Data([3, 1, 4, 1, 5, 9])
    page.tiles[1] = TileModel(id: 1, label: "Hi", tts: "hello",
                              photoData: Data([1, 2]), audioData: Data([7, 7]))

    let url = try! BookBackup.write(pages: [page], settings: settings)
    check("a backup file is written", FileManager.default.fileExists(atPath: url.path))

    let (archive, summary) = try! BookBackup.read(from: url)
    eq("backup keeps every page", archive.pages.count, 1)
    eq("backup counts buttons", summary.buttons, 1)
    eq("backup counts hotspots", summary.hotspots, 1)
    eq("a button's photo survives the round trip", archive.pages[0].tiles[1]?.photoData, Data([1, 2]))
    eq("a button's recording survives", archive.pages[0].tiles[1]?.audioData, Data([7, 7]))
    eq("the scene picture survives", archive.pages[0].sceneImageData, Data([3, 1, 4, 1, 5, 9]))
    eq("settings travel with the book", archive.settings?.voiceId, "com.example.voice")
    eq("the lock travels with the book", archive.settings?.childLock, true)

    // A plain array of pages - what the single-page share writes - must still
    // restore rather than telling a parent their file is broken.
    let bare = FileManager.default.temporaryDirectory.appendingPathComponent("bare.json")
    try! JSONEncoder().encode([page]).write(to: bare)
    let (bareArchive, _) = try! BookBackup.read(from: bare)
    eq("a bare page list is accepted too", bareArchive.pages.count, 1)

    try? FileManager.default.removeItem(at: url)
    try? FileManager.default.removeItem(at: bare)
    _ = s5
}

// --------------------------- editing a tile must not destroy its photo/audio
// Regression: QuickEditModalView built a fresh TileModel and dropped
// photoData, audioData, labelHex and labelPositionTop, so changing a label
// silently destroyed a tile's photo and its recorded voice.
do {
    let s2 = AACStore()
    s2.currentPageIndex = 0
    var page = s2.currentPage
    page.tiles[1] = TileModel(id: 1, label: "Before", tts: "before",
                              photoData: Data([1,2,3,4]),
                              labelHex: "#FF0000",
                              audioData: Data([9,9,9]),
                              labelPositionTop: true)
    s2.currentPage = page
    s2.saveNow()

    // Simulate exactly what the edit screen now does on Save.
    let reopened = AACStore()
    reopened.currentPageIndex = 0
    var pg = reopened.currentPage
    let existing = pg.tiles[1]
    pg.tiles[1] = TileModel(
        id: 1,
        label: "After",                      // the only thing the user changed
        tts: "after",
        symbolName: nil,
        photoData: existing?.photoData,
        bgHex: "#FFFFFF",
        borderHex: "#CBD5E1",
        labelHex: existing?.labelHex ?? "#1E293B",
        labelSize: 1.0,
        audioData: existing?.audioData,
        isSoundItOut: false,
        labelPositionTop: existing?.labelPositionTop ?? false
    )
    reopened.currentPage = pg
    reopened.saveNow()

    let after = AACStore()
    let t = after.pages[0].tiles[1]
    eq("label change applied", t?.label ?? "", "After")
    eq("photo survives a label edit", t?.photoData, Data([1,2,3,4]))
    eq("recorded voice survives a label edit", t?.audioData, Data([9,9,9]))
    eq("label colour survives", t?.labelHex ?? "", "#FF0000")
    eq("label position survives", t?.labelPositionTop ?? false, true)
}

// ------------------------------------------------------------- reset restores
let resetStore = AACStore()
resetStore.resetToDefaults()
eq("reset restores the starter title", resetStore.pages[0].title, "Colors")
eq("reset restores page count", resetStore.pages.count, 7)
let afterReset = AACStore()
eq("reset persisted, not just in memory", afterReset.pages[0].title, "Colors")

// -------------------------------------------------------- index safety
let empty = AACStore()
empty.pages = []
eq("empty book yields a safe placeholder", empty.currentPage.title, "Default")
empty.currentPageIndex = 999
eq("out-of-range index is safe", empty.currentPage.title, "Default")
empty.nextPage()   // must not crash


// ---------------------------------------------- every size the picker offers
//
// A segmented picker whose selection is not one of its tags renders BLANK, and
// a blank control reads as a missing feature - which is exactly how it was
// reported: "on Help & Requests the thing that lets you pick 1 or 48 isn't
// there". Page Options, the New Page Wizard and the symbol keyboard now share
// one list, so the thing to guard is that no page can be built carrying a size
// that list does not offer.
let offered = Set(SymbolWordBank.keyCountOptions)
check("the starter book only uses sizes the picker offers",
      store.pages.allSatisfy { $0.type != .grid || offered.contains($0.gridSize) },
      "offending: \(store.pages.filter { $0.type == .grid && !offered.contains($0.gridSize) }.map { "\($0.title)=\($0.gridSize)" })")

check("every starter template only uses sizes the picker offers",
      PageTemplateCatalog.all.allSatisfy { offered.contains(max($0.gridSize, $0.buttonCount)) },
      "offending: \(PageTemplateCatalog.all.filter { !offered.contains(max($0.gridSize, $0.buttonCount)) }.map { "\($0.id)=\(max($0.gridSize, $0.buttonCount))" })")

check("no template hides its own buttons",
      PageTemplateCatalog.all.allSatisfy { $0.gridSize >= $0.buttonCount },
      "offending: \(PageTemplateCatalog.all.filter { $0.gridSize < $0.buttonCount }.map(\.id))")

for legacy in [3, 6, 20, 30, 42, 100] {
    check("a page saved at \(legacy) snaps to something the picker offers",
          offered.contains(SymbolWordBank.nearestKeyCount(legacy)))
}

// ------------------------------------------------------------ saved buttons
let favURL = TileFavorites.storeURL
let favBackup = favURL.appendingPathExtension("testbackup")
let hadFavs = FileManager.default.fileExists(atPath: favURL.path)
if hadFavs { try? FileManager.default.moveItem(at: favURL, to: favBackup) }
func restoreFavorites() {
    try? FileManager.default.removeItem(at: favURL)
    if hadFavs { try? FileManager.default.moveItem(at: favBackup, to: favURL) }
}

do {
    let favs = TileFavorites()
    let mom = TileModel(id: 4, label: "Mom", tts: "Mom", photoData: Data([1, 2, 3]),
                        bgHex: "#FFF9C4", borderHex: "#E5C100", labelHex: "#111111",
                        labelSize: 1.4, audioData: Data([7, 7]), labelPositionTop: true)
    _ = favs.add(from: mom)
    eq("saving a button puts it in the library", favs.items.count, 1)

    // The whole point: the photograph and the recording travel with it.
    let back = favs.items[0].tile(inSlot: 31)
    eq("the saved button lands in the slot it is dropped in", back.id, 31)
    eq("its photo comes with it", back.photoData, Data([1, 2, 3]))
    eq("its recording comes with it", back.audioData, Data([7, 7]))
    eq("its colour comes with it", back.bgHex, "#FFF9C4")
    eq("its border comes with it", back.borderHex, "#E5C100")
    eq("its text colour comes with it", back.labelHex, "#111111")
    eq("its word size comes with it", back.labelSize, 1.4)
    eq("its word position comes with it", back.labelPositionTop, true)

    // Saving the same name again replaces rather than duplicating.
    var edited = mom
    edited.tts = "Mommy"
    _ = favs.add(from: edited)
    eq("saving the same name again replaces it", favs.items.count, 1)
    eq("the replacement is the newer one", favs.items[0].tts, "Mommy")

    _ = favs.add(from: TileModel(id: 2, label: "Dog", tts: "Dog", photoData: Data([4])))
    eq("a different name is a second saved button", favs.items.count, 2)
    eq("search finds by name", favs.search("do").count, 1)
    eq("search is case-insensitive", favs.search("MOM").count, 1)
    eq("an empty search returns everything", favs.search("  ").count, 2)

    // Deleting from a filtered list must delete the row that was shown, not
    // whatever sits at that position in the full library.
    let shown = favs.search("do")
    favs.remove(id: shown[0].id)
    eq("deleting the searched row deletes that one", favs.items.count, 1)
    eq("and leaves the other alone", favs.items[0].name, "Mom")

    let reloaded = TileFavorites()
    eq("the library survives a restart", reloaded.items.count, 1)
    eq("and so does its photo", reloaded.items[0].photoData, Data([1, 2, 3]))
    eq("and so does its recording", reloaded.items[0].audioData, Data([7, 7]))

    // A button saved with no name at all falls back to its label rather than
    // going into the library as an untitled blank.
    let unnamed = TileFavorites()
    _ = unnamed.add(SavedTile(name: "   ", label: "Water", tts: "Water"))
    eq("an unnamed save takes the button's own word", unnamed.items.first?.name ?? "", "Water")
}
restoreFavorites()



// -------------------------------------------------- a lone capital letter
//
// Handed "I" on its own the synthesiser reads the letter's NAME, so the "I"
// key on the symbol keyboard said "capital I". Only single characters are
// touched - anything longer must reach the voice exactly as it was typed.
eq("a lone capital I speaks as the word", SpokenText.forSpeech("I"), "i")
eq("a lone capital A too", SpokenText.forSpeech("A"), "a")
eq("surrounding spaces do not defeat it", SpokenText.forSpeech("  I "), "i")
eq("a two-letter word is untouched", SpokenText.forSpeech("It"), "It")
eq("a sentence is untouched", SpokenText.forSpeech("I want more"), "I want more")
eq("an all-caps word is untouched", SpokenText.forSpeech("STOP"), "STOP")
eq("an already-lowercase letter is untouched", SpokenText.forSpeech("a"), "a")
eq("an emoji is untouched", SpokenText.forSpeech("\u{1F34E}"), "\u{1F34E}")
eq("empty text is untouched", SpokenText.forSpeech(""), "")

// ------------------------------------------------ keyboard keys are editable
//
// Keyboard keys come from the word bank, not from page.tiles, so until now
// there was nowhere to put a change - tapping a key in the page editor did
// nothing at all. Edits live on the page, keyed by word id.
let theI = SymbolWordBank.groups.lazy.flatMap({ $0.words }).first(where: { $0.id == "i" })
check("the keyboard has an I key", theI != nil)

if let word = theI {
    eq("the I key is written with a capital", word.label, "I")
    eq("and it is in the People group",
       SymbolWordBank.groups.first(where: { $0.words.contains(where: { $0.id == "i" }) })?.title ?? "", "People")

    var page = PageModel(title: "Keys", type: .keyboard)
    check("a fresh keyboard page stores no edits", page.keyboardEdits == nil)
    eq("an unedited word is itself", word.applying(nil), word)

    page.keyboardEdits = ["i": KeyboardKeyEdit(label: "me", tts: "me", colorHex: "#C8E6C9")]
    let edited = word.applying(page.keyboardEdits?["i"])
    eq("an edited label applies", edited.label, "me")
    eq("an edited spoken text applies", edited.tts, "me")
    eq("an edited colour applies", edited.color, "#C8E6C9")
    eq("what the edit left alone is untouched", edited.icon, word.icon)
    eq("the word bank itself is not changed", word.label, "I")

    // An edit reset back to the bank's own values must not be stored, or a
    // page grows by ninety-five identical entries the first time someone
    // opens a key and closes it again.
    check("an empty edit knows it is empty", KeyboardKeyEdit().isEmpty)
    check("hidden:false is still empty", KeyboardKeyEdit(hidden: false).isEmpty)
    check("hidden:true is not empty", !KeyboardKeyEdit(hidden: true).isEmpty)
    check("a photo alone is not empty", !KeyboardKeyEdit(photoData: Data([1])).isEmpty)

    // A photo and a recording put on a key travel with it.
    let rich = word.applying(KeyboardKeyEdit(photoData: Data([1, 2]), audioData: Data([3])))
    eq("a photo on a key applies", rich.photoData, Data([1, 2]))
    eq("a recording on a key applies", rich.audioData, Data([3]))

    // Round-trips through the real encoder, and older boards still decode.
    let encoded = try! JSONEncoder().encode(page)
    let back = try! JSONDecoder().decode(PageModel.self, from: encoded)
    eq("key edits survive a save and reload", back.keyboardEdits?["i"]?.label ?? "", "me")
    eq("the page kind survives with them", back.type, PageType.keyboard)

    let legacy = "{\"id\":\"" + UUID().uuidString + "\",\"title\":\"Old\",\"type\":\"Talking Keyboard\",\"gridSize\":16,\"bgHex\":\"#FFFFFF\",\"enabled\":true,\"express\":false,\"tiles\":{},\"hotspots\":[]}"
    let older = try? JSONDecoder().decode(PageModel.self, from: Data(legacy.utf8))
    check("a board saved before key editing still decodes", older != nil)
    check("and comes back with no edits", older?.keyboardEdits == nil)
}

// ------------------------------------------- pictures, Bella, and old settings
// Two picture sets and a recorded voice arrived together. What matters most
// is that a settings file or backup from before them still loads whole.
do {
    // The clip index is keyed by this; Tools/sync-native-assets.py mirrors it.
    eq("phrase key: case and punctuation", SpokenText.normalisedPhrase("I Want!"), "i want")
    eq("phrase key: spacing", SpokenText.normalisedPhrase("  my   Schedule "), "my schedule")
    eq("phrase key: apostrophes stay", SpokenText.normalisedPhrase("don\u{2019}t"), "don't")
    eq("phrase key: underscore breaks", SpokenText.normalisedPhrase("thank_you"), "thank you")
    eq("phrase key: nothing left", SpokenText.normalisedPhrase("...!"), "")

    eq("bella id names her folder", SpokenText.recordedVoiceFolder(SpokenText.bellaVoiceId), "bella")
    eq("a system voice is not a folder", SpokenText.recordedVoiceFolder("com.apple.voice.compact.en-US.Samantha"), nil)
    check("bella is a recorded voice", VoiceClips.isRecordedVoice(SpokenText.bellaVoiceId))
    check("the system default is not", !VoiceClips.isRecordedVoice(nil))
    eq("no clip for a system voice", VoiceClips.clip(for: "hello", voiceId: nil), nil)

    eq("new settings speak with Bella", AppSettings().voiceId, SpokenText.bellaVoiceId)
    eq("new settings open our pictures", AppSettings().symbolSet, SymbolSet.talkTiles)

    let dec = JSONDecoder()
    let old = "{\"speechRate\":0.6,\"childLock\":true,\"lockPIN\":\"4321\",\"activationDelay\":0,\"repeatLockout\":0,\"activateOnRelease\":false}"
    let a = try? dec.decode(AppSettings.self, from: Data(old.utf8))
    check("a settings file from before the sets still loads", a != nil)
    eq("and keeps its PIN", a?.lockPIN, "4321")
    eq("and keeps its lock", a?.childLock, true)
    eq("and gets Bella", a?.voiceId, SpokenText.bellaVoiceId)
    eq("and gets our pictures", a?.symbolSet, SymbolSet.talkTiles)

    let sys = try? dec.decode(AppSettings.self, from: Data("{\"voiceId\":null,\"symbolSet\":\"mulberry\"}".utf8))
    eq("an explicit iPad-default voice stays that way", sys?.voiceId, String?.none)
    eq("mulberry stays chosen", sys?.symbolSet, SymbolSet.mulberry)

    var chosen = AppSettings()
    chosen.voiceId = nil
    chosen.symbolSet = .mulberry
    let data = try! JSONEncoder().encode(chosen)
    eq("iPad default survives a save", (try? dec.decode(AppSettings.self, from: data))?.voiceId, String?.none)
    eq("the set survives a save", (try? dec.decode(AppSettings.self, from: data))?.symbolSet, SymbolSet.mulberry)

    let future = try? dec.decode(AppSettings.self, from: Data("{\"symbolSet\":\"picto\"}".utf8))
    eq("a set this build does not know falls back", future?.symbolSet, SymbolSet.talkTiles)

    // The catalogue is a bundle resource, so a command-line test sees none of
    // it; what it can prove is that the empty case is quiet, not a crash.
    check("no catalogue here, no crash", TalkTilesCatalog.search("cat").isEmpty)
    check("no voices here, no crash", VoiceClips.available.isEmpty)
}

restore()
if fails.isEmpty {
    print("STORE / BUTTON LOGIC OK — all checks passed")
} else {
    fails.forEach { print("FAIL  \($0)") }
    exit(1)
}
