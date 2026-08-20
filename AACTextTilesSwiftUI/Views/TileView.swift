import SwiftUI

public struct TileView: View {
    public let slotId: Int
    public let tile: TileModel?
    public let isEditMode: Bool
    public let cellWidth: CGFloat
    public let cellHeight: CGFloat
    public let onTap: () -> Void

    @State private var isPressed: Bool = false

    public init(
        slotId: Int,
        tile: TileModel?,
        isEditMode: Bool,
        cellWidth: CGFloat = 160,
        cellHeight: CGFloat = 160,
        onTap: @escaping () -> Void
    ) {
        self.slotId = slotId
        self.tile = tile
        self.isEditMode = isEditMode
        self.cellWidth = cellWidth
        self.cellHeight = cellHeight
        self.onTap = onTap
    }

    public var body: some View {
        let minDim = min(cellWidth, cellHeight)
        let cornerRadius = min(22.0, max(8.0, minDim * 0.09))
        let pad = max(4.0, min(14.0, minDim * 0.05))

        Button(action: {
            withAnimation(.easeInOut(duration: 0.12)) {
                isPressed = true
            }
            onTap()
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                isPressed = false
            }
        }) {
            ZStack {
                if let t = tile, (!t.label.isEmpty || t.symbolName != nil || t.photoData != nil) {
                    let hasLabel = !t.label.isEmpty
                    let hasSymbol = t.symbolName != nil || t.photoData != nil

                    VStack(spacing: minDim * 0.03) {
                        if t.labelPositionTop && hasLabel {
                            labelView(t, hasSymbol: hasSymbol, minDim: minDim)
                        }

                        if hasSymbol {
                            symbolView(t, hasLabel: hasLabel, minDim: minDim)
                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                        }

                        if !t.labelPositionTop && hasLabel {
                            labelView(t, hasSymbol: hasSymbol, minDim: minDim)
                        }
                    }
                    .padding(pad)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color(hex: t.bgHex))
                    .overlay(
                        RoundedRectangle(cornerRadius: cornerRadius)
                            .stroke(
                                isPressed ? Color(hex: "#00E676") : Color(hex: t.borderHex),
                                lineWidth: isPressed ? 4 : max(2, minDim * 0.02)
                            )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
                    .shadow(color: Color.black.opacity(0.12), radius: 5, x: 0, y: 3)
                    .scaleEffect(isPressed ? 1.03 : 1.0)
                } else if isEditMode {
                    // Empty Editor Tile
                    VStack(spacing: 8) {
                        Image(systemName: "plus.circle.fill")
                            .font(.system(size: min(44, max(24, minDim * 0.22))))
                            .foregroundColor(Color(hex: "#008369"))
                        if minDim > 70 {
                            Text("Tap to Add")
                                .font(.system(size: min(17, max(12, minDim * 0.11)), weight: .bold))
                                .foregroundColor(Color(hex: "#64748B"))
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color(hex: "#F8FAFC"))
                    .overlay(
                        RoundedRectangle(cornerRadius: cornerRadius)
                            .stroke(style: StrokeStyle(lineWidth: 2, dash: [6]))
                            .foregroundColor(Color(hex: "#CBD5E1"))
                    )
                    .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
                } else {
                    // Empty Player Tile
                    Color.clear
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
        }
        .buttonStyle(PlainButtonStyle())
    }

    @ViewBuilder
    private func labelView(_ t: TileModel, hasSymbol: Bool, minDim: CGFloat) -> some View {
        let baseSize: CGFloat = hasSymbol ? min(32, max(14, minDim * 0.13)) : min(44, max(18, minDim * 0.22))
        Text(t.label)
            .font(.system(size: baseSize * t.labelSize, weight: .bold))
            .foregroundColor(Color(hex: t.labelHex))
            .lineLimit(2)
            .minimumScaleFactor(0.5)
            .multilineTextAlignment(.center)
    }

    @ViewBuilder
    private func symbolView(_ t: TileModel, hasLabel: Bool, minDim: CGFloat) -> some View {
        let symSize: CGFloat = hasLabel ? min(80, max(28, minDim * 0.38)) : min(110, max(36, minDim * 0.58))

        if let photoData = t.photoData, let uiImage = UIImage(data: photoData) {
            Image(uiImage: uiImage)
                .resizable()
                .scaledToFit()
        } else if let sym = t.symbolName?.lowercased() {
            switch sym {
            case "eat", "food": Text("🍎").font(.system(size: symSize))
            case "water", "drink": Text("💧").font(.system(size: symSize))
            case "yes": Text("✅").font(.system(size: symSize))
            case "no": Text("❌").font(.system(size: symSize))
            case "help": Text("🙋").font(.system(size: symSize))
            case "happy": Text("😊").font(.system(size: symSize))
            case "sad": Text("😢").font(.system(size: symSize))
            case "more": Text("➕").font(.system(size: symSize))
            case "stop": Text("🛑").font(.system(size: symSize))
            case "bathroom", "toilet": Text("🚻").font(.system(size: symSize))
            case "play", "toy": Text("🧸").font(.system(size: symSize))
            case "home", "house": Text("🏠").font(.system(size: symSize))
            case "school": Text("🏫").font(.system(size: symSize))
            case "sleep", "bed": Text("😴").font(.system(size: symSize))
            case "love", "like": Text("❤️").font(.system(size: symSize))
            case "dog": Text("🐶").font(.system(size: symSize))
            case "cat": Text("🐱").font(.system(size: symSize))
            case "book": Text("📖").font(.system(size: symSize))
            case "bus": Text("🚌").font(.system(size: symSize))
            case "music": Text("🎵").font(.system(size: symSize))
            default:
                Image(systemName: "star.fill")
                    .font(.system(size: symSize * 0.75))
                    .foregroundColor(Color(hex: "#008369"))
            }
        } else {
            Spacer(minLength: 0)
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
