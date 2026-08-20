import SwiftUI

public struct HomeView: View {
    @ObservedObject public var store: AACStore
    public let onLaunchPlayer: () -> Void
    public let onLaunchEditor: () -> Void
    public let onOpenHelp: () -> Void
    public let onOpenSettings: () -> Void
    public let onOpenDownloads: () -> Void

    public var body: some View {
        VStack(spacing: 0) {
            // Teal Header with Signature Arch
            ZStack {
                Color(hex: "#008369")
                VStack {
                    HStack {
                        Text("talk tiles")
                            .font(.system(size: 34, weight: .black))
                            .foregroundColor(.white)
                        Text("NEW")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(Color(hex: "#008369"))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(Color.white)
                            .cornerRadius(12)
                    }
                }
            }
            .frame(height: 120)

            // Main Hub Body
            VStack(spacing: 20) {
                // High-visibility Orange Player Button
                Button(action: onLaunchPlayer) {
                    HStack(spacing: 16) {
                        Image(systemName: "play.circle.fill")
                            .font(.system(size: 44))
                        Text("Player")
                            .font(.system(size: 32, weight: .black))
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 110)
                    .background(Color(hex: "#F27935"))
                    .cornerRadius(20)
                    .shadow(color: Color.black.opacity(0.12), radius: 8, y: 4)
                }

                // 2x2 Secondary Action Grid
                LazyVGrid(columns: [GridItem(.flexible(), spacing: 16), GridItem(.flexible(), spacing: 16)], spacing: 16) {
                    // Page Editor
                    homeActionButton(title: "Page Editor", icon: "pencil.circle.fill", color: "#008369", action: onLaunchEditor)
                    // Settings
                    homeActionButton(title: "Settings", icon: "gearshape.fill", color: "#008369", action: onOpenSettings)
                    // Downloads
                    homeActionButton(title: "Downloads", icon: "arrow.down.circle.fill", color: "#008369", action: onOpenDownloads)
                    // Help
                    homeActionButton(title: "Help", icon: "questionmark.circle.fill", color: "#008369", action: onOpenHelp)
                }
            }
            .padding(24)
            .frame(maxWidth: 800)

            Spacer()

            // Bottom Footer
            HStack {
                Text("Default Book (GoTalk Now Edition)")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(Color(hex: "#64748B"))
            }
            .padding(.bottom, 20)
        }
        .background(Color(hex: "#F8FAFC"))
        .edgesIgnoringSafeArea(.top)
    }

    private func homeActionButton(title: String, icon: String, color: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 34))
                    .foregroundColor(Color(hex: color))
                Text(title)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(Color(hex: "#1E293B"))
            }
            .frame(maxWidth: .infinity)
            .frame(height: 120)
            .background(Color.white)
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(Color(hex: "#E2E8F0"), lineWidth: 1.5)
            )
            .shadow(color: Color.black.opacity(0.06), radius: 6, y: 2)
        }
    }
}
