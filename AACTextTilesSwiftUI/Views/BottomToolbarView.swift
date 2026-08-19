import SwiftUI

public struct BottomToolbarView: View {
    @ObservedObject public var store: AACStore
    public let onOpenPages: () -> Void
    public let onOpenOptions: () -> Void
    public let onOpenNewPage: () -> Void
    public let onGoHome: () -> Void

    public var body: some View {
        HStack(spacing: 12) {
            // Left Action Group
            HStack(spacing: 8) {
                // Previous Arrow
                Button(action: { store.prevPage() }) {
                    Image(systemName: "arrowtriangle.backward.fill")
                        .font(.system(size: 20))
                        .foregroundColor(.white)
                        .frame(width: 44, height: 44)
                }

                // Next Arrow
                Button(action: { store.nextPage() }) {
                    Image(systemName: "arrowtriangle.forward.fill")
                        .font(.system(size: 20))
                        .foregroundColor(.white)
                        .frame(width: 44, height: 44)
                }

                // Orange Home Square Button
                Button(action: onGoHome) {
                    Image(systemName: "house.fill")
                        .font(.system(size: 20))
                        .foregroundColor(.white)
                        .frame(width: 44, height: 44)
                        .background(Color(hex: "#F27935"))
                        .cornerRadius(8)
                }

                // Editor Mode: Sliders / Options Button
                if store.isEditMode {
                    Button(action: onOpenOptions) {
                        Image(systemName: "slider.horizontal.3")
                            .font(.system(size: 22))
                            .foregroundColor(.white)
                            .frame(width: 44, height: 44)
                    }
                }
            }

            Spacer()

            // Center: Clickable Page Title Dropdown
            Button(action: onOpenPages) {
                HStack(spacing: 4) {
                    Text(store.currentPage.title)
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(.white)
                    Image(systemName: "chevron.down")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.white.opacity(0.85))
                }
            }

            Spacer()

            // Right Action Group
            HStack(spacing: 8) {
                // Layers / Pages Button
                Button(action: onOpenPages) {
                    Image(systemName: "square.stack.3d.up.fill")
                        .font(.system(size: 20))
                        .foregroundColor(.white)
                        .frame(width: 44, height: 44)
                }

                if store.isEditMode {
                    // Orange Add / New Page Button
                    Button(action: onOpenNewPage) {
                        Image(systemName: "plus")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.white)
                            .frame(width: 44, height: 44)
                            .background(Color(hex: "#F27935"))
                            .cornerRadius(8)
                    }
                } else {
                    // Green Play Page Button
                    Button(action: {
                        let tiles = store.currentPage.tiles.values
                        let phrase = tiles.map { $0.tts.isEmpty ? $0.label : $0.tts }.joined(separator: ", ")
                        SpeechManager.shared.speak(phrase)
                    }) {
                        Image(systemName: "play.fill")
                            .font(.system(size: 20))
                            .foregroundColor(.white)
                            .frame(width: 44, height: 44)
                            .background(Color(hex: "#00A86B"))
                            .cornerRadius(8)
                    }
                }
            }
        }
        .padding(.horizontal, 16)
        .frame(height: 60)
        .background(Color(hex: "#008369"))
        .shadow(color: Color.black.opacity(0.15), radius: 4, y: -2)
    }
}
