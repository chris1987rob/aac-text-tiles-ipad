import Foundation

/// A button kept aside so it can be put on another page without building it
/// again.
///
/// The expensive part of a button is never the word - it is the photograph of
/// the actual dog and the recording of the actual parent saying "Mom". Those
/// used to live in exactly one slot on exactly one page; putting "Mom" on a
/// second page meant taking the photo again and recording the voice again, and
/// the two copies then drifted apart.
///
/// Everything a `TileModel` carries is stored except its slot id, which belongs
/// to the page it came from, not to the button.
public struct SavedTile: Codable, Identifiable, Equatable {
    public var id: String
    public var name: String
    public var label: String
    public var tts: String
    public var symbolName: String?
    public var photoData: Data?
    public var audioData: Data?
    public var bgHex: String
    public var borderHex: String
    public var labelHex: String
    public var labelSize: Double
    public var isSoundItOut: Bool
    public var labelPositionTop: Bool
    public var savedAt: Date

    public init(
        id: String = UUID().uuidString,
        name: String,
        label: String,
        tts: String,
        symbolName: String? = nil,
        photoData: Data? = nil,
        audioData: Data? = nil,
        bgHex: String = "#FFFFFF",
        borderHex: String = "#CBD5E1",
        labelHex: String = "#1E293B",
        labelSize: Double = 1.0,
        isSoundItOut: Bool = false,
        labelPositionTop: Bool = false,
        savedAt: Date = Date()
    ) {
        self.id = id
        self.name = name
        self.label = label
        self.tts = tts
        self.symbolName = symbolName
        self.photoData = photoData
        self.audioData = audioData
        self.bgHex = bgHex
        self.borderHex = borderHex
        self.labelHex = labelHex
        self.labelSize = labelSize
        self.isSoundItOut = isSoundItOut
        self.labelPositionTop = labelPositionTop
        self.savedAt = savedAt
    }

    /// What the button looks like when it is put back on a page. The slot id
    /// comes from wherever it is being dropped, never from the saved copy.
    public func tile(inSlot slot: Int) -> TileModel {
        TileModel(
            id: slot,
            label: label,
            tts: tts,
            symbolName: symbolName,
            photoData: photoData,
            bgHex: bgHex,
            borderHex: borderHex,
            labelHex: labelHex,
            labelSize: labelSize,
            audioData: audioData,
            isSoundItOut: isSoundItOut,
            labelPositionTop: labelPositionTop
        )
    }

    public var hasPhoto: Bool { photoData != nil }
    public var hasRecording: Bool { audioData != nil }
}

/// The saved-buttons library. Pure Foundation on purpose - `Tools/StoreTests`
/// compiles for macOS and cannot see UIKit, so anything with a UIImage in it
/// would take this file out of the tests.
public final class TileFavorites: ObservableObject {
    public static let shared = TileFavorites()

    @Published public private(set) var items: [SavedTile] = []

    /// Photos and recordings make these rows heavy, so the library is capped.
    /// A parent who has saved 200 buttons cannot find one anyway.
    public static let limit = 200

    public init(load: Bool = true) {
        if load { self.items = TileFavorites.loadFromDisk() ?? [] }
    }

    public static var storeURL: URL {
        let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        return dir.appendingPathComponent("aac_favorites.json")
    }

    public static func loadFromDisk() -> [SavedTile]? {
        guard FileManager.default.fileExists(atPath: storeURL.path),
              let data = try? Data(contentsOf: storeURL),
              let decoded = try? JSONDecoder().decode([SavedTile].self, from: data) else { return nil }
        return decoded
    }

    public func save() {
        guard let data = try? JSONEncoder().encode(items) else { return }
        try? data.write(to: TileFavorites.storeURL, options: .atomic)
    }

    /// Saving the same button twice replaces the earlier copy rather than
    /// filling the library with near-duplicates. "Same" is the name a parent
    /// gave it, compared the way a person compares names.
    @discardableResult
    public func add(_ saved: SavedTile) -> SavedTile {
        var entry = saved
        if entry.name.trimmingCharacters(in: .whitespaces).isEmpty {
            entry.name = entry.label.isEmpty ? "Button" : entry.label
        }
        if let existing = items.firstIndex(where: {
            $0.name.caseInsensitiveCompare(entry.name) == .orderedSame
        }) {
            entry.id = items[existing].id
            items[existing] = entry
        } else {
            items.insert(entry, at: 0)
            if items.count > TileFavorites.limit { items.removeLast(items.count - TileFavorites.limit) }
        }
        save()
        return entry
    }

    public func add(from tile: TileModel, name: String? = nil) -> SavedTile {
        add(SavedTile(
            name: name ?? tile.label,
            label: tile.label,
            tts: tile.tts,
            symbolName: tile.symbolName,
            photoData: tile.photoData,
            audioData: tile.audioData,
            bgHex: tile.bgHex,
            borderHex: tile.borderHex,
            labelHex: tile.labelHex,
            labelSize: tile.labelSize,
            isSoundItOut: tile.isSoundItOut,
            labelPositionTop: tile.labelPositionTop
        ))
    }

    public func remove(id: String) {
        items.removeAll { $0.id == id }
        save()
    }

    public func remove(atOffsets offsets: IndexSet) {
        items.remove(atOffsets: offsets)
        save()
    }

    public func rename(id: String, to name: String) {
        guard let index = items.firstIndex(where: { $0.id == id }) else { return }
        items[index].name = name
        save()
    }

    public func contains(name: String) -> Bool {
        items.contains { $0.name.caseInsensitiveCompare(name) == .orderedSame }
    }

    /// Case- and position-insensitive, so "mom" finds "Mom" and "berr" finds
    /// "Blueberries".
    public func search(_ query: String) -> [SavedTile] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !q.isEmpty else { return items }
        return items.filter {
            $0.name.lowercased().contains(q)
                || $0.label.lowercased().contains(q)
                || $0.tts.lowercased().contains(q)
        }
    }
}
