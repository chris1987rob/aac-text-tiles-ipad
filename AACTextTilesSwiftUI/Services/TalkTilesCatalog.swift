import Foundation

/// One of our own pictures, as listed in `TalkTilesSymbols/catalog.json`.
public struct TalkTilesSymbol: Codable, Identifiable, Equatable {
    public let id: String
    public let label: String
    public let tts: String
    public let category: String
    public let tags: [String]
    public let emoji: String
}

/// The Talk Tiles picture set: 2,210 pictures drawn in-house (see
/// aac-board/symbol_gen/), one per vocabulary word, each with a recorded
/// clip of Bella saying it. Both the pictures and `catalog.json` are copied
/// into the bundle by Tools/sync-native-assets.py.
///
/// Pure Foundation on purpose - the search ranking is the part worth testing
/// and UIKit would keep it out of Tools/StoreTests.
public enum TalkTilesCatalog {

    /// The bundle folder both the catalogue and the pictures live in.
    public static let folder = "TalkTilesSymbols"

    public static let symbols: [TalkTilesSymbol] = {
        guard let url = Bundle.main.url(forResource: "catalog", withExtension: "json",
                                        subdirectory: folder),
              let data = try? Data(contentsOf: url),
              let list = try? JSONDecoder().decode([TalkTilesSymbol].self, from: data) else {
            return []
        }
        return list
    }()

    public static var isAvailable: Bool { !symbols.isEmpty }

    public static let byId: [String: TalkTilesSymbol] = {
        var out: [String: TalkTilesSymbol] = [:]
        for s in symbols where out[s.id] == nil { out[s.id] = s }
        return out
    }()

    /// Categories in catalogue order (core first), for the picker's chips.
    public static let categories: [String] = {
        var seen = Set<String>()
        var out: [String] = []
        for s in symbols where !s.category.isEmpty && !seen.contains(s.category) {
            seen.insert(s.category)
            out.append(s.category)
        }
        return out
    }()

    public static func categoryTitle(_ category: String) -> String {
        category.replacingOccurrences(of: "_", with: " ").capitalized
    }

    /// Ranked search over label, spoken text and tags. Exact and prefix
    /// matches on the label come first so "cat" is the cat, not "certificate".
    /// An empty query lists the category (or everything) in catalogue order.
    public static func search(_ query: String, category: String? = nil, limit: Int = 400) -> [TalkTilesSymbol] {
        let q = SpokenText.normalisedPhrase(query)
        let pool = symbols.filter { category == nil || $0.category == category }
        guard !q.isEmpty else { return Array(pool.prefix(limit)) }

        var exact: [TalkTilesSymbol] = [], prefix: [TalkTilesSymbol] = []
        var word: [TalkTilesSymbol] = [], tag: [TalkTilesSymbol] = [], loose: [TalkTilesSymbol] = []
        for s in pool {
            let label = SpokenText.normalisedPhrase(s.label)
            let tts = SpokenText.normalisedPhrase(s.tts)
            if label == q || tts == q { exact.append(s) }
            else if label.hasPrefix(q) || tts.hasPrefix(q) { prefix.append(s) }
            else if label.split(separator: " ").contains(where: { $0.hasPrefix(q) }) { word.append(s) }
            else if s.tags.contains(where: { $0.lowercased().hasPrefix(q) }) { tag.append(s) }
            else if label.contains(q) || tts.contains(q) { loose.append(s) }
            if exact.count + prefix.count + word.count + tag.count + loose.count >= limit * 2 { break }
        }
        return Array((exact + prefix + word + tag + loose).prefix(limit))
    }
}
