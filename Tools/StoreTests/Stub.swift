import Foundation
// SpeechManager pulls in AVAudioSession, which needs a real audio stack. The
// store only ever calls speak(), so a recording stub stands in and lets the
// test assert that the right words would have been spoken.
public final class SpeechManager {
    public static let shared = SpeechManager()
    public private(set) var spoken: [String] = []
    public func speak(_ text: String, rate: Float = 0.5) { spoken.append(text) }
    public func playAudioData(_ data: Data) { spoken.append("<audio:\(data.count)b>") }
    public func soundItOut(word: String, completion: (() -> Void)? = nil) { spoken.append("<sound:\(word)>") }
    public func reset() { spoken.removeAll() }
}
