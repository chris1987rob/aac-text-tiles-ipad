import SwiftUI

public struct RootView: View {
    @StateObject private var store = AACStore()
    @State private var isShowingHome: Bool = false

    // Modals state
    @State private var selectedSlotForEdit: Int? = nil
    @State private var selectedHotspotForEdit: HotspotModel? = nil
    @State private var isShowingPageWizard: Bool = false
    @State private var isShowingOnlineGallery: Bool = false
    @State private var isShowingPagesNavigator: Bool = false
    @State private var isShowingHelpGuide: Bool = false

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
                    onOpenHelp: { isShowingHelpGuide = true }
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
                    onOpenOptions: { isShowingPagesNavigator = true },
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
