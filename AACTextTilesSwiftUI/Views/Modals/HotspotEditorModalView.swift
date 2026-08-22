import SwiftUI

public struct HotspotEditorModalView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject public var store: AACStore
    public let hotspot: HotspotModel

    @StateObject private var recorder = AudioRecorder()

    @State private var label: String = ""
    @State private var tts: String = ""
    @State private var style: HotspotStyle = .invisible
    @State private var action: HotspotAction = .tts
    @State private var width: Double = 25
    @State private var height: Double = 25
    @State private var audioData: Data? = nil
    @State private var jumpPageId: UUID? = nil

    private let minSizePct: Double = 6.0

    public var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Label")) {
                    TextField("Name on Photo (e.g. Cat, Sofa, TV)", text: $label)
                }

                // Size is editable as a number here as well as by dragging the
                // corner handle on the scene, because dragging to an exact size
                // on a moving photo is fiddly.
                Section(header: Text("Size")) {
                    sizeSlider(title: "Width", value: $width, axisOrigin: hotspot.x)
                    sizeSlider(title: "Height", value: $height, axisOrigin: hotspot.y)
                    Button("Reset to Default Size") {
                        width = 25
                        height = 25
                    }
                    .foregroundColor(Color(hex: "#0284C7"))
                }

                Section(header: Text("When Tapped")) {
                    Picker("Action Type", selection: $action) {
                        ForEach(HotspotAction.allCases, id: \.self) { act in
                            Text(act.rawValue).tag(act)
                        }
                    }
                    .pickerStyle(SegmentedPickerStyle())

                    if action == .tts {
                        TextField("What to speak when tapped", text: $tts)
                        playbackButton(
                            title: "Play Preview",
                            enabled: !spokenText.isEmpty
                        ) {
                            SpeechManager.shared.speak(spokenText)
                        }
                    }

                    if action == .recorded {
                        recordingControls()
                    }

                    if action == .jump {
                        Picker("Go to Page", selection: $jumpPageId) {
                            Text("None").tag(UUID?.none)
                            ForEach(store.pages) { page in
                                Text(page.title).tag(UUID?.some(page.id))
                            }
                        }
                    }
                }

                Section(header: Text("Appearance in Player Mode")) {
                    Picker("Style", selection: $style) {
                        ForEach(HotspotStyle.allCases, id: \.self) { st in
                            Text(st.rawValue).tag(st)
                        }
                    }
                    .pickerStyle(SegmentedPickerStyle())
                }

                Section {
                    Button("🗑 Delete Hotspot") {
                        store.currentPage.hotspots.removeAll { $0.id == hotspot.id }
                        store.save()
                        presentationMode.wrappedValue.dismiss()
                    }
                    .foregroundColor(.red)
                }
            }
            .navigationBarTitle("Edit Hotspot", displayMode: .inline)
            .navigationBarItems(
                leading: Button("Cancel") { presentationMode.wrappedValue.dismiss() },
                trailing: Button("Save") {
                    saveHotspot()
                    presentationMode.wrappedValue.dismiss()
                }
                .font(.headline)
                .foregroundColor(Color(hex: "#008369"))
            )
        }
        .onAppear {
            label = hotspot.label
            tts = hotspot.tts
            style = hotspot.style
            action = hotspot.action
            width = hotspot.w
            height = hotspot.h
            audioData = hotspot.audioData
            jumpPageId = hotspot.jumpPageId
        }
        .onReceive(recorder.$recordedData.compactMap { $0 }) { data in
            audioData = data
        }
    }

    private var spokenText: String {
        tts.isEmpty ? label : tts
    }

    // MARK: - Size

    /// Clamped so a hotspot can never be sized off the edge of the scene, which
    /// is the same rule the drag handle enforces.
    @ViewBuilder
    private func sizeSlider(title: String, value: Binding<Double>, axisOrigin: Double) -> some View {
        let upper = max(minSizePct + 1, 100.0 - axisOrigin)
        VStack(alignment: .leading, spacing: 2) {
            HStack {
                Text(title)
                Spacer()
                Text("\(Int(value.wrappedValue))%")
                    .foregroundColor(Color(hex: "#64748B"))
                    .font(.system(size: 15, weight: .semibold))
            }
            Slider(value: value, in: minSizePct...upper, step: 1)
        }
        .padding(.vertical, 2)
    }

    // MARK: - Audio

    @ViewBuilder
    private func recordingControls() -> some View {
        Button(action: toggleRecording) {
            HStack(spacing: 8) {
                Image(systemName: recorder.isRecording ? "stop.circle.fill" : "mic.circle.fill")
                    .font(.system(size: 22))
                Text(recorder.isRecording
                     ? "Stop Recording"
                     : (audioData == nil ? "Record Voice" : "Re-record Voice"))
            }
            .foregroundColor(recorder.isRecording ? .red : Color(hex: "#008369"))
        }

        playbackButton(title: "Play Recording", enabled: audioData != nil) {
            if let data = audioData {
                SpeechManager.shared.playAudioData(data)
            }
        }

        if audioData != nil {
            Button("Remove Recording") { audioData = nil }
                .foregroundColor(.red)
        }
    }

    @ViewBuilder
    private func playbackButton(title: String, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: "play.circle.fill")
                    .font(.system(size: 22))
                Text(title)
            }
            .foregroundColor(enabled ? Color(hex: "#0284C7") : Color(hex: "#94A3B8"))
        }
        .disabled(!enabled)
    }

    private func toggleRecording() {
        if recorder.isRecording {
            recorder.stopRecording()
        } else {
            recorder.startRecording()
        }
    }

    private func saveHotspot() {
        guard let idx = store.currentPage.hotspots.firstIndex(where: { $0.id == hotspot.id }) else { return }
        store.currentPage.hotspots[idx].label = label
        store.currentPage.hotspots[idx].tts = tts.isEmpty ? label : tts
        store.currentPage.hotspots[idx].style = style
        store.currentPage.hotspots[idx].action = action
        store.currentPage.hotspots[idx].audioData = audioData
        store.currentPage.hotspots[idx].jumpPageId = jumpPageId
        // Clamp on save too: x/y may have moved by dragging while this sheet
        // was open, which would otherwise let w push past the right edge.
        store.currentPage.hotspots[idx].w = min(width, 100.0 - store.currentPage.hotspots[idx].x)
        store.currentPage.hotspots[idx].h = min(height, 100.0 - store.currentPage.hotspots[idx].y)
        store.save()
    }
}
