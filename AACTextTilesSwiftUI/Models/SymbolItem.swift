import Foundation

public struct SymbolItem: Identifiable, Hashable {
    public let id: String
    public let name: String
    public let category: String
    public let keywords: [String]
    public let emoji: String?
    public let svgFilename: String?

    public init(
        id: String,
        name: String,
        category: String = "Core",
        keywords: [String] = [],
        emoji: String? = nil,
        svgFilename: String? = nil
    ) {
        self.id = id
        self.name = name
        self.category = category
        self.keywords = keywords
        self.emoji = emoji
        self.svgFilename = svgFilename
    }
}
