import Foundation
import AVFoundation

public class AudioRecorder: NSObject, ObservableObject, AVAudioRecorderDelegate {
    private var recorder: AVAudioRecorder?
    private var tempAudioURL: URL?

    @Published public var isRecording: Bool = false
    @Published public var recordedData: Data?

    public func startRecording() {
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker])
            try session.setActive(true)

            let docDir = FileManager.default.temporaryDirectory
            tempAudioURL = docDir.appendingPathComponent(UUID().uuidString + ".m4a")

            let settings: [String: Any] = [
                AVFormatIDKey: Int(kAudioFormatMPEG4AAC),
                AVSampleRateKey: 44100,
                AVNumberOfChannelsKey: 1,
                AVEncoderAudioQualityKey: AVAudioQuality.high.rawValue
            ]

            if let url = tempAudioURL {
                recorder = try AVAudioRecorder(url: url, settings: settings)
                recorder?.delegate = self
                recorder?.record()
                isRecording = true
            }
        } catch {
            print("Recording start error: \(error)")
        }
    }

    public func stopRecording() {
        recorder?.stop()
        isRecording = false
        if let url = tempAudioURL, let data = try? Data(contentsOf: url) {
            self.recordedData = data
        }
    }
}
