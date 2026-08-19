import SwiftUI

public struct TileView: View {
    public let slotId: Int
    public let tile: TileModel?
    public let isEditMode: Bool
    public let onTap: () -> Void

    @State private var isPressed: Bool = false

    public init(slotId: Int, tile: TileModel?, isEditMode: Bool, onTap: @escaping () -> Void) {
        self.slotId = slotId
        self.tile = tile
        self.isEditMode = isEditMode
        self.onTap = onTap
    }

    public var body: some View {
        Button(action: {
            withAnimation(.easeInOut(duration: 0.15)) {
                isPressed = true
            }
            onTap()
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                isPressed = false
            }
        }) {
            ZStack {
                if let t = tile, (!t.label.isEmpty || t.symbolName != nil || t.photoData != nil) {
                    // Configured Tile
                    VStack(spacing: 6) {
                        if t.labelPositionTop && !t.label.isEmpty {
                            labelView(t)
                        }

                        // Image / Symbol / Emoji
                        symbolView(t)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)

                        if !t.labelPositionTop && !t.label.isEmpty {
                            labelView(t)
                        }
                    }
                    .padding(8)
                    .background(Color(hex: t.bgHex))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(
                                isPressed ? Color(hex: "#00E676") : Color(hex: t.borderHex),
                                lineWidth: isPressed ? 4 : 2
                            )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .shadow(color: Color.black.opacity(0.08), radius: 4, x: 0, y: 2)
                    .scaleEffect(isPressed ? 1.03 : 1.0)
                } else if isEditMode {
                    // Empty Editor Tile
                    VStack(spacing: 8) {
                        Image(systemName: "plus.circle.fill")
                            .font(.system(size: 32))
                            .foregroundColor(Color(hex: "#008369"))
                        Text("Tap to Add Button")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(Color(hex: "#64748B"))
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color(hex: "#F8FAFC"))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(style: StrokeStyle(lineWidth: 2, dash: [6]))
                            .foregroundColor(Color(hex: "#CBD5E1"))
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                } else {
                    // Empty Player Tile
                    Color.clear
                }
            }
        }
        .buttonStyle(PlainButtonStyle())
    }

    @ViewBuilder
    private func labelView(_ t: TileModel) -> some View {
        Text(t.label)
            .font(.system(size: 20 * t.labelSize, weight: .bold))
            .foregroundColor(Color(hex: t.labelHex))
            .lineLimit(2)
            .minimumScaleFactor(0.6)
            .multilineTextAlignment(.center)
    }

    @ViewBuilder
    private func symbolView(_ t: TileModel) -> some View {
        if let photoData = t.photoData, let uiImage = UIImage(data: photoData) {
            Image(uiImage: uiImage)
                .resizable()
                .scaledToFit()
        } else if let sym = t.symbolName {
            switch sym {
            case "eat": Text("🍎").font(.system(size: 48))
            case "water": Text("💧").font(.system(size: 48))
            case "yes": Text("✅").font(.system(size: 48))
            case "no": Text("❌").font(.system(size: 48))
            case "help": Text("🙋").font(.system(size: 48))
            case "happy": Text("😊").font(.system(size: 48))
            case "sad": Text("😢").font(.system(size: 48))
            case "more": Text("➕").font(.system(size: 48))
            case "stop": Text("🛑").font(.system(size: 48))
            case "bathroom": Text("🚻").font(.system(size: 48))
            case "play": Text("🧸").font(.system(size: 48))
            default:
                Image(systemName: "star.fill")
                    .font(.system(size: 40))
                    .foregroundColor(Color(hex: "#008369"))
            }
        } else {
            Spacer()
        }
    }
}

// Color Hex Extension
extension Color {
    init(hex: String) {
        let clean = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: clean).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch clean.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}
