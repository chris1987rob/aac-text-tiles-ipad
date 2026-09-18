import Foundation

/// One pressable word on the symbol keyboard.
public struct SymbolWord: Identifiable, Equatable {
    public let id: String
    public var label: String
    public var tts: String
    public var icon: String
    public var color: String
    /// Set only when a page has put a picture or a recorded voice on this key.
    /// The word bank itself never carries either - it is a fixed vocabulary,
    /// and a photograph of somebody's mother belongs to one page, not to every
    /// keyboard in the app.
    public var photoData: Data? = nil
    public var audioData: Data? = nil

    /// This word as one page has changed it. Every field of the edit is
    /// optional and `nil` means "leave the word bank's own value alone".
    public func applying(_ edit: KeyboardKeyEdit?) -> SymbolWord {
        guard let edit = edit else { return self }
        var out = self
        if let v = edit.label,    !v.isEmpty { out.label = v }
        if let v = edit.tts,      !v.isEmpty { out.tts = v }
        if let v = edit.icon,     !v.isEmpty { out.icon = v }
        if let v = edit.colorHex, !v.isEmpty { out.color = v }
        out.photoData = edit.photoData
        out.audioData = edit.audioData
        return out
    }
}

/// The symbol keyboard's vocabulary, derived from the template boards rather
/// than written out again here. Those boards are already a curated AAC
/// vocabulary with a picture and a Fitzgerald-key colour on every word, so a
/// second hand-written list would only be a worse copy that drifts out of step.
public enum SymbolWordBank {

    /// Groups are the Fitzgerald key: the colour a word carries in the template
    /// boards is its part of speech, and it is the same coding mainstream AAC
    /// systems use, so a child who learns "green means an action" here reads
    /// every other board the same way.
    public struct Group: Identifiable {
        public let id: String
        public let title: String
        public let color: String
        public let words: [SymbolWord]
    }

    private static let groupOrder: [(String, String)] = [
        (TemplateColor.person,     "People"),
        (TemplateColor.verb,       "Actions"),
        (TemplateColor.descriptor, "Describing"),
        (TemplateColor.noun,       "Things"),
        (TemplateColor.social,     "Social"),
        (TemplateColor.question,   "Questions"),
        (TemplateColor.negative,   "No & Stop")
    ]

    public static let groups: [Group] = {
        // A word can appear on several boards; the first sighting wins so the
        // keyboard never shows "more" twice with two different pictures.
        var seen = Set<String>()
        var byColor: [String: [SymbolWord]] = [:]

        for template in PageTemplateCatalog.all {
            for tile in template.tiles {
                guard let icon = tile.symbol, !icon.isEmpty else { continue }
                let key = tile.label.lowercased()
                guard !seen.contains(key) else { continue }
                seen.insert(key)
                byColor[tile.color, default: []].append(
                    SymbolWord(id: key, label: tile.label, tts: tile.tts, icon: icon, color: tile.color)
                )
            }
        }

        return groupOrder.compactMap { color, title in
            guard let words = byColor[color], !words.isEmpty else { return nil }
            return Group(id: title, title: title, color: color, words: words)
        }
    }()

    public static var totalWords: Int { groups.reduce(0) { $0 + $1.words.count } }
}

public extension SymbolWordBank {

    /// The groups a page actually offers. `nil` (a board saved before the
    /// keyboard had settings, or one that was never narrowed) means all of
    /// them; an id that no longer exists is dropped rather than shown empty.
    static func groups(limitedTo ids: [String]?) -> [Group] {
        guard let ids = ids, !ids.isEmpty else { return groups }
        let wanted = Set(ids)
        let kept = groups.filter { wanted.contains($0.id) }
        return kept.isEmpty ? groups : kept
    }

    /// **The same sizes a Standard Grid page offers**, deliberately. The
    /// keyboard used to have a set of its own (6/12/20/30/42), which meant the
    /// one screen in the app where you choose how big the pictures are asked
    /// the question two different ways depending on the page kind. If this
    /// list is ever changed, change `gridSizes` in the wizard with it.
    /// 12 is here because a dozen of the starter boards are laid out 3x4 and
    /// were saving `gridSize: 12` while this list did not offer it - which
    /// rendered the size control blank on every one of them.
    static let keyCountOptions: [Int] = [1, 2, 4, 9, 12, 16, 25, 36, 48]

    static let defaultKeyCount: Int = 16

    static func keyCountLabel(_ count: Int) -> String {
        switch count {
        case ...2:  return "Huge"
        case 4:     return "Very big"
        case 9:     return "Big"
        case 12:    return "Big"
        case 16:    return "Medium"
        case 25:    return "Small"
        case 36:    return "Very small"
        default:    return "Tiny"
        }
    }

    /// A segmented picker whose selection is not one of its tags shows nothing
    /// selected. Keyboard pages made before the sizes were brought into line
    /// hold counts like 20 that are no longer offered, so they snap to the
    /// nearest one rather than opening on a blank control.
    static func nearestKeyCount(_ count: Int?) -> Int {
        guard let count = count else { return defaultKeyCount }
        return keyCountOptions.min { abs($0 - count) < abs($1 - count) } ?? defaultKeyCount
    }
}
