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
