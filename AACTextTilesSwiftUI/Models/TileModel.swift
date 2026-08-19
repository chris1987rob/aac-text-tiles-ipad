import Foundation
import SwiftUI

public struct TileModel: Identifiable, Codable, Equatable {
    public var id: Int // Slot ID (1..36)
    public var label: String
    public var tts: String
    public var symbolName: String?
    public var photoData: Data?
    public var bgHex: String
    public var borderHex: String
    public var labelHex: String
    public var labelSize: Double // 0.8 .. 2.0
    public var audioData: Data?
    public var isSoundItOut: Bool
    public var labelPositionTop: Bool

    public init(
        id: Int,
        label: String = "",
        tts: String = "",
        symbolName: String? = nil,
        photoData: Data? = nil,
        bgHex: String = "#FFFFFF",
        borderHex: String = "#CBD5E1",
        labelHex: String = "#1E293B",
        labelSize: Double = 1.0,
        audioData: Data? = nil,
        isSoundItOut: Bool = false,
        labelPositionTop: Bool = false
    ) {
        self.id = id
        self.label = label
        self.tts = tts.isEmpty ? label : tts
        self.symbolName = symbolName
        self.photoData = photoData
        self.bgHex = bgHex
        self.borderHex = borderHex
        self.labelHex = labelHex
        self.labelSize = labelSize
        self.audioData = audioData
        self.isSoundItOut = isSoundItOut
        self.labelPositionTop = labelPositionTop
    }
}
