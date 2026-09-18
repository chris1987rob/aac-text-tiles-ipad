import SwiftUI

/// Every screen this app can present on top of the board.
///
/// There used to be nine separate `.sheet` modifiers stacked on one view.
/// SwiftUI does not reliably honour more than one: presenting a second while
/// another is up, or opening one straight after another dismisses, leaves a
/// half-drawn sheet frozen on screen with the board rendering through it.
///
/// That is what a person hits the moment they tap through the app for real, and
/// it never showed up in testing here because launching straight into one
/// screen with a launch argument only ever presents a single sheet from a clean
/// start. This is the third time the trap has bitten this codebase; the rule is
/// already written into the other two fixes. One sheet, one enum.
private enum RootSheet: Identifiable {
    case tile(Int)
    case keyboardKey(String)
    case hotspot(HotspotModel)
    case pageOptions
    case pageWizard
    case gallery
    case pages
    case help
    case settings
    case pin

    var id: String {
        switch self {
        case .tile(let slot):    return "tile-\(slot)"
        case .keyboardKey(let w): return "key-\(w)"
        case .hotspot(let spot): return "hotspot-\(spot.id)"
        case .pageOptions:       return "pageOptions"
        case .pageWizard:        return "pageWizard"
        case .gallery:           return "gallery"
        case .pages:             return "pages"
        case .help:              return "help"
        case .settings:          return "settings"
        case .pin:               return "pin"
        }
    }
}

public struct RootView: View {
    @ObservedObject public var store: AACStore
    @State private var isShowingHome: Bool = false
    @State private var activeSheet: RootSheet? = nil

    public init(store: AACStore) { self.store = store }

    public var body: some View {
        ZStack {
            if isShowingHome {
                HomeView(
                    store: store,
                    onLaunchPlayer: {
                        store.isEditMode = false
                        withAnimation { isShowingHome = false }
                    },
                    onLaunchEditor: { requestEditor() },
                    onOpenHelp: { present(.help) },
                    onOpenSettings: { present(.settings) },
                    // The gallery screen existed but nothing could open it.
                    onOpenDownloads: { present(.gallery) }
                )
            } else {
                BoardView(
                    store: store,
                    onGoHome: { withAnimation { isShowingHome = true } },
                    onSelectTile: { slot in present(.tile(slot)) },
                    onSelectKey: { wordId in present(.keyboardKey(wordId)) },
                    onSelectHotspot: { spot in present(.hotspot(spot)) },
                    onAddHotspot: {
                        // Highest id + 1, not count + 1: deleting a hotspot and
                        // adding another used to hand out an id already in use,
                        // and two hotspots with one id confuses both the editor
                        // and the sheet that opens on it.
                        let newId = (store.currentPage.hotspots.map(\.id).max() ?? 0) + 1
                        let newSpot = HotspotModel(id: newId, label: "Hotspot \(newId)", tts: "Hotspot \(newId)")
                        store.currentPage.hotspots.append(newSpot)
                        present(.hotspot(newSpot))
                    },
                    onOpenPages: { present(.pages) },
                    onOpenOptions: { present(.pageOptions) },
                    onOpenNewPage: { present(.pageWizard) }
                )
            }
        }
        .sheet(item: $activeSheet) { sheet in
            switch sheet {
            case .tile(let slot):
                QuickEditModalView(store: store, slotId: IntItem(value: slot))
            case .keyboardKey(let wordId):
                KeyboardKeyEditorModalView(store: store, wordId: wordId)
            case .hotspot(let spot):
                HotspotEditorModalView(store: store, hotspot: spot)
            case .pageOptions:
                PageOptionsModalView(store: store)
            case .pageWizard:
                PageWizardModalView(store: store)
            case .gallery:
                OnlineGalleryModalView(store: store)
            case .pages:
                PagesNavigatorModalView(store: store)
            case .help:
                HelpGuideModalView()
            case .settings:
                SettingsModalView(store: store)
            case .pin:
                PinPromptView(expected: store.settings.lockPIN) {
                    // Deferred: changing screens while the sheet is still
                    // dismissing is the same race that made the picture buttons
                    // look dead.
                    DispatchQueue.main.async { enterEditor() }
                }
            }
        }
        .onAppear(perform: openScreenFromLaunchArgument)
    }

    /// Asking for a sheet while one is already up is what wedges the screen, so
    /// anything on screen is dismissed first and the new one follows a beat
    /// later. A stuck half-drawn sheet cannot be cleared without force-quitting
    /// the app, which for a child mid-sentence means losing their voice.
    private func present(_ sheet: RootSheet) {
        guard activeSheet != nil else {
            activeSheet = sheet
            return
        }
        activeSheet = nil
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
            activeSheet = sheet
        }
    }

    /// Every route into the editor comes through here. The lock used to be a
    /// switch that set a flag nobody read, so a parent could turn it on, hand
    /// the iPad over, and the editor was still one tap away on the Home screen.
    private func requestEditor() {
        if store.isLocked {
            present(.pin)
        } else {
            enterEditor()
        }
    }

    private func enterEditor() {
        store.isEditMode = true
        withAnimation { isShowingHome = false }
    }

    /// Opens a screen straight from a launch argument so each one can actually
    /// be looked at. There is no way to drive touches on the simulator, and a
    /// screen nobody has seen is a screen nobody has checked — which is how two
    /// dead buttons and an unreachable gallery survived this long.
    ///
    ///     xcrun simctl launch <device> <bundle-id> -openScreen settings
    ///
    /// Does nothing without the argument. Note what it CANNOT catch: it only
    /// ever opens one screen from a clean start, so it never exercises opening
    /// a sheet on top of another - which is all a person does. The nine stacked
    /// sheets above survived weeks of screenshots taken this way.
    private func openScreenFromLaunchArgument() {
        // Editor mode is its own switch so any screen can be opened in it,
        // e.g. -openScreen scene -editMode YES for the hotspot option bar.
        if UserDefaults.standard.bool(forKey: "editMode") {
            store.isEditMode = true
        }
        // `-pageType grid` only ever lands on the FIRST grid page, so a board
        // with seven grids has six that could never be looked at. Name the page
        // instead: -openScreen options -pageTitle "Help & Requests".
        if let wanted = UserDefaults.standard.string(forKey: "pageTitle"),
           let index = store.pages.firstIndex(where: { $0.title.caseInsensitiveCompare(wanted) == .orderedSame }) {
            store.currentPageIndex = index
        } else if UserDefaults.standard.object(forKey: "pageIndex") != nil {
            let index = UserDefaults.standard.integer(forKey: "pageIndex")
            if store.pages.indices.contains(index) { store.currentPageIndex = index }
        }

        guard let screen = UserDefaults.standard.string(forKey: "openScreen") else { return }
        switch screen {
        case "home":     isShowingHome = true
        case "settings": activeSheet = .settings
        case "gallery":  activeSheet = .gallery
        case "help":     activeSheet = .help
        case "options":
            // Page Options shows a different set of controls per page kind, so
            // which page it opens on has to be sayable:
            //   -openScreen options -pageType keyboard
            if UserDefaults.standard.string(forKey: "pageTitle") == nil,
               let wanted = pageType(named: UserDefaults.standard.string(forKey: "pageType")),
               let index = store.pages.firstIndex(where: { $0.type == wanted }) {
                store.currentPageIndex = index
            }
            activeSheet = .pageOptions
        case "pages":    activeSheet = .pages
        case "wizard":   activeSheet = .pageWizard
        case "tile":     activeSheet = .tile(1)
        case "hotspot":
            store.currentPageIndex = store.pages.firstIndex { !$0.hotspots.isEmpty } ?? 0
            if let first = store.currentPage.hotspots.first { activeSheet = .hotspot(first) }
        case "scene":
            store.currentPageIndex = store.pages.firstIndex { $0.type == .scene } ?? 0
        case "keyboard":
            store.currentPageIndex = store.pages.firstIndex { $0.type == .keyboard } ?? 0
        case "edit":
            store.isEditMode = true
        default: break
        }
    }

    private func pageType(named name: String?) -> PageType? {
        switch name {
        case "grid":     return .grid
        case "scene":    return .scene
        case "keyboard": return .keyboard
        default:         return nil
        }
    }
}

public struct IntItem: Identifiable {
    public var id: Int { value }
    public let value: Int
}
