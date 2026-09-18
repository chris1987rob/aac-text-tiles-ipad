import Foundation

/// Turning what a button *says* into what the synthesiser is *given*.
///
/// Kept apart from `SpeechManager` because that class pulls in AVAudioSession
/// and is stubbed out in `Tools/StoreTests` - anything living inside it cannot
/// be tested. This is pure Foundation, so the tests exercise the real thing.
public enum SpokenText {

    /// Handed a lone capital letter, the voice reads out the letter's NAME
    /// rather than the word: the "I" key on the symbol keyboard said
    /// **"capital I"** instead of "I". Lowercasing is enough to fix it - the
    /// letter i is pronounced the same as the pronoun - and the key still
    /// *shows* a capital I, which is how the word is written.
    ///
    /// Only a single character is ever touched. Anything longer is a real word
    /// or phrase and must reach the synthesiser exactly as it was typed, or
    /// every button a parent wrote in capitals would start speaking wrong.
    public static func forSpeech(_ text: String) -> String {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count == 1, let c = trimmed.first, c.isUppercase else { return text }
        return trimmed.lowercased()
    }
}

// MARK: - Recorded voice

public extension SpokenText {

    /// The `AppSettings.voiceId` that means "Bella", the app's own recorded
    /// voice, rather than an `AVSpeechSynthesisVoice` identifier. Clips for
    /// her live in the bundle under `Voices/bella/`; see `VoiceClips`.
    static let bellaVoiceId = "talktiles:bella"

    /// The folder under `Voices/` for a recorded voice id, nil for a system
    /// voice. `talktiles:bella` -> `bella`.
    static func recordedVoiceFolder(_ voiceId: String?) -> String? {
        guard let id = voiceId, id.hasPrefix("talktiles:") else { return nil }
        let name = String(id.dropFirst("talktiles:".count))
        return name.isEmpty ? nil : name
    }

    /// The key a phrase is looked up by in a voice's clip index: lowercase,
    /// letters, digits and apostrophes kept, everything else collapsed to one
    /// space. "I Want!" and "i want" are the same clip.
    ///
    /// Tools/sync-native-assets.py writes the index with the same rule. If
    /// this changes, that must change with it, or every clip goes missing.
    static func normalisedPhrase(_ text: String) -> String {
        var out = ""
        var pendingSpace = false
        for scalar in text.lowercased().unicodeScalars {
            let keep = scalar.properties.isAlphabetic
                || CharacterSet.decimalDigits.contains(scalar)
                || scalar == "'"
                || scalar == "\u{2019}"
            if keep {
                if pendingSpace && !out.isEmpty { out.append(" ") }
                pendingSpace = false
                out.unicodeScalars.append(scalar == "\u{2019}" ? "'" : scalar)
            } else {
                pendingSpace = true
            }
        }
        return out
    }
}
