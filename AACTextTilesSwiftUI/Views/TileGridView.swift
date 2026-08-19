import SwiftUI

public struct TileGridView: View {
    @ObservedObject public var store: AACStore
    public let onSelectTile: (Int) -> Void

    public var body: some View {
        let p = store.currentPage
        let columns = gridColumns(for: p.gridSize)

        LazyVGrid(columns: columns, spacing: 12) {
            ForEach(1...p.gridSize, id: \.self) { slotId in
                let tile = p.tiles[slotId]
                TileView(
                    slotId: slotId,
                    tile: tile,
                    isEditMode: store.isEditMode,
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
                .frame(minHeight: tileHeight(for: p.gridSize))
            }
        }
        .padding(14)
    }

    private func gridColumns(for size: Int) -> [GridItem] {
        let count: Int
        switch size {
        case 1: count = 1
        case 2: count = 2
        case 4: count = 2
        case 9: count = 3
        case 16: count = 4
        case 25: count = 5
        case 36: count = 6
        default: count = 2
        }
        return Array(repeating: GridItem(.flexible(), spacing: 12), count: count)
    }

    private func tileHeight(for size: Int) -> CGFloat {
        switch size {
        case 1, 2: return 240
        case 4: return 180
        case 9: return 130
        case 16: return 100
        case 25: return 80
        case 36: return 65
        default: return 140
        }
    }
}
