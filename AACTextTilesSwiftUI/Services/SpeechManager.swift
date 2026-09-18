import Foundation
import AVFoundation

public class SpeechManager: NSObject, ObservableObject, AVSpeechSynthesizerDelegate, AVAudioPlayerDelegate {
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

    /// Clips still to play after `audioPlayer` finishes - a sentence Bella
    /// says one word at a time. See `VoiceClips.chain`.
    private var clipQueue: [URL] = []
    private var clipRate: Float = 1.0

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
        stopClips()

        // Bella (or another recorded voice): the clip that says exactly this,
        // else the word clips in order, else the iPad voice says it. A clip
        // is a real recording, so it starts instantly and never says
        // "capital I".
        let want = voiceId ?? preferredVoiceId
        if VoiceClips.isRecordedVoice(want), playClips(for: text, voiceId: want, rate: rate) {
            return
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
        stopClips()
        do {
            audioPlayer = try AVAudioPlayer(data: data)
            audioPlayer?.play()
        } catch {
            print("Audio playback error: \(error)")
        }
    }

    // MARK: - Recorded voice clips

    /// True when something started playing. False means "no clip for these
    /// words" and the caller should let the synthesiser have them.
    @discardableResult
    private func playClips(for text: String, voiceId: String?, rate: Float) -> Bool {
        var urls: [URL] = []
        if let one = VoiceClips.clip(for: text, voiceId: voiceId) {
            urls = [one]
        } else if let chain = VoiceClips.chain(for: text, voiceId: voiceId) {
            urls = chain
        }
        guard !urls.isEmpty else { return false }
        return playClipSequence(urls, rate: rate)
    }

    /// Plays clips back to back. Public so Settings can preview a voice.
    @discardableResult
    public func playClipSequence(_ urls: [URL], rate: Float? = nil) -> Bool {
        guard let first = urls.first else { return false }
        ensureSessionActive()
        if synthesizer.isSpeaking { synthesizer.stopSpeaking(at: .immediate) }
        clipQueue = Array(urls.dropFirst())
        clipRate = SpeechManager.clipRate(for: rate ?? defaultRate)
        return startClip(first)
    }

    private func startClip(_ url: URL) -> Bool {
        do {
            let player = try AVAudioPlayer(contentsOf: url)
            player.delegate = self
            if clipRate != 1.0 {
                player.enableRate = true
                player.rate = clipRate
            }
            audioPlayer = player
            isSpeaking = true
            return player.play()
        } catch {
            print("Clip playback error: \(error) \(url.lastPathComponent)")
            clipQueue.removeAll()
            isSpeaking = false
            return false
        }
    }

    private func stopClips() {
        clipQueue.removeAll()
        if let p = audioPlayer, p.isPlaying { p.stop() }
    }

    /// The Settings speed slider is an `AVSpeechUtterance` rate (0.3-0.7,
    /// 0.45 normal). A recording cannot take that number directly, so the
    /// three bands the slider labels (Slow / Normal / Fast) become playback
    /// multipliers. AVAudioPlayer keeps the pitch when the rate changes.
    static func clipRate(for speechRate: Float) -> Float {
        switch speechRate {
        case ..<0.38: return 0.8
        case ..<0.52: return 1.0
        default:      return 1.25
        }
    }

    public func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        guard player === audioPlayer else { return }
        if flag, !clipQueue.isEmpty {
            let next = clipQueue.removeFirst()
            _ = startClip(next)
        } else {
            clipQueue.removeAll()
            isSpeaking = false
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
