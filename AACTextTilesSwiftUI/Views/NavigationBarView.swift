import SwiftUI

/// The app's navigation bar. It sat at the bottom of the board until 2026-08-26;
/// the original web version always had it at the top, which is also where a
/// child's hand is least likely to be resting when they reach for a tile.
///
/// Two very different bars share this view (2026-09-13):
///
/// - **Player.** Back and Home on the left, the page name large in the
///   middle, nothing on the right. The name opens the page list - with the
///   forward arrow and the "layers" button gone it is the other way a child
///   changes page, so it must stay tappable here even though it cannot be
///   *edited* here.
/// - **Editor.** Back, Home and Options on the left, New Page on the right, and
///   tapping the page name turns it into a text field so a page can be named
///   without opening Page Options.
public struct NavigationBarView: View {
    @ObservedObject public var store: AACStore
    public let onOpenPages: () -> Void
    public let onOpenOptions: () -> Void
    public let onOpenNewPage: () -> Void
    public let onGoHome: () -> Void

    @State private var isRenaming = false
    @State private var draftTitle = ""
    @FocusState private var titleFieldFocused: Bool

    public var body: some View {
        // The name is laid over the bar and centred on the SCREEN, not on the
        // gap between the button groups - two buttons on the left and none on
        // the right used to push it visibly off-centre.
        ZStack {
            Group {
                if store.isEditMode {
                    editorTitle
                } else {
                    playerTitle
                }
            }
            .padding(.horizontal, 170)

            HStack(spacing: 12) {
            // Left Action Group
            HStack(spacing: 8) {
                // Back one page, in both the player and the editor.
                RoundBarButton(icon: "arrow.left", label: "Back") {
                    commitRename(); store.prevPage()
                }

                // Orange Home circle
                RoundBarButton(icon: "house.fill", fill: BoardTheme.accent, tint: .white, label: "Home") {
                    commitRename(); onGoHome()
                }

                // Editor Mode: Sliders / Options Button
                if store.isEditMode {
                    RoundBarButton(icon: "slider.horizontal.3", label: "Page options") {
                        commitRename(); onOpenOptions()
                    }
                }
            }

            Spacer()

            // Right Action Group. Empty in the player: the green "read the
            // whole page" button that used to live here is gone - play belongs
            // with the sentence bar, nowhere else.
            if store.isEditMode {
                RoundBarButton(icon: "plus", fill: BoardTheme.accent, tint: .white, label: "New page") {
                    commitRename(); onOpenNewPage()
                }
            }
            }
        }
        .padding(.horizontal, 16)
        .frame(height: 64)
        .background(BoardTheme.bar)
        .shadow(color: Color.black.opacity(0.06), radius: 4, y: 2)
        .zIndex(1)
        .onChange(of: store.currentPageIndex) { _ in
            // Renaming one page and then stepping to another must not carry
            // the half-typed name across.
            isRenaming = false
        }
        .onChange(of: store.isEditMode) { _ in isRenaming = false }
    }

    /// Player: the page name, bigger and nudged right, since the right-hand
    /// side of the bar has no buttons any more. Tapping it opens the page list.
    private var playerTitle: some View {
        Button(action: onOpenPages) {
            HStack(spacing: 8) {
                Text(store.currentPage.title.uppercased())
                    .font(.system(size: 30, weight: .bold))
                    .tracking(3.5)
                    .foregroundColor(BoardTheme.ink)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                Image(systemName: "chevron.down")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(BoardTheme.inkSoft)
            }
        }
        .buttonStyle(PlainButtonStyle())
    }

    /// Editor: tap the name to rename it in place. The chevron beside it still
    /// opens the page list, so the editor can jump straight to any page.
    @ViewBuilder
    private var editorTitle: some View {
        if isRenaming {
            HStack(spacing: 8) {
                TextField("Page name", text: $draftTitle, onCommit: commitRename)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(BoardTheme.ink)
                    .multilineTextAlignment(.center)
                    .disableAutocorrection(true)
                    .focused($titleFieldFocused)
                    .padding(.horizontal, 12)
                    .frame(width: 280, height: 44)
                    .background(BoardTheme.sentence)
                    .cornerRadius(22)
                RoundBarButton(icon: "checkmark", fill: Color(hex: "#3CC47C"), tint: .white,
                               size: 40, label: "Done renaming", action: commitRename)
            }
        } else {
            HStack(spacing: 4) {
                Button(action: beginRename) {
                    HStack(spacing: 6) {
                        Text(store.currentPage.title.uppercased())
                            .font(.system(size: 24, weight: .bold))
                            .tracking(2.5)
                            .foregroundColor(BoardTheme.ink)
                            .lineLimit(1)
                            .minimumScaleFactor(0.6)
                        Image(systemName: "pencil")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(BoardTheme.inkSoft)
                    }
                    .frame(minHeight: 44)
                }
                .buttonStyle(PlainButtonStyle())
                .accessibilityLabel("Rename page")
                Button(action: onOpenPages) {
                    Image(systemName: "chevron.down")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(BoardTheme.inkSoft)
                        .frame(width: 36, height: 44)
                }
                .buttonStyle(PlainButtonStyle())
                .accessibilityLabel("Pages in this book")
            }
        }
    }

    private func beginRename() {
        draftTitle = store.currentPage.title
        isRenaming = true
        // The field is not in the hierarchy until the state flips, so focus
        // has to follow a beat later or the keyboard never comes up.
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) { titleFieldFocused = true }
    }

    /// Writes the draft back. An empty name is dropped rather than saved - a
    /// page with no name is impossible to find in the page list.
    private func commitRename() {
        guard isRenaming else { return }
        isRenaming = false
        let trimmed = draftTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmed.isEmpty, trimmed != store.currentPage.title {
            store.currentPage.title = trimmed
        }
    }
}
