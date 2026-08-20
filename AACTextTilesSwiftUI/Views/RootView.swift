import SwiftUI

public struct RootView: View {
    @StateObject private var store = AACStore()
    @State private var isShowingHome: Bool = false

    // Modals state
    @State private var selectedSlotForEdit: Int? = nil
    @State private var selectedHotspotForEdit: HotspotModel? = nil
    @State private var isShowingPageOptions: Bool = false
    @State private var isShowingPageWizard: Bool = false
    @State private var isShowingOnlineGallery: Bool = false
    @State private var isShowingPagesNavigator: Bool = false
    @State private var isShowingHelpGuide: Bool = false
    @State private var isShowingSettings: Bool = false

    public init() {}

    public var body: some View {
        ZStack {
            if isShowingHome {
                HomeView(
                    store: store,
                    onLaunchPlayer: {
                        store.isEditMode = false
                        withAnimation { isShowingHome = false }
                    },
                    onLaunchEditor: {
                        store.isEditMode = true
                        withAnimation { isShowingHome = false }
                    },
                    onOpenHelp: { isShowingHelpGuide = true },
                    onOpenSettings: { isShowingSettings = true },
                    // The gallery screen existed but nothing could open it.
                    onOpenDownloads: { isShowingOnlineGallery = true }
                )
            } else {
                BoardView(
                    store: store,
                    onGoHome: { withAnimation { isShowingHome = true } },
                    onSelectTile: { slot in selectedSlotForEdit = slot },
                    onSelectHotspot: { spot in selectedHotspotForEdit = spot },
                    onAddHotspot: {
                        let newId = store.currentPage.hotspots.count + 1
                        let newSpot = HotspotModel(id: newId, label: "Hotspot \(newId)", tts: "Hotspot \(newId)")
                        store.currentPage.hotspots.append(newSpot)
                        selectedHotspotForEdit = newSpot
                    },
                    onOpenPages: { isShowingPagesNavigator = true },
                    onOpenOptions: { isShowingPageOptions = true },
                    onOpenNewPage: { isShowingPageWizard = true }
                )
            }
        }
        .sheet(item: selectedSlotForEditBinding) { item in
            QuickEditModalView(store: store, slotId: item)
        }
        .sheet(item: $selectedHotspotForEdit) { spot in
            HotspotEditorModalView(store: store, hotspot: spot)
        }
        .sheet(isPresented: $isShowingPageOptions) {
            PageOptionsModalView(store: store)
        }
        .sheet(isPresented: $isShowingPageWizard) {
            PageWizardModalView(store: store)
        }
        .sheet(isPresented: $isShowingOnlineGallery) {
            OnlineGalleryModalView(store: store)
        }
        .sheet(isPresented: $isShowingPagesNavigator) {
            PagesNavigatorModalView(store: store)
        }
        .sheet(isPresented: $isShowingHelpGuide) {
            HelpGuideModalView()
        }
        .sheet(isPresented: $isShowingSettings) {
            SettingsModalView(store: store)
        }
        .onAppear(perform: openScreenFromLaunchArgument)
    }

    /// Opens a screen straight from a launch argument so each one can actually
    /// be looked at. There is no way to drive touches on the simulator, and a
    /// screen nobody has seen is a screen nobody has checked — which is how two
    /// dead buttons and an unreachable gallery survived this long.
    ///
    ///     xcrun simctl launch <device> <bundle-id> -openScreen settings
    ///
    /// Does nothing without the argument.
    private func openScreenFromLaunchArgument() {
        guard let screen = UserDefaults.standard.string(forKey: "openScreen") else { return }
        switch screen {
        case "home":     isShowingHome = true
        case "settings": isShowingSettings = true
        case "gallery":  isShowingOnlineGallery = true
        case "help":     isShowingHelpGuide = true
        case "options":  isShowingPageOptions = true
        case "pages":    isShowingPagesNavigator = true
        case "wizard":   isShowingPageWizard = true
        case "tile":     selectedSlotForEdit = 1
        case "hotspot":
            store.currentPageIndex = store.pages.firstIndex { !$0.hotspots.isEmpty } ?? 0
            selectedHotspotForEdit = store.currentPage.hotspots.first
        case "scene":
            store.currentPageIndex = store.pages.firstIndex { $0.type == .scene } ?? 0
        case "keyboard":
            store.currentPageIndex = store.pages.firstIndex { $0.type == .keyboard } ?? 0
        case "edit":
            store.isEditMode = true
        default: break
        }
    }

    private var selectedSlotForEditBinding: Binding<IntItem?> {
        Binding(
            get: { selectedSlotForEdit.map { IntItem(value: $0) } },
            set: { selectedSlotForEdit = $0?.value }
        )
    }
}

public struct IntItem: Identifiable {
    public var id: Int { value }
    public let value: Int
}
