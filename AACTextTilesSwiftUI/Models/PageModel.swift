import Foundation
import SwiftUI

public enum PageType: String, Codable, CaseIterable {
    case grid = "Standard Grid"
    case scene = "Visual Scene Display"
    case keyboard = "Talking Keyboard"

    /// What a person is shown. The raw values above are written into every saved
    /// board, so renaming one would strand existing pages on decode - the label
    /// changes here instead.
    public var displayName: String {
        switch self {
        case .grid:     return "Standard Grid"
        case .scene:    return "Visual Scene Display"
        case .keyboard: return "Symbol Keyboard"
        }
    }
}

public struct PageModel: Identifiable, Codable, Equatable {
    public var id: UUID
    public var title: String
    public var type: PageType
    public var gridSize: Int // 1, 2, 4, 9, 16, 25, 36
    public var bgHex: String
    public var enabled: Bool
    public var express: Bool
    public var tiles: [Int: TileModel]
    public var hotspots: [HotspotModel]
    public var scenePresetKey: String?
    public var sceneImageData: Data?

    /// Symbol keyboard only: how many picture keys fill one screen. The keys
    /// are then sized to use every point of space under the sentence bar, so
    /// this is really "how big do you want the pictures" - fewer keys, bigger
    /// pictures. Optional because boards saved before the keyboard had any
    /// settings must still decode.
    public var keyboardKeys: Int?

    /// Symbol keyboard only: which word groups are offered, by group id
    /// ("People", "Actions", ...). nil means all of them - a child starting out
    /// may only cope with two.
    public var keyboardGroups: [String]?

    /// Changes a parent has made to individual keys on THIS keyboard page,
    /// keyed by the word's id.
    ///
    /// Keyboard keys come from `SymbolWordBank`, which derives the vocabulary
    /// from the template boards - they are not `tiles`, they belong to no page,
    /// and until now that meant tapping one in the page editor did nothing at
    /// all. Storing the changes per page rather than editing the word bank
    /// keeps the bank a fixed vocabulary while letting one page say "Mum" where
    /// another says "Mom".
    ///
    /// **Optional on purpose** - synthesized `Codable` uses `decodeIfPresent`
    /// for optionals, so every board saved before this still decodes.
    public var keyboardEdits: [String: KeyboardKeyEdit]?

    public init(
        id: UUID = UUID(),
        title: String = "New Page",
        type: PageType = .grid,
        gridSize: Int = 4,
        bgHex: String = "#FFFFFF",
        enabled: Bool = true,
        express: Bool = false,
        tiles: [Int: TileModel] = [:],
        hotspots: [HotspotModel] = [],
        scenePresetKey: String? = nil,
        sceneImageData: Data? = nil,
        keyboardKeys: Int? = nil,
        keyboardGroups: [String]? = nil,
        keyboardEdits: [String: KeyboardKeyEdit]? = nil
    ) {
        self.id = id
        self.title = title
        self.type = type
        self.gridSize = gridSize
        self.bgHex = bgHex
        self.enabled = enabled
        self.express = express
        self.tiles = tiles
        self.hotspots = hotspots
        self.scenePresetKey = scenePresetKey
        self.sceneImageData = sceneImageData
        self.keyboardKeys = keyboardKeys
        self.keyboardGroups = keyboardGroups
        self.keyboardEdits = keyboardEdits
    }
}

/// One key on one keyboard page, changed.
///
/// Every field is optional and means "leave this as the word bank has it".
/// A key the parent has not touched stores nothing, so a page with one edited
/// word does not write out all ninety-five.
public struct KeyboardKeyEdit: Codable, Equatable {
    public var label: String?
    public var tts: String?
    public var icon: String?
    public var colorHex: String?
    public var photoData: Data?
    public var audioData: Data?
    /// Takes the word off this page without touching any other page.
    public var hidden: Bool?

    public init(
        label: String? = nil,
        tts: String? = nil,
        icon: String? = nil,
        colorHex: String? = nil,
        photoData: Data? = nil,
        audioData: Data? = nil,
        hidden: Bool? = nil
    ) {
        self.label = label
        self.tts = tts
        self.icon = icon
        self.colorHex = colorHex
        self.photoData = photoData
        self.audioData = audioData
        self.hidden = hidden
    }

    /// Nothing left to remember. An edit that has been reset back to the word
    /// bank's own values is dropped rather than saved as an empty object.
    public var isEmpty: Bool {
        label == nil && tts == nil && icon == nil && colorHex == nil
            && photoData == nil && audioData == nil && (hidden == nil || hidden == false)
    }
}
