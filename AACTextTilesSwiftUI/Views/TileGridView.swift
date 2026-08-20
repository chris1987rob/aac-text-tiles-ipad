import SwiftUI

public struct TileGridView: View {
    @ObservedObject public var store: AACStore
    public let onSelectTile: (Int) -> Void

    public var body: some View {
        let p = store.currentPage

        GeometryReader { geometry in
            let isLandscape = geometry.size.width >= geometry.size.height
            let (cols, rows) = gridDimensions(for: p.gridSize, isLandscape: isLandscape)
            let spacing: CGFloat = spacingFor(gridSize: p.gridSize)
            let padding: CGFloat = paddingFor(gridSize: p.gridSize)

            let totalHSpacing = spacing * CGFloat(max(0, cols - 1))
            let totalVSpacing = spacing * CGFloat(max(0, rows - 1))

            let availableWidth = max(50, geometry.size.width - (padding * 2) - totalHSpacing)
            let availableHeight = max(50, geometry.size.height - (padding * 2) - totalVSpacing)

            let cellWidth = availableWidth / CGFloat(cols)
            let cellHeight = availableHeight / CGFloat(rows)

            VStack(spacing: spacing) {
                ForEach(0..<rows, id: \.self) { r in
                    HStack(spacing: spacing) {
                        ForEach(0..<cols, id: \.self) { c in
                            let slotId = r * cols + c + 1
                            if slotId <= p.gridSize {
                                let tile = p.tiles[slotId]
                                TileView(
                                    slotId: slotId,
                                    tile: tile,
                                    isEditMode: store.isEditMode,
                                    cellWidth: cellWidth,
                                    cellHeight: cellHeight,
                                    onTap: {
                                        if store.isEditMode {
                                            onSelectTile(slotId)
                                        } else if let t = tile, !t.label.isEmpty || !t.tts.isEmpty {
                                            if p.express {
                                                store.addExpressChip(t.label.isEmpty ? t.tts : t.label)
                                            }
                                            if let audioData = t.audioData {
                                                SpeechManager.shared.playAudioData(audioData)
                                            } else if t.isSoundItOut {
                                                SpeechManager.shared.soundItOut(word: t.tts.isEmpty ? t.label : t.tts)
                                            } else {
                                                SpeechManager.shared.speak(t.tts.isEmpty ? t.label : t.tts)
                                            }
                                        } else if store.isEditMode {
                                            onSelectTile(slotId)
                                        }
                                    }
                                )
                                .frame(width: cellWidth, height: cellHeight)
                            } else {
                                Color.clear
                                    .frame(width: cellWidth, height: cellHeight)
                            }
                        }
                    }
                }
            }
            .padding(padding)
            .frame(width: geometry.size.width, height: geometry.size.height, alignment: .center)
        }
    }

    private func gridDimensions(for size: Int, isLandscape: Bool) -> (cols: Int, rows: Int) {
        switch size {
        case 1:
            return (1, 1)
        case 2:
            return isLandscape ? (2, 1) : (1, 2)
        case 4:
            return (2, 2)
        case 6:
            return isLandscape ? (3, 2) : (2, 3)
        case 8:
            return isLandscape ? (4, 2) : (2, 4)
        case 9:
            return (3, 3)
        case 12:
            return isLandscape ? (4, 3) : (3, 4)
        case 16:
            return (4, 4)
        case 20:
            return isLandscape ? (5, 4) : (4, 5)
        case 25:
            return (5, 5)
        case 30:
            return isLandscape ? (6, 5) : (5, 6)
        case 36:
            return (6, 6)
        case 48:
            return isLandscape ? (8, 6) : (6, 8)
        default:
            let sq = Int(ceil(sqrt(Double(size))))
            return (sq, sq)
        }
    }

    private func spacingFor(gridSize: Int) -> CGFloat {
        if gridSize <= 4 { return 16 }
        if gridSize <= 9 { return 12 }
        if gridSize <= 16 { return 10 }
        if gridSize <= 25 { return 8 }
        return 6
    }

    private func paddingFor(gridSize: Int) -> CGFloat {
        if gridSize <= 4 { return 16 }
        if gridSize <= 9 { return 12 }
        if gridSize <= 16 { return 10 }
        return 8
    }
}
