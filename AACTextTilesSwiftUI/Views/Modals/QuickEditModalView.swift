import SwiftUI
import UIKit

/// One sheet, one enum. The photo picker and the symbol library are both
/// sheets, and stacking `.sheet` modifiers on one view is what made the picture
/// buttons here look dead once before.
private enum EditSheet: Identifiable {
    case photo(ImagePickerRequest)
    case symbols

    var id: String {
        switch self {
        case .photo(let r): return "photo-\(r.id)"
        case .symbols:      return "symbols"
        }
    }
}

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
    /// Sound It Out was taken off the editor on 2026-09-13. The field is kept
    /// only so saving still writes the model; it is always false now, so a
    /// button saved from here speaks normally even if it used to sound out.
    private let isSoundItOut = false
    @State private var selectedLabelHex: String = "#1E293B"
    @State private var photoData: Data? = nil
    @State private var activeSheet: EditSheet? = nil
    @State private var audioData: Data? = nil
    @State private var labelPositionTop: Bool = false
    @State private var favoriteName: String = ""
    @State private var savedConfirmation: String? = nil
    @StateObject private var recorder = AudioRecorder()
    @ObservedObject private var favorites = TileFavorites.shared

    public var body: some View {
        NavigationView {
            Form {
                // Section 1: Button Label & Speech
                Section(header: Text("Words"),
                        footer: Text("Leave the second one empty to have the voice say exactly what is written on the button.")) {
                    LabeledField(label: "On the button", placeholder: "e.g. Apple, Help, Water", text: $label)
                    LabeledField(label: "Voice says", placeholder: "Same as the button", text: $tts)
                }

                // Section 1b: Hear it before saving. Previously there was no way
                // to check what a button would say without leaving the editor,
                // and a recorded voice could not be listened back to at all.
                Section(header: Text("Voice")) {
                    playbackButton(title: "Play Preview", enabled: !spokenText.isEmpty) {
                        SpeechManager.shared.speak(spokenText)
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

                    // Was one "Add Picture" button raising a confirmation
                    // dialog. See PictureSourceRows: in portrait that dialog
                    // landed off the bottom of the screen.
                    PictureSourceRows(data: $photoData) { request in
                        // Choosing a picture and choosing a symbol are the same
                        // slot on the button, so one replaces the other.
                        activeSheet = .photo(request)
                    }
                }

                // Section 3: Symbol Selection
                Section(header: Text("Symbol & Icon"),
                        footer: Text(SymbolLibrary.isAvailable
                                     ? "\(SymbolLibrary.names.count) picture symbols are built in. The emoji below cover core words the symbol set does not have - yes, no, please, stop."
                                     : "No symbol library is bundled with this build.")) {

                    if let chosen = selectedSymbol, let img = SymbolLibrary.image(named: chosen) {
                        HStack(spacing: 12) {
                            Image(uiImage: img)
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 56, height: 56)
                            Text(SymbolLibrary.readable(chosen))
                                .font(.system(size: 15, weight: .semibold))
                            Spacer()
                            Button("Remove") { selectedSymbol = nil }
                                .foregroundColor(Color(hex: "#B91C1C"))
                        }
                        .padding(.vertical, 2)
                    }

                    Button {
                        activeSheet = .symbols
                    } label: {
                        Label(selectedSymbol == nil ? "Choose a Symbol" : "Choose a Different Symbol",
                              systemImage: "magnifyingglass")
                    }

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

                // Section 4: Colour
                //
                // This was ten unnamed circles on one scrolling line, with no
                // way to reach a colour that was not among them and no control
                // at all for the border or the text - while a page background,
                // one screen away, offered named swatches AND a custom picker.
                // Same rows, same twenty swatches, same custom picker as every
                // other colour in the app now.
                Section(
                    header: Text("Button Colours"),
                    footer: Text("The starter boards colour buttons by kind - yellow for people, green for actions, orange for things. Matching a new button to the ones beside it keeps that meaning intact.")
                ) {
                    ColorRow(title: "Button Colour", hex: $selectedBgHex)
                    ColorRow(title: "Border Colour", hex: $selectedBorderHex)
                    ColorRow(title: "Text Colour", hex: $selectedLabelHex)
                }

                // Section 5: Word Size
                Section(header: Text("Word Size (\(String(format: "%.1fx", labelSize)))")) {
                    Slider(value: $labelSize, in: 0.8...2.0, step: 0.1)
                    Toggle("Word above the picture", isOn: $labelPositionTop)
                }

                favoritesSection
            }
            // The slot number was the only thing this title ever said, and it
            // is an internal detail - nobody thinks of a button as "button 7".
            // The word on the button is what tells you which one you opened.
            .navigationBarTitle(screenTitle, displayMode: .inline)
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
        // On iPad a bare NavigationView defaults to the split-view style, so
        // inside a sheet it renders as a sidebar next to an empty detail pane
        // instead of one plain form. Every modal in this app is a single
        // column and must say so.
        .navigationViewStyle(StackNavigationViewStyle())
        .sheet(item: $activeSheet) { sheet in
            switch sheet {
            case .photo(let request):
                ImagePicker(sourceType: request.source) { img in
                    // Tiles render small; 1024 keeps board.json from ballooning
                    // while still looking sharp on the iPad's display.
                    let sized = img.downscaled(maxDimension: 1024)
                    if let data = sized.jpegData(compressionQuality: 0.85) {
                        photoData = data
                        selectedSymbol = nil
                    }
                }
            case .symbols:
                SymbolPickerView { name in
                    selectedSymbol = name
                    photoData = nil
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
                selectedLabelHex = tile.labelHex
                labelPositionTop = tile.labelPositionTop
                labelSize = tile.labelSize
                photoData = tile.photoData
                audioData = tile.audioData
            }
        }
    }

    // MARK: - Saved buttons

    /// The expensive part of a button is the photograph of the actual dog and
    /// the recording of the actual parent's voice. Both used to belong to one
    /// slot on one page, so putting "Mom" on a second page meant taking the
    /// photo again and recording the voice again.
    @ViewBuilder
    private var favoritesSection: some View {
        Section(
            header: Text("Saved Buttons"),
            footer: Text(savedConfirmation
                         ?? "Saving keeps the picture, the recording, the words and the colours together, so this button can be put on any page without building it again.")
        ) {
            NavigationLink(destination: savedButtonPicker) {
                HStack {
                    Image(systemName: "star.fill")
                        .foregroundColor(Color(hex: "#F59E0B"))
                        .frame(width: 26)
                    Text("Use a Saved Button")
                        .font(.system(size: 16, weight: .semibold))
                    Spacer()
                    Text("\(favorites.items.count)")
                        .font(.system(size: 14))
                        .foregroundColor(Color(hex: "#64748B"))
                }
            }

            HStack(spacing: 10) {
                Image(systemName: "square.and.arrow.down.fill")
                    .foregroundColor(Color(hex: "#008369"))
                    .frame(width: 26)
                TextField(label.isEmpty ? "Name to save it under" : label, text: $favoriteName)
            }

            Button(action: saveToFavorites) {
                HStack(spacing: 10) {
                    Image(systemName: "star")
                        .frame(width: 26)
                    Text(favorites.contains(name: favoriteNameToUse) ? "Update Saved Button" : "Save This Button")
                        .font(.system(size: 16, weight: .semibold))
                    Spacer()
                }
                .contentShape(Rectangle())
                .foregroundColor(canSaveFavorite ? Color(hex: "#008369") : Color(hex: "#94A3B8"))
            }
            .buttonStyle(PlainButtonStyle())
            .disabled(!canSaveFavorite)
        }
    }

    private var screenTitle: String {
        let word = label.trimmingCharacters(in: .whitespacesAndNewlines)
        return word.isEmpty ? "Quick Edit" : word
    }

    private var favoriteNameToUse: String {
        let typed = favoriteName.trimmingCharacters(in: .whitespacesAndNewlines)
        return typed.isEmpty ? label.trimmingCharacters(in: .whitespacesAndNewlines) : typed
    }

    /// A button with no name, no picture and no recording is nothing worth
    /// keeping, and would fill the library with blanks called "Button".
    private var canSaveFavorite: Bool {
        !favoriteNameToUse.isEmpty && (photoData != nil || audioData != nil || selectedSymbol != nil || !label.isEmpty)
    }

    private func saveToFavorites() {
        guard canSaveFavorite else { return }
        let saved = favorites.add(SavedTile(
            name: favoriteNameToUse,
            label: label,
            tts: tts.isEmpty ? label : tts,
            symbolName: selectedSymbol,
            photoData: photoData,
            audioData: audioData,
            bgHex: selectedBgHex,
            borderHex: selectedBorderHex,
            labelHex: selectedLabelHex,
            labelSize: labelSize,
            isSoundItOut: isSoundItOut,
            labelPositionTop: labelPositionTop
        ))
        favoriteName = ""
        savedConfirmation = "Saved as \"\(saved.name)\". It is in Saved Buttons on every page now."
    }

    /// Loads a saved button into the form rather than writing it straight to
    /// the page: the parent still sees what they are about to get, and Cancel
    /// still cancels.
    private var savedButtonPicker: some View {
        SavedButtonPickerView(favorites: favorites) { saved in
            label = saved.label
            tts = saved.tts
            selectedSymbol = saved.symbolName
            photoData = saved.photoData
            audioData = saved.audioData
            selectedBgHex = saved.bgHex
            selectedBorderHex = saved.borderHex
            selectedLabelHex = saved.labelHex
            labelSize = saved.labelSize
            labelPositionTop = saved.labelPositionTop
            savedConfirmation = "Loaded \"\(saved.name)\". Press Save to put it on this button."
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

    /// Everything on the button is loaded into this screen's state in
    /// `onAppear` and written back here. Building a fresh TileModel from only
    /// the fields the form showed used to drop photoData, audioData, labelHex
    /// and labelPositionTop back to their defaults - so putting a photo or a
    /// recorded voice on a tile and then changing its label destroyed both. The
    /// two colours the form did not offer were being carried through by hand;
    /// they are edited here now, so there is nothing left to carry.
    private func saveTile() {
        let updated = TileModel(
            id: slotId.value,
            label: label,
            tts: tts.isEmpty ? label : tts,
            symbolName: selectedSymbol,
            photoData: photoData,
            bgHex: selectedBgHex,
            borderHex: selectedBorderHex,
            labelHex: selectedLabelHex,
            labelSize: labelSize,
            audioData: audioData,
            isSoundItOut: isSoundItOut,
            labelPositionTop: labelPositionTop
        )
        store.currentPage.tiles[slotId.value] = updated
        store.save()
    }
}

/// The saved-buttons library. A plain list rather than a grid on purpose: what
/// tells two saved buttons apart is the photograph and whether it has a
/// recording, and both of those need a row's worth of width to be legible.
public struct SavedButtonPickerView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject public var favorites: TileFavorites
    public let onPick: (SavedTile) -> Void

    @State private var query: String = ""

    public init(favorites: TileFavorites, onPick: @escaping (SavedTile) -> Void) {
        self.favorites = favorites
        self.onPick = onPick
    }

    private var results: [SavedTile] { favorites.search(query) }

    public var body: some View {
        List {
            if favorites.items.isEmpty {
                emptyState
            } else {
                Section {
                    HStack(spacing: 8) {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(Color(hex: "#94A3B8"))
                        TextField("Search saved buttons", text: $query)
                    }
                }

                Section(footer: Text("Swipe a saved button to the left to delete it. Deleting it here does not touch any page it is already on.")) {
                    ForEach(results) { saved in
                        Button(action: {
                            onPick(saved)
                            presentationMode.wrappedValue.dismiss()
                        }) {
                            row(saved)
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                    .onDelete(perform: delete)
                }
            }
        }
        .navigationBarTitle("Saved Buttons", displayMode: .inline)
    }

    @ViewBuilder
    private var emptyState: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Nothing saved yet")
                .font(.system(size: 17, weight: .bold))
            Text("Build a button the way you want it - its picture, its recording, its colours - then use Save This Button. It will be here on every page after that.")
                .font(.system(size: 14))
                .foregroundColor(Color(hex: "#64748B"))
        }
        .padding(.vertical, 10)
    }

    /// `onDelete` hands back offsets into the rows on screen, which are the
    /// SEARCH RESULTS, not the library. Passing those straight to the store
    /// would delete whatever happened to sit at that position in the full list -
    /// the compacted-index trap this codebase has hit before. Map back by id.
    private func delete(at offsets: IndexSet) {
        let shown = results
        for index in offsets where shown.indices.contains(index) {
            favorites.remove(id: shown[index].id)
        }
    }

    @ViewBuilder
    private func row(_ saved: SavedTile) -> some View {
        HStack(spacing: 12) {
            thumbnail(saved)
            VStack(alignment: .leading, spacing: 3) {
                Text(saved.name)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(Color(hex: "#1E293B"))
                Text(saved.tts.isEmpty ? saved.label : saved.tts)
                    .font(.system(size: 13))
                    .foregroundColor(Color(hex: "#64748B"))
                    .lineLimit(1)
                HStack(spacing: 10) {
                    if saved.hasPhoto { tag("Photo", "photo.fill", "#0284C7") }
                    if saved.hasRecording { tag("Recording", "waveform", "#7C3AED") }
                    if saved.symbolName != nil && !saved.hasPhoto { tag("Symbol", "square.grid.2x2.fill", "#008369") }
                }
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(Color(hex: "#CBD5E1"))
        }
        .padding(.vertical, 4)
        .contentShape(Rectangle())
    }

    @ViewBuilder
    private func tag(_ text: String, _ icon: String, _ tint: String) -> some View {
        HStack(spacing: 3) {
            Image(systemName: icon).font(.system(size: 9, weight: .bold))
            Text(text).font(.system(size: 10, weight: .bold))
        }
        .foregroundColor(Color(hex: tint))
    }

    /// Shows the saved button roughly as it will land on the page - its own
    /// colour and border, not a generic grey square, because colour is one of
    /// the things being reused.
    @ViewBuilder
    private func thumbnail(_ saved: SavedTile) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: 10)
                .fill(Color(hex: saved.bgHex))
                .frame(width: 56, height: 56)
            RoundedRectangle(cornerRadius: 10)
                .stroke(Color(hex: saved.borderHex), lineWidth: 2)
                .frame(width: 56, height: 56)

            if let data = saved.photoData, let ui = UIImage(data: data) {
                Image(uiImage: ui)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 52, height: 52)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            } else if let name = saved.symbolName, let img = SymbolLibrary.image(named: name) {
                Image(uiImage: img)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 40, height: 40)
            } else if let name = saved.symbolName {
                // The emoji chips are stored under the same field as a symbol
                // name, and SymbolLibrary has no picture for them.
                Text(name).font(.system(size: 28))
            } else {
                Text(saved.label.prefix(2))
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(Color(hex: saved.labelHex))
            }
        }
    }
}

/// A text field with a caption that stays put.
///
/// A bare `TextField` only says what it is for while it is empty - the moment
/// a button has words in it, the editor showed two unlabelled boxes of text
/// and nobody could tell which was the word on the tile and which the voice
/// would speak. The caption sits to the left and never goes away.
public struct LabeledField: View {
    public let label: String
    public let placeholder: String
    @Binding public var text: String

    public init(label: String, placeholder: String, text: Binding<String>) {
        self.label = label
        self.placeholder = placeholder
        self._text = text
    }

    public var body: some View {
        HStack(spacing: 12) {
            Text(label)
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(Color(hex: "#64748B"))
                .frame(width: 118, alignment: .leading)
            TextField(placeholder, text: $text)
                .multilineTextAlignment(.leading)
        }
    }
}
