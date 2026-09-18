import SwiftUI

/// The main menu, in the same pastel language as the board (2026-09-13): a
/// white bar with the app name in spaced capitals, then soft rounded cards on
/// the pale ground. Player is the big orange one because it is the one a
/// child reaches for; the four adult jobs are pastel cards underneath.
public struct HomeView: View {
    @ObservedObject public var store: AACStore
    public let onLaunchPlayer: () -> Void
    public let onLaunchEditor: () -> Void
    public let onOpenHelp: () -> Void
    public let onOpenSettings: () -> Void
    public let onOpenDownloads: () -> Void

    public var body: some View {
        VStack(spacing: 0) {
            // Top bar, matching the board's.
            HStack {
                Spacer()
                Text("TALK TILES")
                    .font(.system(size: 30, weight: .bold))
                    .tracking(3.5)
                    .foregroundColor(BoardTheme.ink)
                Spacer()
            }
            .frame(height: 64)
            .background(BoardTheme.bar)
            .shadow(color: Color.black.opacity(0.06), radius: 4, y: 2)
            .zIndex(1)

            // A thin rainbow band under the bar - the same ring the Play
            // button wears - so the menu is not a white bar on a grey ground.
            LinearGradient(gradient: Gradient(colors: [
                Color(hex: "#FF7A7A"), Color(hex: "#FFB35C"), Color(hex: "#FFE66D"),
                Color(hex: "#7AE582"), Color(hex: "#5CC8FF"), Color(hex: "#A78BFA")
            ]), startPoint: .leading, endPoint: .trailing)
            .frame(height: 6)

            // Main Hub Body
            VStack(spacing: 20) {
                // The big orange Player card.
                Button(action: onLaunchPlayer) {
                    HStack(spacing: 18) {
                        Image(systemName: "play.fill")
                            .font(.system(size: 32, weight: .bold))
                            .foregroundColor(BoardTheme.accent)
                            .frame(width: 74, height: 74)
                            .background(Color.white)
                            .clipShape(Circle())
                            .overlay(Circle().stroke(BoardTheme.rainbow, lineWidth: 6))
                        VStack(alignment: .leading, spacing: 2) {
                            Text("PLAYER")
                                .font(.system(size: 36, weight: .bold))
                                .tracking(3)
                            Text("Tap tiles to talk")
                                .font(.system(size: 17, weight: .semibold))
                                .opacity(0.9)
                        }
                        .foregroundColor(.white)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 130)
                    .background(
                        LinearGradient(gradient: Gradient(colors: [Color(hex: "#FF9A3C"), Color(hex: "#F5722B")]),
                                       startPoint: .topLeading, endPoint: .bottomTrailing)
                    )
                    .cornerRadius(28)
                    .shadow(color: Color(hex: "#F5722B").opacity(0.35), radius: 10, y: 5)
                }
                .buttonStyle(PlainButtonStyle())

                // 2x2 cards for the adult jobs, each in its own bright colour.
                LazyVGrid(columns: [GridItem(.flexible(), spacing: 16), GridItem(.flexible(), spacing: 16)], spacing: 16) {
                    homeCard(title: "Page Editor", subtitle: "Build and change pages",
                             icon: "pencil", color: "#4DB2FF", action: onLaunchEditor)
                    homeCard(title: "Settings", subtitle: "Voice, touch, child lock, backup",
                             icon: "gearshape.fill", color: "#9B7BFF", action: onOpenSettings)
                    homeCard(title: "Downloads", subtitle: "Ready-made boards to add",
                             icon: "arrow.down.to.line", color: "#2EC98A", action: onOpenDownloads)
                    homeCard(title: "Help", subtitle: "How everything works",
                             icon: "questionmark", color: "#FFC93C", action: onOpenHelp)
                }
            }
            .padding(24)
            .frame(maxWidth: 800)

            Spacer(minLength: 12)
        }
        .background(BoardTheme.background.edgesIgnoringSafeArea(.all))
    }

    private func homeCard(title: String, subtitle: String, icon: String, color: String,
                          action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(Color(hex: color))
                    .frame(width: 58, height: 58)
                    .background(Color.white)
                    .clipShape(Circle())
                    .shadow(color: Color.black.opacity(0.10), radius: 3, y: 2)
                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                        .font(.system(size: 22, weight: .bold))
                    Text(subtitle)
                        .font(.system(size: 14, weight: .medium))
                        .opacity(0.9)
                }
                .foregroundColor(.white)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 18)
            .frame(maxWidth: .infinity)
            .frame(height: 110)
            .background(Color(hex: color))
            .cornerRadius(24)
            .shadow(color: Color(hex: color).opacity(0.35), radius: 8, y: 4)
        }
        .buttonStyle(PlainButtonStyle())
    }
}
