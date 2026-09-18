import Foundation

/// Pre-recorded clips for the app's own voices, shipped in the bundle under
/// `Voices/<voice>/` by Tools/sync-native-assets.py.
///
/// Each voice folder carries one clip per catalogue word, a `phrases/` folder
/// for the sentences the built-in boards speak ("My Schedule", "Can I have
/// more please"), and an `index.json` keyed by `SpokenText.normalisedPhrase`.
/// The lookup is by what the clip *says*, not by which picture is on the
/// button: a button that says "I want" gets the "I want" clip whatever
/// picture it carries, and one that just says "Want" does not.
public enum VoiceClips {

    public struct Voice: Equatable {
        public let id: String        // AppSettings.voiceId, e.g. "talktiles:bella"
        public let folder: String    // bundle folder under Voices/
        public let name: String
        public let description: String
    }

    private struct Index: Decodable {
        let voice: String
        let name: String
        let description: String?
        let clips: [String: String]
    }

    private static var loaded: [String: Index?] = [:]

    private static func index(for folder: String) -> Index? {
        if let hit = loaded[folder] { return hit }
        var result: Index? = nil
        if let url = Bundle.main.url(forResource: "index", withExtension: "json",
                                     subdirectory: "Voices/\(folder)"),
           let data = try? Data(contentsOf: url) {
            result = try? JSONDecoder().decode(Index.self, from: data)
        }
        loaded[folder] = result
        return result
    }

    /// Every recorded voice actually present in this build, Bella first.
    public static let available: [Voice] = {
        let root = Bundle.main.url(forResource: "Voices", withExtension: nil)
            ?? Bundle.main.resourceURL?.appendingPathComponent("Voices")
        guard let dir = root,
              let names = try? FileManager.default.contentsOfDirectory(atPath: dir.path) else { return [] }
        var out: [Voice] = []
        for folder in names.sorted() {
            guard let idx = index(for: folder), !idx.clips.isEmpty else { continue }
            out.append(Voice(id: "talktiles:\(folder)", folder: folder,
                             name: idx.name, description: idx.description ?? ""))
        }
        return out.sorted {
            if $0.id == SpokenText.bellaVoiceId { return true }
            if $1.id == SpokenText.bellaVoiceId { return false }
            return $0.name < $1.name
        }
    }()

    public static var isAvailable: Bool { !available.isEmpty }

    public static func voice(for voiceId: String?) -> Voice? {
        guard let folder = SpokenText.recordedVoiceFolder(voiceId) else { return nil }
        return available.first { $0.folder == folder }
    }

    /// True when the setting names one of our voices, whether or not its
    /// clips made it into this build - the caller then falls back to the iPad
    /// voice rather than to silence.
    public static func isRecordedVoice(_ voiceId: String?) -> Bool {
        SpokenText.recordedVoiceFolder(voiceId) != nil
    }

    /// The one clip that says exactly this text, if the voice has it.
    public static func clip(for text: String, voiceId: String?) -> URL? {
        guard let folder = SpokenText.recordedVoiceFolder(voiceId),
              let idx = index(for: folder) else { return nil }
        let key = SpokenText.normalisedPhrase(text)
        guard !key.isEmpty, let file = idx.clips[key] else { return nil }
        return url(file, folder: folder)
    }

    /// For a sentence no single clip says, the word clips in order - but only
    /// when every word has one. Half a sentence in Bella's voice and the rest
    /// in the iPad's would be worse than all of it in the iPad's.
    public static func chain(for text: String, voiceId: String?) -> [URL]? {
        guard let folder = SpokenText.recordedVoiceFolder(voiceId),
              let idx = index(for: folder) else { return nil }
        let words = SpokenText.normalisedPhrase(text).split(separator: " ").map(String.init)
        guard words.count >= 2 else { return nil }
        var out: [URL] = []
        for w in words {
            guard let file = idx.clips[w], let u = url(file, folder: folder) else { return nil }
            out.append(u)
        }
        return out
    }

    /// Something to play from the Settings "Test the voice" button.
    public static func previewClips(voiceId: String?) -> [URL] {
        if let one = clip(for: "Hello", voiceId: voiceId) {
            var out = [one]
            if let more = chain(for: "I want more please", voiceId: voiceId) { out += more }
            return out
        }
        return []
    }

    private static func url(_ file: String, folder: String) -> URL? {
        // `file` is "<id>.mp3" or "phrases/<name>.mp3", relative to the voice folder.
        let parts = file.split(separator: "/").map(String.init)
        guard let last = parts.last else { return nil }
        let name = (last as NSString).deletingPathExtension
        let ext = (last as NSString).pathExtension
        let sub = (["Voices", folder] + parts.dropLast()).joined(separator: "/")
        return Bundle.main.url(forResource: name, withExtension: ext, subdirectory: sub)
    }
}
