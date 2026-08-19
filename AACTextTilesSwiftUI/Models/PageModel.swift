import Foundation
import SwiftUI

public enum PageType: String, Codable, CaseIterable {
    case grid = "Standard Grid"
    case scene = "Visual Scene Display"
    case keyboard = "Talking Keyboard"
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
        sceneImageData: Data? = nil
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
    }
}
