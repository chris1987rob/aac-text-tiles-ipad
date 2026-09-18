import SwiftUI

public struct BoardView: View {
    @ObservedObject public var store: AACStore
    public let onGoHome: () -> Void
    public let onSelectTile: (Int) -> Void
    public let onSelectKey: (String) -> Void
    public let onSelectHotspot: (HotspotModel) -> Void
    public let onAddHotspot: () -> Void
    public let onOpenPages: () -> Void
    public let onOpenOptions: () -> Void
    public let onOpenNewPage: () -> Void

    public var body: some View {
        VStack(spacing: 0) {
            // Navigation bar, at the top of the board.
            NavigationBarView(
                store: store,
                onOpenPages: onOpenPages,
                onOpenOptions: onOpenOptions,
                onOpenNewPage: onOpenNewPage,
                onGoHome: onGoHome
            )

            // Express Speech Bar (if enabled)
            if store.currentPage.express && store.currentPage.type != .keyboard {
                ExpressBarView(store: store)
            }

            // Main Communication Canvas
            ZStack {
                pageGround
                    .edgesIgnoringSafeArea(.all)

                switch store.currentPage.type {
                case .grid:
                    TileGridView(store: store, onSelectTile: onSelectTile)
                case .scene:
                    VisualSceneView(store: store, onSelectHotspot: onSelectHotspot, onAddHotspot: onAddHotspot)
                case .keyboard:
                    KeyboardPageView(store: store, onSelectKey: onSelectKey)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            // Hard stop: whatever a page renders, it cannot grow past the space
            // it was given and push the toolbar off the bottom of the screen.
            .clipped()
        }
        // Was teal, to blend with the toolbar that used to sit at the bottom.
        // With the bar moved to the top that left a teal band stranded across
        // the bottom safe area, so the board's own colour fills it instead.
        .background(pageGround)
    }

    /// A page that has never had a colour chosen sits on the theme's pale
    /// ground rather than flat white, so the pastel tiles have something to
    /// sit against. Any colour a person picked is honoured as-is.
    private var pageGround: Color {
        store.currentPage.bgHex.uppercased() == "#FFFFFF" ? BoardTheme.background : Color(hex: store.currentPage.bgHex)
    }
}
