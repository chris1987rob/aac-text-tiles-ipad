import Foundation
import SwiftUI

public enum HotspotStyle: String, Codable, CaseIterable {
    case invisible = "Invisible"
    case highlight = "Yellow Glow"
    case outline = "Dashed Box"
}

public enum HotspotAction: String, Codable, CaseIterable {
    case tts = "Text-to-Speech"
    case recorded = "Recorded Voice"
    case jump = "Jump to Page"
}

public struct HotspotModel: Identifiable, Codable, Equatable {
    public var id: Int
    public var x: Double // Percent (0..100)
    public var y: Double
    public var w: Double
    public var h: Double
    public var label: String
    public var tts: String
    public var style: HotspotStyle
    public var action: HotspotAction
    public var audioData: Data?
    public var jumpPageId: UUID?

    public init(
        id: Int,
        x: Double = 30,
        y: Double = 30,
        w: Double = 25,
        h: Double = 25,
        label: String = "Hotspot",
        tts: String = "Hotspot",
        style: HotspotStyle = .invisible,
        action: HotspotAction = .tts,
        audioData: Data? = nil,
        jumpPageId: UUID? = nil
    ) {
        self.id = id
        self.x = x
        self.y = y
        self.w = w
        self.h = h
        self.label = label
        self.tts = tts.isEmpty ? label : tts
        self.style = style
        self.action = action
        self.audioData = audioData
        self.jumpPageId = jumpPageId
    }
}
