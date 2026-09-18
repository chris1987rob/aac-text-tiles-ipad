import SwiftUI
import UIKit

/// A keyboard made of pictures instead of letters. Pressing a symbol puts its
/// word in the sentence bar and speaks it, so a child who cannot spell can
/// still build "I want the cookie" one picture at a time.
///
/// This replaced a QWERTY spelling keyboard on 2026-08-26. Spelling asks a
/// child to already know the word letter by letter; this asks them to recognise
/// a picture, which is the whole premise of a symbol AAC system.
///
/// The keys FILL whatever space is left under the sentence bar rather than
/// scrolling at a fixed size. A scrolling list of small keys is the wrong shape
/// for this app twice over: a child who cannot reliably point also cannot
/// reliably scroll, and a word that is off-screen may as well not exist. So the
/// page asks how many keys should fit (Page Options ▸ Keyboard), lays exactly
/// that many out edge to edge, and puts the rest on numbered pages.
public struct KeyboardPageView: View {
    @ObservedObject public var store: AACStore

    /// Opens the editor for one key. Until 2026-09-06 this view took no such
    /// callback and never looked at `isEditMode`, so a keyboard page was the
    /// one page kind whose buttons could not be edited at all - tapping a key
    /// in the page editor just spoke it, exactly as it does for a child.
    public let onSelectKey: (String) -> Void

    @State private var sentence: [SymbolWord] = []
    @State private var groupId: String = ""
    @State private var justPressedId: String? = nil
    @State private var wordPage: Int = 0

    public init(store: AACStore, onSelectKey: @escaping (String) -> Void = { _ in }) {
        self.store = store
        self.onSelectKey = onSelectKey
    }

    public var body: some View {
        VStack(spacing: 10) {
            sentenceBar
            groupRow
            symbolGrid
        }
        .padding(.vertical, 10)
        .onAppear(perform: syncGroupSelection)
        // A page can be narrowed to different groups from Page Options while
        // it is open, and moving to another keyboard page reuses this view.
        .onChange(of: store.currentPageIndex) { _ in
            wordPage = 0
            syncGroupSelection()
        }
        .onChange(of: visibleGroups.map(\.id)) { _ in syncGroupSelection() }
    }

    // MARK: - Sentence bar

    private var sentenceBar: some View {
        SentencePill(
            words: sentence.map(\.label),
            placeholder: "Press a picture to start a sentence",
            onClear: { sentence.removeAll() },
            onTap: speakSentence
        ) {
            HStack(spacing: 10) {
                RoundBarButton(icon: "delete.left.fill", fill: BoardTheme.chip, tint: BoardTheme.inkSoft,
                               size: 36, label: "Remove last word") {
                    if !sentence.isEmpty { sentence.removeLast() }
                }
                SpeakNowButton(action: speakSentence)
            }
        }
        .padding(.horizontal, 14)
    }

    private func speakSentence() {
        let text = sentence.map(\.tts).joined(separator: " ")
        guard !text.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        SpeechManager.shared.speak(text, rate: Float(store.settings.speechRate), voiceId: store.settings.voiceId)
    }

    // MARK: - Groups and paging

    private var visibleGroups: [SymbolWordBank.Group] {
        SymbolWordBank.groups(limitedTo: store.currentPage.keyboardGroups)
    }

    /// The words this page shows: the group's vocabulary with this page's own
    /// changes laid over it, and anything the parent has taken off the page
    /// dropped. In edit mode hidden words stay visible, greyed - a word you
    /// have removed has to be reachable to put back.
    private var currentWords: [SymbolWord] {
        let base = visibleGroups.first { $0.id == groupId }?.words ?? visibleGroups.first?.words ?? []
        let edits = store.currentPage.keyboardEdits
        return base.compactMap { word in
            let edit = edits?[word.id]
            if edit?.hidden == true && !store.isEditMode { return nil }
            return word.applying(edit)
        }
    }

    private func isHidden(_ word: SymbolWord) -> Bool {
        store.currentPage.keyboardEdits?[word.id]?.hidden == true
    }

    private var keysPerScreen: Int {
        max(2, store.currentPage.keyboardKeys ?? SymbolWordBank.defaultKeyCount)
    }

    private var pageCount: Int {
        max(1, Int(ceil(Double(currentWords.count) / Double(keysPerScreen))))
    }

    /// The keys-per-screen setting is a ceiling, not a quota. Spreading the
    /// words evenly over the pages they need is what actually fills the screen:
    /// People has 15 words, so a rigid 20 left a dead band across the bottom
    /// third of the board, and 95 Things over 5 pages of 20 ended on a page
    /// with 15. Evened out, People lays out 15 and Things gives 19 five times -
    /// every page full, and every page the same size as the last.
    private var wordsPerPage: Int {
        max(1, Int(ceil(Double(currentWords.count) / Double(pageCount))))
    }

    /// The words on the page being shown. Clamped rather than trusted: the
    /// group can change under it, and an out-of-range slice is a crash.
    private var wordsOnScreen: [SymbolWord] {
        let page = min(max(0, wordPage), pageCount - 1)
        let start = page * wordsPerPage
        guard start < currentWords.count else { return [] }
        let end = min(start + wordsPerPage, currentWords.count)
        return Array(currentWords[start..<end])
    }

    private func syncGroupSelection() {
        let ids = visibleGroups.map(\.id)
        // The tabs cannot be tapped from a screenshot session, so the group to
        // open on can be named at launch - the same reason `-openScreen` exists:
        //   -openScreen keyboard -keyboardGroup Things
        // Only "People" was ever being looked at, and paging only happens in the
        // big groups.
        if let wanted = UserDefaults.standard.string(forKey: "keyboardGroup"),
           ids.contains(wanted), groupId != wanted {
            groupId = wanted
            wordPage = 0
        }
        if !ids.contains(groupId) {
            groupId = ids.first ?? ""
            wordPage = 0
        }
        if wordPage >= pageCount { wordPage = 0 }
    }

    /// The tabs and the page arrows share one row. Giving the arrows a row of
    /// their own would cost the keys ~40 points of height on every page,
    /// including the many that only have one page of words.
    private var groupRow: some View {
        HStack(spacing: 10) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(visibleGroups) { group in
                        let selected = group.id == groupId
                        Button(action: {
                            groupId = group.id
                            wordPage = 0
                        }) {
                            // Pastel tabs: the chosen one is solid with a dark
                            // underline, the rest are washed out.
                            Text(group.title.uppercased())
                                .font(.system(size: 15, weight: .bold))
                                .tracking(0.5)
                                .foregroundColor(selected ? BoardTheme.ink : BoardTheme.inkSoft)
                                .padding(.horizontal, 18)
                                .padding(.vertical, 10)
                                .background(Color(hex: group.color).opacity(selected ? 1 : 0.55))
                                .cornerRadius(14)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 14)
                                        .stroke(selected ? BoardTheme.ink.opacity(0.35) : Color.clear, lineWidth: 2)
                                )
                        }
                    }
                }
                .padding(.horizontal, 14)
            }

            if pageCount > 1 {
                HStack(spacing: 6) {
                    pageArrow("chevron.left", enabled: wordPage > 0) { wordPage -= 1 }
                    Text("\(min(wordPage, pageCount - 1) + 1)/\(pageCount)")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(Color(hex: "#475569"))
                        .frame(minWidth: 34)
                    pageArrow("chevron.right", enabled: wordPage < pageCount - 1) { wordPage += 1 }
                }
                .padding(.trailing, 14)
            }
        }
    }

    private func pageArrow(_ icon: String, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: { if enabled { action() } }) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(enabled ? Color(hex: "#1E293B") : Color(hex: "#CBD5E1"))
                .frame(width: 38, height: 38)
                .background(Color(hex: "#F1F5F9"))
                .cornerRadius(10)
        }
        .buttonStyle(PlainButtonStyle())
        .disabled(!enabled)
    }

    // MARK: - Symbols

    /// Sized from the space it is handed, the same way `TileGridView` sizes a
    /// board: work out the grid, divide the room up, and hand each key an
    /// exact width and height. Nothing here scrolls.
    private var symbolGrid: some View {
        GeometryReader { geo in
            // Sized from the words the pages actually carry, not from the
            // setting - that is the difference between keys that reach the
            // bottom of the screen and keys that stop short of it.
            let keys = wordsPerPage
            let (cols, rows) = gridDimensions(for: keys, in: geo.size)
            let spacing = spacingFor(keys: keys)
            let padding = paddingFor(keys: keys)

            let usableWidth = max(40, geo.size.width - padding * 2 - spacing * CGFloat(cols - 1))
            let usableHeight = max(40, geo.size.height - padding * 2 - spacing * CGFloat(rows - 1))
            let cellWidth = usableWidth / CGFloat(cols)
            let cellHeight = usableHeight / CGFloat(rows)

            let words = wordsOnScreen

            VStack(spacing: spacing) {
                ForEach(0..<rows, id: \.self) { row in
                    HStack(spacing: spacing) {
                        ForEach(0..<cols, id: \.self) { col in
                            let index = row * cols + col
                            if index < words.count {
                                symbolButton(words[index], width: cellWidth, height: cellHeight)
                            } else {
                                // Only the final page of an uneven split can
                                // land here, and it keeps the size the other
                                // pages use rather than restretching - a word
                                // must not move as you page onto it.
                                Color.clear.frame(width: cellWidth, height: cellHeight)
                            }
                        }
                    }
                }
            }
            .padding(padding)
            .frame(width: geo.size.width, height: geo.size.height, alignment: .center)
        }
    }

    /// Aims the grid at a key a little wider than it is tall - a picture with a
    /// word under it reads best in that shape, and a square-ish grid on a
    /// landscape iPad leaves fat gutters down both sides instead.
    private func gridDimensions(for keys: Int, in size: CGSize) -> (cols: Int, rows: Int) {
        let ratio = Double(size.width / max(size.height, 1))
        let target = max(1.0, ratio / 1.25)
        var cols = Int((Double(keys) * target).squareRoot().rounded())
        cols = min(max(cols, 1), keys)
        let rows = max(1, Int(ceil(Double(keys) / Double(cols))))
        // Recompute the columns from the rows so a layout like 7×5 for 30 keys
        // does not leave a whole dead column on the right.
        cols = max(1, Int(ceil(Double(keys) / Double(rows))))
        return (cols, rows)
    }

    private func spacingFor(keys: Int) -> CGFloat {
        if keys <= 6 { return 14 }
        if keys <= 12 { return 12 }
        if keys <= 20 { return 10 }
        return 8
    }

    private func paddingFor(keys: Int) -> CGFloat {
        if keys <= 12 { return 14 }
        return 10
    }

    private func symbolButton(_ word: SymbolWord, width: CGFloat, height: CGFloat) -> some View {
        // The picture takes the room; the label is a caption under it. Both are
        // derived from the key's own height so one setting drives everything.
        let labelSize = min(22, max(11, height * 0.15))
        let iconSize = max(20, height - labelSize - 18)
        let editing = store.isEditMode
        let hidden = editing && isHidden(word)

        return Button(action: {
            if editing { onSelectKey(word.id) } else { press(word) }
        }) {
            VStack(spacing: 4) {
                keyPicture(word, size: iconSize)
                Text(word.label)
                    .font(.system(size: labelSize, weight: .bold))
                    .foregroundColor(Color(hex: "#1E293B"))
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
            }
            .frame(width: width, height: height)
            .background(Color(hex: word.color))
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(justPressedId == word.id ? Color(hex: "#008369") : Color(hex: "#CBD5E1"),
                            lineWidth: justPressedId == word.id ? 4 : 1)
            )
            // In the page editor a key says it can be edited, the same way an
            // empty grid tile says "Tap to Add". Without this the editor looks
            // identical to the player and the keys read as dead.
            .overlay(alignment: .topTrailing) {
                if editing {
                    Image(systemName: hidden ? "eye.slash.fill" : "pencil.circle.fill")
                        .font(.system(size: min(22, max(13, height * 0.16))))
                        .foregroundColor(.white)
                        .padding(3)
                        .background(Circle().fill(Color(hex: hidden ? "#94A3B8" : "#008369")))
                        .padding(5)
                }
            }
            .opacity(hidden ? 0.4 : 1)
            .scaleEffect(justPressedId == word.id ? 0.95 : 1.0)
        }
        .buttonStyle(PlainButtonStyle())
    }

    /// A key shows a photo if the page put one there, then a bundled symbol if
    /// the icon names one, and otherwise the emoji the word bank carries.
    @ViewBuilder
    private func keyPicture(_ word: SymbolWord, size: CGFloat) -> some View {
        if let data = word.photoData, let ui = UIImage(data: data) {
            Image(uiImage: ui)
                .resizable()
                .scaledToFill()
                .frame(width: size, height: size)
                .clipShape(RoundedRectangle(cornerRadius: 8))
        } else if let img = SymbolLibrary.image(named: word.icon) {
            Image(uiImage: img)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: size, height: size)
        } else {
            Text(word.icon)
                .font(.system(size: size))
                .minimumScaleFactor(0.4)
                .lineLimit(1)
        }
    }

    /// Speaks the single word as it lands, rather than only on Speak. A child
    /// building a sentence needs to hear that the press did something; waiting
    /// until the end to find out is how a wrong word survives to the end.
    private func press(_ word: SymbolWord) {
        // The same tremor guard the board tiles use - one intended press must
        // not land five copies of a word in the sentence.
        guard TouchAccess.shared.shouldFire(
            key: "keyboard-\(word.id)",
            lockout: store.settings.repeatLockout
        ) else { return }

        sentence.append(word)
        // A voice recorded onto this key wins over the synthesiser, the same
        // way it does on a board tile.
        if let data = word.audioData {
            SpeechManager.shared.playAudioData(data)
        } else {
            SpeechManager.shared.speak(word.tts, rate: Float(store.settings.speechRate), voiceId: store.settings.voiceId)
        }

        withAnimation(.easeOut(duration: 0.12)) { justPressedId = word.id }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.22) {
            withAnimation(.easeOut(duration: 0.12)) {
                if justPressedId == word.id { justPressedId = nil }
            }
        }
    }
}

/// The keyboard's setup, shared by the New Page Wizard and Page Options so a
/// keyboard page can be changed after it is made. A page whose only chance to
/// be set up is the wizard is the dead end the scene picture used to be.
public struct KeyboardSetupSections: View {
    @Binding var keys: Int
    @Binding var groupIds: [String]
    let stepNumber: Int?

    public init(keys: Binding<Int>, groupIds: Binding<[String]>, stepNumber: Int? = nil) {
        self._keys = keys
        self._groupIds = groupIds
        self.stepNumber = stepNumber
    }

    private func header(_ step: Int, _ text: String) -> String {
        stepNumber == nil ? text : "\(stepNumber! + step - 1). \(text)"
    }

    public var body: some View {
        Section(
            header: Text(header(1, "Picture Grid Layout")),
            footer: Text("The same sizes a Standard Grid page offers. The pictures fill the whole screen under the sentence bar, so fewer pictures means bigger ones. Words that do not fit go on a second page you reach with the arrows.")
        ) {
            Picker("Grid Size", selection: $keys) {
                ForEach(SymbolWordBank.keyCountOptions, id: \.self) { count in
                    Text("\(count)").tag(count)
                }
            }
            .pickerStyle(SegmentedPickerStyle())

            HStack {
                Text(SymbolWordBank.keyCountLabel(keys))
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(Color(hex: "#008369"))
                Spacer()
                Text("\(keys) pictures at a time")
                    .font(.system(size: 13))
                    .foregroundColor(Color(hex: "#64748B"))
            }
        }
        // A page saved under the keyboard's old sizes holds a count this
        // picker no longer offers, which renders as nothing selected.
        .onAppear { keys = SymbolWordBank.nearestKeyCount(keys) }

        Section(
            header: Text(header(2, "Word Groups")),
            footer: Text("The colours are the Fitzgerald key used across AAC systems - yellow people, green actions, orange things. Turn groups off to start smaller; you can turn them back on any time.")
        ) {
            ForEach(SymbolWordBank.groups) { group in
                Toggle(isOn: binding(for: group.id)) {
                    HStack(spacing: 10) {
                        RoundedRectangle(cornerRadius: 5)
                            .fill(Color(hex: group.color))
                            .frame(width: 26, height: 26)
                            .overlay(
                                RoundedRectangle(cornerRadius: 5)
                                    .stroke(Color(hex: "#CBD5E1"), lineWidth: 1)
                            )
                        Text(group.title)
                            .font(.system(size: 16, weight: .semibold))
                        Spacer()
                        Text("\(group.words.count)")
                            .font(.system(size: 13))
                            .foregroundColor(Color(hex: "#64748B"))
                    }
                }
            }
        }
    }

    /// Turning the last group off would leave a keyboard with nothing on it, so
    /// the last one on refuses to turn off.
    private func binding(for id: String) -> Binding<Bool> {
        Binding(
            get: { groupIds.contains(id) },
            set: { on in
                if on {
                    if !groupIds.contains(id) { groupIds.append(id) }
                } else if groupIds.count > 1 {
                    groupIds.removeAll { $0 == id }
                }
            }
        )
    }
}

/// Editing one key of one keyboard page.
///
/// Deliberately not `QuickEditModalView`: that one is keyed on an integer slot
/// and writes into `page.tiles`, and a keyboard key is neither. What it edits
/// is an *override* over a word the word bank owns, so every control here can
/// also be put back — "Reset to the original word" has no equivalent on a tile.
public struct KeyboardKeyEditorModalView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject public var store: AACStore
    public let wordId: String

    @State private var label: String = ""
    @State private var tts: String = ""
    @State private var icon: String = ""
    @State private var colorHex: String = "#FFFFFF"
    @State private var photoData: Data? = nil
    @State private var audioData: Data? = nil
    @State private var hidden: Bool = false
    @State private var activeSheet: KeyEditSheet? = nil
    @StateObject private var recorder = AudioRecorder()

    public init(store: AACStore, wordId: String) {
        self.store = store
        self.wordId = wordId
    }

    /// The word as the bank defines it, before this page changed anything.
    private var original: SymbolWord? {
        SymbolWordBank.groups.lazy.flatMap(\.words).first { $0.id == wordId }
    }

    private var spokenText: String { tts.isEmpty ? label : tts }

    public var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Word & Speech Text"),
                        footer: Text("Changing a word here changes it on this page only. The same word on another keyboard page is left alone.")) {
                    LabeledField(label: "On the key", placeholder: "Word shown on the key", text: $label)
                    LabeledField(label: "Voice says", placeholder: "Same as the key", text: $tts)
                }

                Section(header: Text("Voice")) {
                    Button(action: {
                        SpeechManager.shared.speak(spokenText,
                                                   rate: Float(store.settings.speechRate),
                                                   voiceId: store.settings.voiceId)
                    }) {
                        Label("Play Preview", systemImage: "play.circle.fill")
                    }
                    .disabled(spokenText.isEmpty)

                    Button(action: toggleRecording) {
                        Label(recorder.isRecording
                              ? "Stop Recording"
                              : (audioData == nil ? "Record Own Voice" : "Re-record Own Voice"),
                              systemImage: recorder.isRecording ? "stop.circle.fill" : "mic.circle.fill")
                            .foregroundColor(recorder.isRecording ? .red : Color(hex: "#008369"))
                    }

                    if audioData != nil {
                        Button(action: { if let d = audioData { SpeechManager.shared.playAudioData(d) } }) {
                            Label("Play Recording", systemImage: "waveform")
                        }
                        Button("Remove Recording") { audioData = nil }
                            .foregroundColor(.red)
                    }
                }

                Section(header: Text("Picture")) {
                    HStack(spacing: 14) {
                        preview()
                        Text(photoData != nil ? "Photo set" : "Using the built-in picture")
                            .font(.system(size: 14))
                            .foregroundColor(Color(hex: "#64748B"))
                        Spacer()
                    }
                    .padding(.vertical, 2)

                    PictureSourceRows(data: $photoData) { request in
                        activeSheet = .photo(request)
                    }

                    Button {
                        activeSheet = .symbols
                    } label: {
                        Label("Choose a Different Symbol", systemImage: "magnifyingglass")
                    }
                }

                Section(header: Text("Colour"),
                        footer: Text("The colour is the Fitzgerald key - yellow people, green actions, orange things. Changing it changes what the key tells a child about the word.")) {
                    ColorRow(title: "Key Colour", hex: $colorHex)
                }

                Section(footer: Text(hidden
                                     ? "This word is off the page. It still shows here in the editor, greyed, so you can put it back."
                                     : "Takes this word off this page. Nothing is deleted and no other page changes.")) {
                    Toggle("Remove from this page", isOn: $hidden)

                    Button("Reset to the Original Word") { resetToOriginal() }
                        .foregroundColor(Color(hex: "#B91C1C"))
                }
            }
            .navigationBarTitle(label.isEmpty ? "Edit Key" : label, displayMode: .inline)
            .navigationBarItems(
                leading: Button("Cancel") { presentationMode.wrappedValue.dismiss() },
                trailing: Button("Save") {
                    save()
                    presentationMode.wrappedValue.dismiss()
                }
                .font(.headline)
                .foregroundColor(Color(hex: "#008369"))
            )
        }
        .navigationViewStyle(StackNavigationViewStyle())
        .sheet(item: $activeSheet) { sheet in
            switch sheet {
            case .photo(let request):
                ImagePicker(sourceType: request.source) { img in
                    let sized = img.downscaled(maxDimension: 1024)
                    photoData = sized.jpegData(compressionQuality: 0.85)
                }
            case .symbols:
                SymbolPickerView(set: store.settings.symbolSet) { name in
                    icon = name
                    photoData = nil
                }
            }
        }
        .onReceive(recorder.$recordedData.compactMap { $0 }) { data in audioData = data }
        .onAppear(perform: load)
    }

    @ViewBuilder
    private func preview() -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: 10)
                .fill(Color(hex: colorHex))
                .frame(width: 64, height: 64)
            RoundedRectangle(cornerRadius: 10)
                .stroke(Color(hex: "#CBD5E1"), lineWidth: 1)
                .frame(width: 64, height: 64)

            if let data = photoData, let ui = UIImage(data: data) {
                Image(uiImage: ui)
                    .resizable().scaledToFill()
                    .frame(width: 58, height: 58)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            } else if let img = SymbolLibrary.image(named: icon) {
                Image(uiImage: img).resizable().aspectRatio(contentMode: .fit).frame(width: 46, height: 46)
            } else {
                Text(icon).font(.system(size: 32))
            }
        }
    }

    private func toggleRecording() {
        if recorder.isRecording { recorder.stopRecording() } else { recorder.startRecording() }
    }

    private func load() {
        guard let base = original else { return }
        let applied = base.applying(store.currentPage.keyboardEdits?[wordId])
        label = applied.label
        tts = applied.tts
        icon = applied.icon
        colorHex = applied.color
        photoData = applied.photoData
        audioData = applied.audioData
        hidden = store.currentPage.keyboardEdits?[wordId]?.hidden ?? false
    }

    private func resetToOriginal() {
        guard let base = original else { return }
        label = base.label
        tts = base.tts
        icon = base.icon
        colorHex = base.color
        photoData = nil
        audioData = nil
        hidden = false
    }

    /// Only what actually differs from the word bank is written. A page where
    /// nothing was changed stores nothing, so `aac_pages.json` does not grow by
    /// ninety-five identical entries the first time somebody opens a key.
    private func save() {
        guard let base = original else { return }
        var edit = KeyboardKeyEdit()
        if label != base.label       { edit.label = label }
        if tts != base.tts           { edit.tts = tts }
        if icon != base.icon         { edit.icon = icon }
        if colorHex != base.color    { edit.colorHex = colorHex }
        edit.photoData = photoData
        edit.audioData = audioData
        if hidden                    { edit.hidden = true }

        var edits = store.currentPage.keyboardEdits ?? [:]
        if edit.isEmpty { edits.removeValue(forKey: wordId) } else { edits[wordId] = edit }
        store.currentPage.keyboardEdits = edits.isEmpty ? nil : edits
        store.save()
    }
}

/// One sheet, one enum - the trap this codebase has hit four times.
private enum KeyEditSheet: Identifiable {
    case photo(ImagePickerRequest)
    case symbols

    var id: String {
        switch self {
        case .photo(let r): return "photo-\(r.id)"
        case .symbols:      return "symbols"
        }
    }
}
