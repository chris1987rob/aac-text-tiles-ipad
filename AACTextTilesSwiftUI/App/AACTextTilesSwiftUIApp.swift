import SwiftUI
import AVFoundation

@main
struct AACTextTilesSwiftUIApp: App {
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var store = AACStore()

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
            RootView(store: store)
        }
        // Ordinary saving is debounced by 0.4s onto a background queue, which
        // is right while a finger is dragging a hotspot and wrong when the app
        // is about to stop existing. `saveNow` has always been here and its own
        // comment said to use it when backgrounding; nothing ever did, so an
        // edit made in the last half-second before the app was swiped away was
        // simply lost.
        .onChange(of: scenePhase) { phase in
            if phase != .active {
                store.saveNow()
                store.saveSettings()
            }
        }
    }
}
