import Foundation

/// Whole-book backup and restore.
///
/// Everything a parent builds lived in one file inside the app's private
/// container, with nothing able to get it out. An iPad lost, broken or simply
/// replaced took months of work with it. A communication book is not something
/// a family can casually rebuild - it is the child's voice.
public enum BookBackup {

    /// Bumped only if the shape changes in a way an older app could not read.
    public static let formatVersion = 1

    public struct Archive: Codable {
        public var format: String
        public var version: Int
        public var createdAt: Date
        public var pages: [PageModel]
        public var settings: AppSettings?
    }

    public struct Summary {
        public let pages: Int
        public let buttons: Int
        public let hotspots: Int
        public let createdAt: Date
    }

    // MARK: - Writing

    /// Writes the whole book to a file in the temporary directory and hands
    /// back its URL for the share sheet. Photos and recordings are inside the
    /// page models already, so this really is everything.
    public static func write(pages: [PageModel], settings: AppSettings) throws -> URL {
        let archive = Archive(
            format: "talktiles.book",
            version: formatVersion,
            createdAt: Date(),
            pages: pages,
            settings: settings
        )

        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let data = try encoder.encode(archive)

        let stamp = Self.fileStamp()
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("TalkTiles-Backup-\(stamp).json")
        try data.write(to: url, options: .atomic)
        return url
    }

    // MARK: - Reading

    /// Reads an archive without applying it, so the app can say what is in the
    /// file before anything is overwritten. Restoring is destructive and a
    /// parent deserves to see what they are about to swap in.
    public static func read(from url: URL) throws -> (Archive, Summary) {
        // A file coming from Files or Mail is security-scoped; without this the
        // read fails with a permissions error that looks like a corrupt file.
        let needsScope = url.startAccessingSecurityScopedResource()
        defer { if needsScope { url.stopAccessingSecurityScopedResource() } }

        let data = try Data(contentsOf: url)
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601

        let archive: Archive
        if let full = try? decoder.decode(Archive.self, from: data) {
            archive = full
        } else {
            // A plain array of pages is what the single-page share produces and
            // what the very first backups would look like. Accept it rather
            // than telling a parent their file is broken.
            let pages = try decoder.decode([PageModel].self, from: data)
            archive = Archive(format: "talktiles.book", version: formatVersion,
                              createdAt: Date(), pages: pages, settings: nil)
        }

        guard !archive.pages.isEmpty else { throw BackupError.empty }

        let summary = Summary(
            pages: archive.pages.count,
            buttons: archive.pages.reduce(0) { $0 + $1.tiles.count },
            hotspots: archive.pages.reduce(0) { $0 + $1.hotspots.count },
            createdAt: archive.createdAt
        )
        return (archive, summary)
    }

    public enum BackupError: LocalizedError {
        case empty

        public var errorDescription: String? {
            switch self {
            case .empty: return "That file does not have any pages in it."
            }
        }
    }

    private static func fileStamp() -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd-HHmm"
        return f.string(from: Date())
    }
}
