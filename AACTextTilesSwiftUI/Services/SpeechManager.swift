import Foundation
import AVFoundation

public class SpeechManager: NSObject, ObservableObject, AVSpeechSynthesizerDelegate {
    public static let shared = SpeechManager()

    /// Rate used when a caller does not pass one. Settable so the Settings
    /// screen's speed slider affects every button on the board, not just the
    /// one preview it plays.
    public var defaultRate: Float = {
        let saved = UserDefaults.standard.double(forKey: "aac.speechRate")
        return saved > 0 ? Float(saved) : 0.5
    }()

    private let synthesizer = AVSpeechSynthesizer()
    private var audioPlayer: AVAudioPlayer?

    @Published public var isSpeaking: Bool = false
    @Published public var activeSyllable: String = ""

    override private init() {
        super.init()
        synthesizer.delegate = self
        setupAudioSession()
    }

    private func setupAudioSession() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .spokenAudio, options: [.duckOthers])
            try session.setActive(true, options: [])
        } catch {
            print("Failed to set AVAudioSession category: \(error)")
        }
    }

    /// The session can be deactivated by interruptions (calls, Siri, other apps).
    /// Re-activating before each utterance keeps AAC speech from silently failing.
    private func ensureSessionActive() {
        do {
            try AVAudioSession.sharedInstance().setActive(true, options: [])
        } catch {
            print("Failed to reactivate AVAudioSession: \(error)")
        }
    }

    /// The voice every utterance uses, chosen in Settings. Nil keeps the old
    /// behaviour of taking whatever the system gives for English.
    ///
    /// A child's voice is part of who they are, and the app used to speak for
    /// every child in the same default adult American one.
    public var preferredVoiceId: String? = nil

    /// Every English voice the iPad actually has, best-known first. iPadOS
    /// ships a range including child voices; this is a list the system hands us
    /// rather than anything the app has to carry.
    public static func availableVoices() -> [AVSpeechSynthesisVoice] {
        AVSpeechSynthesisVoice.speechVoices()
            .filter { $0.language.hasPrefix("en") }
            .sorted {
                if $0.language == $1.language { return $0.name < $1.name }
                // Whatever the iPad is set to comes first.
                let here = Locale.current.identifier.replacingOccurrences(of: "_", with: "-")
                if $0.language == here { return true }
                if $1.language == here { return false }
                return $0.language < $1.language
            }
    }

    private func resolvedVoice() -> AVSpeechSynthesisVoice? {
        if let want = preferredVoiceId,
           let match = AVSpeechSynthesisVoice.speechVoices().first(where: { $0.identifier == want }) {
            return match
        }
        return AVSpeechSynthesisVoice(language: "en-US")
            ?? AVSpeechSynthesisVoice.speechVoices().first { $0.language.hasPrefix("en") }
    }

    public func speak(_ text: String, rate: Float? = nil, voiceId: String? = nil) {
        let rate = rate ?? defaultRate
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        ensureSessionActive()
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }

        let utterance = AVSpeechUtterance(string: SpokenText.forSpeech(text))
        utterance.rate = rate
        utterance.pitchMultiplier = 1.0
        utterance.volume = 1.0

        if let want = voiceId,
           let match = AVSpeechSynthesisVoice.speechVoices().first(where: { $0.identifier == want }) {
            utterance.voice = match
        } else if let voice = resolvedVoice() {
            utterance.voice = voice
        }

        isSpeaking = true
        synthesizer.speak(utterance)
    }

    public func playAudioData(_ data: Data) {
        ensureSessionActive()
        do {
            audioPlayer = try AVAudioPlayer(data: data)
            audioPlayer?.play()
        } catch {
            print("Audio playback error: \(error)")
        }
    }

    public func soundItOut(word: String, completion: (() -> Void)? = nil) {
        let syllables = SyllableHelper.split(word: word)
        guard !syllables.isEmpty else {
            speak(word)
            completion?()
            return
        }

        var delay = 0.0
        for (index, syl) in syllables.enumerated() {
            DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
                self.activeSyllable = syl
                self.speak(syl, rate: 0.45)
            }
            delay += 0.75
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            self.activeSyllable = word
            self.speak(word, rate: 0.52)
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                self.activeSyllable = ""
                completion?()
            }
        }
    }

    public func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        DispatchQueue.main.async {
            self.isSpeaking = false
        }
    }
}

public struct SyllableHelper {
    public static func split(word: String) -> [String] {
        let w = word.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        switch w {
        case "butterfly": return ["but", "ter", "fly"]
        case "dinosaur": return ["di", "no", "saur"]
        case "computer": return ["com", "pu", "ter"]
        case "telephone": return ["tel", "e", "phone"]
        case "banana": return ["ba", "nan", "a"]
        case "elephant": return ["el", "e", "phant"]
        case "hospital": return ["hos", "pi", "tal"]
        case "refrigerator": return ["re", "frig", "er", "a", "tor"]
        case "playground": return ["play", "ground"]
        case "classroom": return ["class", "room"]
        case "water": return ["wa", "ter"]
        case "happy": return ["hap", "py"]
        case "morning": return ["morn", "ing"]
        default:
            if w.count <= 4 { return [w] }
            let mid = w.index(w.startIndex, offsetBy: w.count / 2)
            return [String(w[..<mid]), String(w[mid...])]
        }
    }
}
