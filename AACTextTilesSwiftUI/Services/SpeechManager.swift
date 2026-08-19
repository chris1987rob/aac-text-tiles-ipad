import Foundation
import AVFoundation

public class SpeechManager: NSObject, ObservableObject, AVSpeechSynthesizerDelegate {
    public static let shared = SpeechManager()

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
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio, options: [.mixWithOthers, .duckOthers])
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("Failed to set AVAudioSession category: \(error)")
        }
    }

    public func speak(_ text: String, rate: Float = 0.5) {
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        synthesizer.stopSpeaking(at: .immediate)

        let utterance = AVSpeechUtterance(string: text)
        utterance.rate = rate
        utterance.pitchMultiplier = 1.0
        utterance.volume = 1.0

        if let voice = AVSpeechSynthesisVoice(language: "en-US") {
            utterance.voice = voice
        }

        isSpeaking = true
        synthesizer.speak(utterance)
    }

    public func playAudioData(_ data: Data) {
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
