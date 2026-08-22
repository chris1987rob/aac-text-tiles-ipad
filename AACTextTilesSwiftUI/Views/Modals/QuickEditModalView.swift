import SwiftUI
import UIKit

public struct QuickEditModalView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject public var store: AACStore
    public let slotId: IntItem

    @State private var label: String = ""
    @State private var tts: String = ""
    @State private var selectedSymbol: String? = nil
    @State private var selectedBgHex: String = "#FFFFFF"
    @State private var selectedBorderHex: String = "#CBD5E1"
    @State private var labelSize: Double = 1.0
    @State private var isSoundItOut: Bool = false
    @State private var photoData: Data? = nil
    @State private var isShowingPhotoOptions = false
    @State private var isShowingImagePicker = false
    @State private var pickerSourceType: UIImagePickerController.SourceType = .photoLibrary
    @State private var audioData: Data? = nil
    @StateObject private var recorder = AudioRecorder()

    private let colors = ["#FFFFFF", "#FFCDD2", "#FFE0B2", "#FFF9C4", "#C8E6C9", "#BBDEFB", "#D1C4E9", "#F8BBD0", "#E0E0E0", "#37474F"]

    public var body: some View {
        NavigationView {
            Form {
                // Section 1: Button Label & Speech
                Section(header: Text("Button Label & Speech Text")) {
                    TextField("Button Label (e.g. Apple, Help, Water)", text: $label)
                    TextField("Spoken Speech Text", text: $tts)
                    Toggle("Sound It Out Phonics (Syllable voice)", isOn: $isSoundItOut)
                }

                // Section 1b: Hear it before saving. Previously there was no way
                // to check what a button would say without leaving the editor,
                // and a recorded voice could not be listened back to at all.
                Section(header: Text("Voice")) {
                    playbackButton(title: "Play Preview", enabled: !spokenText.isEmpty) {
                        if isSoundItOut {
                            SpeechManager.shared.soundItOut(word: spokenText)
                        } else {
                            SpeechManager.shared.speak(spokenText)
                        }
                    }

                    Button(action: toggleRecording) {
                        HStack(spacing: 8) {
                            Image(systemName: recorder.isRecording ? "stop.circle.fill" : "mic.circle.fill")
                                .font(.system(size: 22))
                            Text(recorder.isRecording
                                 ? "Stop Recording"
                                 : (audioData == nil ? "Record Own Voice" : "Re-record Own Voice"))
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

                // Section 2: Button Picture
                Section(header: Text("Button Picture")) {
                    HStack(spacing: 14) {
                        picturePreview()
                        VStack(alignment: .leading, spacing: 4) {
                            Text(photoData != nil ? "Picture set" : "No picture")
                                .font(.system(size: 15, weight: .semibold))
                            Text(photoData != nil
                                 ? "The picture replaces the symbol on this button."
                                 : "Use a photo of the real object or person.")
                                .font(.system(size: 12))
                                .foregroundColor(Color(hex: "#64748B"))
                        }
                        Spacer()
                    }
                    .padding(.vertical, 2)

                    Button(action: { isShowingPhotoOptions = true }) {
                        HStack(spacing: 8) {
                            Image(systemName: "camera.fill")
                            Text(photoData != nil ? "Change Picture" : "Add Picture")
                        }
                        .foregroundColor(Color(hex: "#008369"))
                    }
                }

                // Section 3: Symbol Selection
                Section(header: Text("Symbol & Icon")) {
                    HStack(spacing: 12) {
                        symbolChip(name: "eat", emoji: "🍎")
                        symbolChip(name: "water", emoji: "💧")
                        symbolChip(name: "yes", emoji: "✅")
                        symbolChip(name: "no", emoji: "❌")
                        symbolChip(name: "help", emoji: "🙋")
                        symbolChip(name: "happy", emoji: "😊")
                    }
                    .padding(.vertical, 4)
                }

                // Section 4: Color Scheme
                Section(header: Text("Button Color Scheme")) {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 10) {
                            ForEach(colors, id: \.self) { hex in
                                Circle()
                                    .fill(Color(hex: hex))
                                    .frame(width: 38, height: 38)
                                    .overlay(
                                        Circle()
                                            .stroke(selectedBgHex == hex ? Color(hex: "#008369") : Color(hex: "#CBD5E1"), lineWidth: selectedBgHex == hex ? 3 : 1)
                                    )
                                    .onTapGesture {
                                        selectedBgHex = hex
                                    }
                            }
                        }
                    }
                }

                // Section 5: Word Size
                Section(header: Text("Word Size (\(String(format: "%.1fx", labelSize)))")) {
                    Slider(value: $labelSize, in: 0.8...2.0, step: 0.1)
                }
            }
            .navigationBarTitle("Quick Edit Button \(slotId.value)", displayMode: .inline)
            .navigationBarItems(
                leading: Button("Cancel") { presentationMode.wrappedValue.dismiss() },
                trailing: Button("Save") {
                    saveTile()
                    presentationMode.wrappedValue.dismiss()
                }
                .font(.headline)
                .foregroundColor(Color(hex: "#008369"))
            )
        }
        .actionSheet(isPresented: $isShowingPhotoOptions) {
            var buttons: [ActionSheet.Button] = [
                .default(Text("📷 Take Photo with Camera")) {
                    pickerSourceType = UIImagePickerController.isSourceTypeAvailable(.camera) ? .camera : .photoLibrary
                    isShowingImagePicker = true
                },
                .default(Text("🖼️ Choose from Photo Library")) {
                    pickerSourceType = .photoLibrary
                    isShowingImagePicker = true
                }
            ]
            if photoData != nil {
                buttons.append(.destructive(Text("Remove Picture")) { photoData = nil })
            }
            buttons.append(.cancel())
            return ActionSheet(title: Text("Button Picture"), buttons: buttons)
        }
        .sheet(isPresented: $isShowingImagePicker) {
            ImagePicker(sourceType: pickerSourceType) { img in
                // Tiles render small; 1024 keeps board.json from ballooning
                // while still looking sharp on the iPad's display.
                let sized = img.downscaled(maxDimension: 1024)
                if let data = sized.jpegData(compressionQuality: 0.85) {
                    photoData = data
                    selectedSymbol = nil
                }
            }
        }
        .onReceive(recorder.$recordedData.compactMap { $0 }) { data in
            audioData = data
        }
        .onAppear {
            if let tile = store.currentPage.tiles[slotId.value] {
                label = tile.label
                tts = tile.tts
                selectedSymbol = tile.symbolName
                selectedBgHex = tile.bgHex
                selectedBorderHex = tile.borderHex
                labelSize = tile.labelSize
                isSoundItOut = tile.isSoundItOut
                photoData = tile.photoData
                audioData = tile.audioData
            }
        }
    }

    @ViewBuilder
    private func picturePreview() -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: 10)
                .fill(Color(hex: "#F8FAFC"))
                .frame(width: 74, height: 74)
            RoundedRectangle(cornerRadius: 10)
                .stroke(Color(hex: "#CBD5E1"), lineWidth: 1)
                .frame(width: 74, height: 74)

            if let data = photoData, let ui = UIImage(data: data) {
                Image(uiImage: ui)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 74, height: 74)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
            } else {
                Image(systemName: "photo")
                    .font(.system(size: 26))
                    .foregroundColor(Color(hex: "#94A3B8"))
            }
        }
    }

    private func symbolChip(name: String, emoji: String) -> some View {
        Button(action: {
            selectedSymbol = name
            // Photo and symbol are mutually exclusive - TileView draws the photo
            // in preference to the symbol, so leaving a photo set would make
            // choosing a symbol look like it did nothing.
            photoData = nil
            if label.isEmpty { label = name.capitalized }
        }) {
            Text(emoji)
                .font(.system(size: 32))
                .padding(8)
                .background(selectedSymbol == name ? Color(hex: "#E6F4EA") : Color(hex: "#F8FAFC"))
                .cornerRadius(10)
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(selectedSymbol == name ? Color(hex: "#008369") : Color.clear, lineWidth: 2)
                )
        }
    }

    private var spokenText: String {
        tts.isEmpty ? label : tts
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

    private func saveTile() {
        // Carry forward everything this screen does not edit. Building a fresh
        // TileModel here used to drop photoData, audioData, labelHex and
        // labelPositionTop back to their defaults - so putting a photo or a
        // recorded voice on a tile and then changing its label silently
        // destroyed both.
        let existing = store.currentPage.tiles[slotId.value]

        let updated = TileModel(
            id: slotId.value,
            label: label,
            tts: tts.isEmpty ? label : tts,
            symbolName: selectedSymbol,
            photoData: photoData,
            bgHex: selectedBgHex,
            borderHex: selectedBorderHex,
            labelHex: existing?.labelHex ?? "#1E293B",
            labelSize: labelSize,
            audioData: audioData,
            isSoundItOut: isSoundItOut,
            labelPositionTop: existing?.labelPositionTop ?? false
        )
        store.currentPage.tiles[slotId.value] = updated
        store.save()
    }
}
