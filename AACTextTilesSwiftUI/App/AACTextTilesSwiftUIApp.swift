import SwiftUI
import AVFoundation

@main
struct AACTextTilesSwiftUIApp: App {
    init() {
        // Configure AVAudioSession for full AAC speech output (plays even if iPad silent switch is on)
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio, options: [.mixWithOthers, .duckOthers])
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("Failed to set AVAudioSession category: \(error)")
        }

        // Keep iPad screen awake during communication
        UIApplication.shared.isIdleTimerDisabled = true
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
