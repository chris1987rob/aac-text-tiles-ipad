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

            templateShelf()

            Spacer(minLength: 12)
        }
        .background(Color(hex: "#F8FAFC"))
        .edgesIgnoringSafeArea(.top)
    }

    /// The boards that are not already in the book, one tap to add. Keeping
    /// them here rather than only behind Downloads means a parent setting the
    /// device up sees what is available without going looking for it.
    @ViewBuilder
    private func templateShelf() -> some View {
        let existing = Set(store.pages.map { $0.title.lowercased() })
        let available = PageTemplateCatalog.extras.filter { !existing.contains($0.title.lowercased()) }

        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Add a Board")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(Color(hex: "#1E293B"))
                Text("\(available.count) templates")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(Color(hex: "#64748B"))
                Spacer()
            }
            .padding(.horizontal, 24)

            if available.isEmpty {
                Text("Every template is already in your book.")
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "#64748B"))
                    .padding(.horizontal, 24)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(available) { template in
                            templateChip(template)
                        }
                    }
                    .padding(.horizontal, 24)
                    .padding(.bottom, 6)
                }
            }
        }
        .frame(maxWidth: 800)
    }

    private func templateChip(_ template: PageTemplate) -> some View {
        Button(action: {
            store.pages.append(template.makePage())
            store.currentPageIndex = store.pages.count - 1
            store.save()
        }) {
            VStack(alignment: .leading, spacing: 6) {
                Text(template.icon).font(.system(size: 30))
                Text(template.title)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(Color(hex: "#1E293B"))
                    .lineLimit(1)
                Text(template.summary)
                    .font(.system(size: 11))
                    .foregroundColor(Color(hex: "#64748B"))
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
                Spacer(minLength: 0)
                Text("+ \(template.buttonCount) buttons")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color(hex: template.accent))
                    .cornerRadius(6)
            }
            .padding(12)
            .frame(width: 168, height: 150, alignment: .topLeading)
            .background(Color.white)
            .cornerRadius(14)
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .stroke(Color(hex: template.accent).opacity(0.45), lineWidth: 1.5)
            )
            .shadow(color: Color.black.opacity(0.06), radius: 5, y: 2)
        }
        .buttonStyle(PlainButtonStyle())
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
