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
eq("starter book has 5 pages", store.pages.count, 5)
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
eq("page count survives", reloaded.pages.count, 5)

// tiles and hotspots must survive too, not just the title
var p0 = reloaded.currentPage
p0.tiles[1] = TileModel(id: 1, label: "TESTTILE", tts: "test tile")
reloaded.currentPage = p0
reloaded.saveNow()
let reloaded2 = AACStore()
eq("tile edit survives", reloaded2.pages[0].tiles[1]?.label ?? "", "TESTTILE")

// scene hotspots
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

// ------------------------------------------------------------- reset restores
let resetStore = AACStore()
resetStore.resetToDefaults()
eq("reset restores the starter title", resetStore.pages[0].title, "Colors")
eq("reset restores page count", resetStore.pages.count, 5)
let afterReset = AACStore()
eq("reset persisted, not just in memory", afterReset.pages[0].title, "Colors")

// -------------------------------------------------------- index safety
let empty = AACStore()
empty.pages = []
eq("empty book yields a safe placeholder", empty.currentPage.title, "Default")
empty.currentPageIndex = 999
eq("out-of-range index is safe", empty.currentPage.title, "Default")
empty.nextPage()   // must not crash

restore()
if fails.isEmpty {
    print("STORE / BUTTON LOGIC OK — all checks passed")
} else {
    fails.forEach { print("FAIL  \($0)") }
    exit(1)
}
