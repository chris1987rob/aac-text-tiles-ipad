import SwiftUI

public struct TileView: View {
    /// Touch-access settings, passed in rather than read from a singleton so a
    /// tile stays previewable and testable on its own.
    public var activationDelay: Double = 0
    public var activateOnRelease: Bool = false

    public let slotId: Int
    public let tile: TileModel?
    public let isEditMode: Bool
    public let cellWidth: CGFloat
    public let cellHeight: CGFloat
    public let onTap: () -> Void

    @State private var isPressed: Bool = false
    @State private var dwellWork: DispatchWorkItem? = nil
    @State private var didFire: Bool = false

    public init(
        activationDelay: Double = 0,
        activateOnRelease: Bool = false,
        slotId: Int,
        tile: TileModel?,
        isEditMode: Bool,
        cellWidth: CGFloat = 160,
        cellHeight: CGFloat = 160,
        onTap: @escaping () -> Void
    ) {
        self.activationDelay = activationDelay
        self.activateOnRelease = activateOnRelease
        self.slotId = slotId
        self.tile = tile
        self.isEditMode = isEditMode
        self.cellWidth = cellWidth
        self.cellHeight = cellHeight
        self.onTap = onTap
    }

    public var body: some View {
        let minDim = min(cellWidth, cellHeight)
        // Rounder than before: the reference look is soft pastel cards.
        let cornerRadius = min(26.0, max(10.0, minDim * 0.13))
        let pad = max(4.0, min(14.0, minDim * 0.05))

        Group {
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
                    // The border colour a person picked is kept, but drawn
                    // faintly: the pastel look is a soft card, not an outlined
                    // box. A press still lights the edge up green.
                    .overlay(
                        RoundedRectangle(cornerRadius: cornerRadius)
                            .stroke(
                                isPressed ? Color(hex: "#00E676") : Color(hex: t.borderHex).opacity(0.35),
                                lineWidth: isPressed ? 4 : 1.5
                            )
                    )
                    .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
                    .shadow(color: Color.black.opacity(0.07), radius: 6, x: 0, y: 3)
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
        .contentShape(Rectangle())
        // In the editor a press opens the editor: no dwell, no lockout, since
        // those exist to protect a child using the board, not an adult building it.
        .gesture(isEditMode
                 ? nil
                 : DragGesture(minimumDistance: 0)
                     .onChanged { _ in if !isPressed { pressDown() } }
                     .onEnded { _ in pressUp() })
        .onTapGesture { if isEditMode { onTap() } }
    }

    // MARK: - Press handling

    private func pressDown() {
        didFire = false
        withAnimation(.easeInOut(duration: 0.12)) { isPressed = true }

        if activationDelay > 0 {
            // Dwell: the finger has to stay put. Lifting early cancels, which
            // is the whole point - a resting hand or a brushing sleeve no
            // longer speaks.
            let work = DispatchWorkItem { fire() }
            dwellWork = work
            DispatchQueue.main.asyncAfter(deadline: .now() + activationDelay, execute: work)
        } else if !activateOnRelease {
            fire()
        }
    }

    private func pressUp() {
        dwellWork?.cancel()
        dwellWork = nil
        if activationDelay == 0 && activateOnRelease { fire() }
        withAnimation(.easeInOut(duration: 0.12)) { isPressed = false }
    }

    private func fire() {
        guard !didFire else { return }
        didFire = true
        onTap()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            withAnimation(.easeInOut(duration: 0.12)) { isPressed = false }
        }
    }

    @ViewBuilder
    private func labelView(_ t: TileModel, hasSymbol: Bool, minDim: CGFloat) -> some View {
        let baseSize: CGFloat = hasSymbol ? min(32, max(14, minDim * 0.13)) : min(44, max(18, minDim * 0.22))
        Text(t.label)
            .font(.system(size: baseSize * t.labelSize, weight: .semibold))
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
                // Anything else is drawn as-is when it is an emoji, so a button
                // can carry any picture without extending this switch. Only a
                // genuinely unknown name falls through to the star.
                if TileView.isEmoji(t.symbolName) {
                    Text(t.symbolName ?? "").font(.system(size: symSize))
                } else if let name = t.symbolName, let img = SymbolLibrary.image(named: name) {
                    // A bundled Mulberry symbol. The switch above only ever
                    // covered twenty hardcoded words; the library carries
                    // thousands and is what the editor now picks from.
                    Image(uiImage: img)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: symSize * 1.5, height: symSize * 1.5)
                } else {
                    Image(systemName: "star.fill")
                        .font(.system(size: symSize * 0.75))
                        .foregroundColor(Color(hex: "#008369"))
                }
            }
        } else {
            Spacer(minLength: 0)
        }
    }
}

extension TileView {
    static func isEmoji(_ raw: String?) -> Bool {
        guard let raw = raw, !raw.isEmpty else { return false }
        return raw.unicodeScalars.contains { $0.properties.isEmoji && $0.value > 0x238C }
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

    /// The round trip back out, so a colour chosen with the system picker can be
    /// stored in the same "#RRGGBB" field every page background already uses.
    var hexString: String {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        UIColor(self).getRed(&r, green: &g, blue: &b, alpha: &a)
        let clamp: (CGFloat) -> Int = { Int((max(0, min(1, $0)) * 255).rounded()) }
        return String(format: "#%02X%02X%02X", clamp(r), clamp(g), clamp(b))
    }
}
