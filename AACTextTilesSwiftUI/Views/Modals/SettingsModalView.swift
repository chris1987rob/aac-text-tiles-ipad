import SwiftUI
import AVFoundation

/// One sheet, one enum. Settings can present a share sheet, a file picker or a
/// PIN pad, and stacking `.sheet` modifiers is what makes a control silently
/// refuse to open.
private enum SettingsSheet: Identifiable {
    case share(URL)
    case restorePicker
    case pin

    var id: String {
        switch self {
        case .share(let u):    return "share-\(u.lastPathComponent)"
        case .restorePicker:   return "restore"
        case .pin:             return "pin"
        }
    }
}

public struct SettingsModalView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject public var store: AACStore

    @State private var confirmReset = false
    @State private var activeSheet: SettingsSheet? = nil

    @State private var pendingRestore: (BookBackup.Archive, BookBackup.Summary)? = nil
    @State private var restoreError: String? = nil
    @State private var backupNote: String? = nil

    public init(store: AACStore) { self.store = store }

    private var voices: [AVSpeechSynthesisVoice] { SpeechManager.availableVoices() }

    public var body: some View {
        NavigationView {
            Form {
                voiceSection
                picturesSection
                accessSection
                lockSection
                backupSection
                bookSection
                dangerSection
                aboutSection
            }
            .navigationBarTitle("Settings", displayMode: .inline)
            .navigationBarItems(trailing: Button("Done") {
                presentationMode.wrappedValue.dismiss()
            })
        }
        .navigationViewStyle(StackNavigationViewStyle())
        .sheet(item: $activeSheet) { sheet in
            switch sheet {
            case .share(let url):
                ActivityView(items: [url])
            case .restorePicker:
                DocumentPickerView { url in loadBackup(url) }
            case .pin:
                PinChangeView(pin: Binding(
                    get: { store.settings.lockPIN },
                    set: { store.settings.lockPIN = $0 }
                ))
            }
        }
        .alert(isPresented: $confirmReset) {
            Alert(title: Text("Reset everything?"),
                  message: Text("Every page, button and talking spot you have made is erased and the starter book comes back. Back up first if you have not."),
                  primaryButton: .destructive(Text("Reset")) {
                      store.resetToDefaults()
                  },
                  secondaryButton: .cancel())
        }
    }

    // MARK: - Voice

    @ViewBuilder
    private var voiceSection: some View {
        Section(header: Text("Voice"),
                footer: Text(voiceFooter)) {

            Picker("Voice", selection: Binding(
                get: { store.settings.voiceId ?? "" },
                set: { store.settings.voiceId = $0.isEmpty ? nil : $0 }
            )) {
                ForEach(VoiceClips.available, id: \.id) { voice in
                    Text("\(voice.name) – Talk Tiles voice").tag(voice.id)
                }
                Text("iPad default").tag("")
                ForEach(voices, id: \.identifier) { voice in
                    Text("\(voice.name) (\(voice.language))").tag(voice.identifier)
                }
            }

            if let voice = VoiceClips.voice(for: store.settings.voiceId) {
                Text(voice.description.isEmpty
                     ? "A recorded voice. Every built-in picture and phrase has a clip; anything she has no clip for, the iPad voice below reads out."
                     : "\(voice.description). Every built-in picture and phrase has a clip; anything she has no clip for, the iPad voice reads out.")
                    .font(.footnote)
                    .foregroundColor(.secondary)
            }

            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text("Speaking speed")
                    Spacer()
                    Text(speedLabel).foregroundColor(.secondary)
                }
                Slider(value: Binding(
                    get: { store.settings.speechRate },
                    set: { store.settings.speechRate = $0 }
                ), in: 0.3...0.7, step: 0.05)
            }

            Button("Test the voice") {
                let clips = VoiceClips.previewClips(voiceId: store.settings.voiceId)
                if !clips.isEmpty {
                    SpeechManager.shared.playClipSequence(clips, rate: Float(store.settings.speechRate))
                } else {
                    SpeechManager.shared.speak("Hello. This is how I will sound.",
                                               rate: Float(store.settings.speechRate),
                                               voiceId: store.settings.voiceId)
                }
            }
        }
    }

    private var voiceFooter: String {
        if VoiceClips.isAvailable {
            return "Bella is the app's own recorded voice and speaks every Talk Tiles picture. The iPad voices read anything typed or edited. More of those can be added under iPad Settings › Accessibility › Spoken Content › Voices."
        }
        return "Pick a voice that fits the person speaking. More voices can be added under iPad Settings › Accessibility › Spoken Content › Voices."
    }

    // MARK: - Pictures

    /// Two picture sets ship in the app; this is where the picker is told
    /// which one to open on. Neither is ever removed from a board - a button
    /// keeps whichever picture it was given.
    @ViewBuilder
    private var picturesSection: some View {
        Section(header: Text("Pictures"),
                footer: Text("The set the symbol picker opens on when you choose a picture for a button. You can still switch sets inside the picker, and buttons already made keep their pictures.")) {

            Picker("Symbol set", selection: Binding(
                get: { store.settings.symbolSet },
                set: { store.settings.symbolSet = $0 }
            )) {
                ForEach(SymbolSet.allCases.filter { SymbolLibrary.has($0) }) { option in
                    Text(option.title).tag(option)
                }
            }
            .pickerStyle(SegmentedPickerStyle())

            ForEach(SymbolSet.allCases.filter { SymbolLibrary.has($0) }) { option in
                HStack(alignment: .top, spacing: 12) {
                    symbolSample(option)
                    VStack(alignment: .leading, spacing: 3) {
                        HStack(spacing: 6) {
                            Text(option.title)
                                .font(.system(size: 15, weight: .semibold))
                            if option == store.settings.symbolSet {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(Color(hex: "#008369"))
                            }
                        }
                        Text("\(SymbolLibrary.count(of: option)) pictures. \(option.blurb)")
                            .font(.footnote)
                            .foregroundColor(.secondary)
                    }
                    Spacer()
                }
                .padding(.vertical, 4)
                .contentShape(Rectangle())
                .onTapGesture { store.settings.symbolSet = option }
            }
        }
    }

    /// Three pictures from a set, so the choice is visible, not just named.
    private func sampleNames(_ set: SymbolSet) -> [String] {
        switch set {
        case .talkTiles: return ["tt:cat", "tt:dog", "tt:apple"]
        case .mulberry:  return ["cat", "dog", "apple"]
        }
    }

    private func symbolSample(_ set: SymbolSet) -> some View {
        HStack(spacing: 4) {
            ForEach(sampleNames(set), id: \.self) { name in
                if let img = SymbolLibrary.image(named: name) {
                    Image(uiImage: img)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 34, height: 34)
                }
            }
        }
        .frame(width: 114, alignment: .leading)
    }

    private var speedLabel: String {
        switch store.settings.speechRate {
        case ..<0.38: return "Slow"
        case ..<0.52: return "Normal"
        default:      return "Fast"
        }
    }

    // MARK: - Touch access

    @ViewBuilder
    private var accessSection: some View {
        Section(header: Text("Touch"),
                footer: Text("For a child whose aim is unsteady. Hold-to-speak ignores a hand resting on the screen; the repeat pause stops a tremor turning one press into five.")) {

            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text("Hold to speak")
                    Spacer()
                    Text(store.settings.activationDelay == 0
                         ? "Off" : String(format: "%.1fs", store.settings.activationDelay))
                        .foregroundColor(.secondary)
                }
                Slider(value: Binding(
                    get: { store.settings.activationDelay },
                    set: { store.settings.activationDelay = $0 }
                ), in: 0...2, step: 0.1)
            }

            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text("Pause before repeat")
                    Spacer()
                    Text(store.settings.repeatLockout == 0
                         ? "Off" : String(format: "%.1fs", store.settings.repeatLockout))
                        .foregroundColor(.secondary)
                }
                Slider(value: Binding(
                    get: { store.settings.repeatLockout },
                    set: { store.settings.repeatLockout = $0 }
                ), in: 0...3, step: 0.1)
            }

            Toggle("Speak when the finger lifts", isOn: Binding(
                get: { store.settings.activateOnRelease },
                set: { store.settings.activateOnRelease = $0 }
            ))
        }
    }

    // MARK: - Child lock

    @ViewBuilder
    private var lockSection: some View {
        Section(header: Text("Child Lock"),
                footer: Text("Locking hides every way into the editor, so a board cannot be rearranged by accident. To stop a child leaving Talk Tiles altogether, turn on iPad Guided Access: Settings › Accessibility › Guided Access, then triple-click the top button with Talk Tiles open.")) {

            Toggle("Lock editing", isOn: Binding(
                get: { store.isLocked },
                set: { store.isLocked = $0 }
            ))

            if store.isLocked {
                Button("Change PIN") { activeSheet = .pin }
            }
        }
    }

    // MARK: - Backup

    @ViewBuilder
    private var backupSection: some View {
        Section(header: Text("Backup"),
                footer: Text("A backup holds every page, button, picture and recording in one file. Email it to yourself or keep it in Files. Do this before changing iPads - there is no other way to get a book back.")) {

            Button {
                makeBackup()
            } label: {
                Label("Back Up Everything", systemImage: "square.and.arrow.up")
            }

            Button {
                restoreError = nil
                activeSheet = .restorePicker
            } label: {
                Label("Restore From a Backup", systemImage: "square.and.arrow.down")
            }

            if let note = backupNote {
                Text(note).font(.footnote).foregroundColor(Color(hex: "#2C7A57"))
            }
            if let err = restoreError {
                Text(err).font(.footnote).foregroundColor(Color(hex: "#B91C1C"))
            }
            if let pending = pendingRestore {
                restoreConfirmation(pending.1)
            }
        }
    }

    /// Restoring replaces the book, so the file is described before it is
    /// applied rather than after.
    @ViewBuilder
    private func restoreConfirmation(_ summary: BookBackup.Summary) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Backup found")
                .font(.system(size: 15, weight: .bold))
            Text("\(summary.pages) pages · \(summary.buttons) buttons · \(summary.hotspots) talking spots")
                .font(.footnote)
                .foregroundColor(.secondary)
            Text("Saved \(summary.createdAt.formatted(date: .abbreviated, time: .shortened))")
                .font(.footnote)
                .foregroundColor(.secondary)
            Text("Restoring replaces the book that is on this iPad now.")
                .font(.footnote)
                .foregroundColor(Color(hex: "#B91C1C"))

            HStack(spacing: 12) {
                Button("Restore") { applyRestore() }
                    .font(.headline)
                    .foregroundColor(Color(hex: "#008369"))
                Button("Cancel") { pendingRestore = nil }
                    .foregroundColor(.secondary)
            }
            .padding(.top, 2)
        }
        .padding(.vertical, 4)
    }

    // MARK: - Book / danger / about

    @ViewBuilder
    private var bookSection: some View {
        Section(header: Text("This Book")) {
            HStack { Text("Pages"); Spacer(); Text("\(store.pages.count)").foregroundColor(.secondary) }
            HStack {
                Text("Buttons filled in")
                Spacer()
                Text("\(store.pages.reduce(0) { $0 + $1.tiles.count })").foregroundColor(.secondary)
            }
            HStack {
                Text("Talking spots")
                Spacer()
                Text("\(store.pages.reduce(0) { $0 + $1.hotspots.count })").foregroundColor(.secondary)
            }
        }
    }

    @ViewBuilder
    private var dangerSection: some View {
        Section(header: Text("Danger zone"),
                footer: Text("This erases every page, button and talking spot you have made and puts the original starter book back. It cannot be undone.")) {
            Button("Reset to the starter book") { confirmReset = true }
                .foregroundColor(Color(hex: "#B91C1C"))
        }
    }

    @ViewBuilder
    private var aboutSection: some View {
        Section(header: Text("About")) {
            if SymbolLibrary.has(.talkTiles) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("\(SymbolLibrary.count(of: .talkTiles)) Talk Tiles pictures")
                        .font(.system(size: 15, weight: .semibold))
                    Text("Drawn in-house for Talk Tiles. No third-party licence.")
                        .font(.footnote)
                        .foregroundColor(.secondary)
                }
                .padding(.vertical, 2)
            }
            if SymbolLibrary.has(.mulberry) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("\(SymbolLibrary.count(of: .mulberry)) Mulberry Symbols")
                        .font(.system(size: 15, weight: .semibold))
                    Text(SymbolLibrary.attribution)
                        .font(.footnote)
                        .foregroundColor(.secondary)
                }
                .padding(.vertical, 2)
            }
            ForEach(VoiceClips.available, id: \.id) { voice in
                VStack(alignment: .leading, spacing: 3) {
                    Text("\(voice.name) voice")
                        .font(.system(size: 15, weight: .semibold))
                    Text("Recorded for Talk Tiles: one clip for every picture and built-in phrase.")
                        .font(.footnote)
                        .foregroundColor(.secondary)
                }
                .padding(.vertical, 2)
            }
        }
    }

    // MARK: - Actions

    private func makeBackup() {
        restoreError = nil
        do {
            store.saveNow()
            let url = try BookBackup.write(pages: store.pages, settings: store.settings)
            backupNote = nil
            activeSheet = .share(url)
        } catch {
            restoreError = "Could not make a backup: \(error.localizedDescription)"
        }
    }

    private func loadBackup(_ url: URL) {
        do {
            let result = try BookBackup.read(from: url)
            restoreError = nil
            // Deferred so the file picker is gone before the confirmation
            // appears in its place.
            DispatchQueue.main.async { pendingRestore = result }
        } catch {
            DispatchQueue.main.async {
                restoreError = "Could not read that file. \(error.localizedDescription)"
            }
        }
    }

    private func applyRestore() {
        guard let (archive, _) = pendingRestore else { return }
        store.pages = archive.pages
        store.currentPageIndex = 0
        if let restored = archive.settings { store.settings = restored }
        store.saveNow()
        store.saveSettings()
        pendingRestore = nil
        backupNote = "Restored \(archive.pages.count) pages."
    }
}

/// Four digits, entered twice, so a mistyped PIN does not lock a parent out of
/// their own board.
public struct PinChangeView: View {
    @Environment(\.presentationMode) private var presentationMode
    @Binding public var pin: String

    @State private var first: String = ""
    @State private var second: String = ""
    @State private var message: String? = nil

    public var body: some View {
        NavigationView {
            Form {
                Section(footer: Text("Four digits. This stops a child getting into the editor; it is not a password and is stored as plain text.")) {
                    SecureField("New PIN", text: $first)
                        .keyboardType(.numberPad)
                    SecureField("Enter it again", text: $second)
                        .keyboardType(.numberPad)
                }
                if let message = message {
                    Text(message).foregroundColor(Color(hex: "#B91C1C")).font(.footnote)
                }
            }
            .navigationBarTitle("Change PIN", displayMode: .inline)
            .navigationBarItems(
                leading: Button("Cancel") { presentationMode.wrappedValue.dismiss() },
                trailing: Button("Save") { save() }.font(.headline)
            )
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }

    private func save() {
        let digits = first.filter(\.isNumber)
        guard digits.count == 4, digits == first else {
            message = "The PIN needs to be exactly four digits."
            return
        }
        guard first == second else {
            message = "Those two do not match."
            return
        }
        pin = digits
        presentationMode.wrappedValue.dismiss()
    }
}
