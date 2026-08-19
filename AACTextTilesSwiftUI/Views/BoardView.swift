import SwiftUI

public struct BoardView: View {
    @ObservedObject public var store: AACStore
    public let onGoHome: () -> Void
    public let onSelectTile: (Int) -> Void
    public let onSelectHotspot: (HotspotModel) -> Void
    public let onAddHotspot: () -> Void
    public let onOpenPages: () -> Void
    public let onOpenOptions: () -> Void
    public let onOpenNewPage: () -> Void

    public var body: some View {
        VStack(spacing: 0) {
            // Express Speech Bar (if enabled)
            if store.currentPage.express && store.currentPage.type != .keyboard {
                ExpressBarView(store: store)
            }

            // Main Communication Canvas
            ZStack {
                Color(hex: store.currentPage.bgHex)
                    .edgesIgnoringSafeArea(.all)

                switch store.currentPage.type {
                case .grid:
                    TileGridView(store: store, onSelectTile: onSelectTile)
                case .scene:
                    VisualSceneView(store: store, onSelectHotspot: onSelectHotspot, onAddHotspot: onAddHotspot)
                case .keyboard:
                    KeyboardPageView(store: store)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            // Signature GoTalk Teal Bottom Toolbar
            BottomToolbarView(
                store: store,
                onOpenPages: onOpenPages,
                onOpenOptions: onOpenOptions,
                onOpenNewPage: onOpenNewPage,
                onGoHome: onGoHome
            )
        }
        .background(Color(hex: "#008369"))
    }
}
