import SwiftUI
import UIKit

/// One sheet, one enum. Page Options can present a photo picker or a share
/// sheet, and two `.sheet` modifiers on the same view silently refuse to
/// present - the trap that made the scene editor's picture buttons look dead.
private enum OptionsSheet: Identifiable {
    case picker(ImagePickerRequest)
    case share(URL)

    var id: String {
        switch self {
        case .picker(let r): return "picker-\(r.id)"
        case .share(let url): return "share-\(url.lastPathComponent)"
        }
    }
}

public struct PageOptionsModalView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject public var store: AACStore

    @State private var title: String = ""
    @State private var gridSize: Int = 4
    @State private var bgHex: String = "#FFFFFF"
    @State private var express: Bool = false

    @State private var keyboardKeys: Int = SymbolWordBank.defaultKeyCount
    @State private var keyboardGroups: [String] = SymbolWordBank.groups.map(\.id)

    @State private var scenePhoto: UIImage? = nil
    @State private var scenePhotoData: Data? = nil
    @State private var activeSheet: OptionsSheet? = nil

    // One list, shared with the New Page Wizard and the symbol keyboard. A
    // segmented picker whose selection is not one of its tags renders blank, so
    // three lists that nearly agreed meant a page saved under one of them could
    // open with nothing selected.
    private let gridSizes = SymbolWordBank.keyCountOptions

    /// Says out loud what shrinking the grid would do. Nine buttons in a grid
    /// of four does not delete five of them, it hides them, and a parent who
    /// cannot tell the difference will not touch the control.
    private var gridSizeFooter: String {
        let used = store.currentPage.tiles.keys.filter { $0 <= gridSize }.count
        let total = store.currentPage.tiles.count
        if total > gridSize {
            return "How many buttons fit on this page. This page has \(total) buttons - at \(gridSize) the last \(total - gridSize) are hidden until you make the grid bigger again. Nothing is deleted."
        }
        return "How many buttons fit on this page. \(used) of \(gridSize) are filled in."
    }

    public var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Page")) {
                    TextField("Page Title", text: $title)
                        .font(.system(size: 17, weight: .semibold))

                    ColorRow(title: "Background", hex: $bgHex)
                }

                // The size of a grid was a collapsed menu here and a row of
                // numbers in the New Page Wizard - so on a page made from a
                // starter board it read as "the picker isn't there", the choice
                // having shrunk to a small grey 9 at the end of a line. It is
                // the same question in both places and now looks the same in
                // both places.
                if store.currentPage.type == .grid {
                    Section(
                        header: Text("Button Grid Layout"),
                        footer: Text(gridSizeFooter)
                    ) {
                        Picker("Buttons", selection: $gridSize) {
                            ForEach(gridSizes, id: \.self) { size in
                                Text("\(size)").tag(size)
                            }
                        }
                        .pickerStyle(SegmentedPickerStyle())
                        .labelsHidden()
                    }
                }

                // The scene picture control used to live on the scene canvas
                // itself. It moved here so a page that already exists can still
                // have its picture changed - the wizard only sets it once, at
                // creation, and a scene with no way back to its picture is a
                // dead end.
                if store.currentPage.type == .scene {
                    scenePictureSection
                }

                if store.currentPage.type == .keyboard {
                    KeyboardSetupSections(keys: $keyboardKeys, groupIds: $keyboardGroups)
                }

                Section(header: Text("Behavior")) {
                    Toggle("Express sentence bar", isOn: $express)
                }

                Section {
                    Button(action: sharePage) {
                        HStack(spacing: 10) {
                            Image(systemName: "square.and.arrow.up")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(Color(hex: "#008369"))
                                .frame(width: 26)
                            Text("Share This Page")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(Color(hex: "#1E293B"))
                            Spacer()
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(PlainButtonStyle())
                } footer: {
                    Text("Sends this page as a file you can message, email or save - another Talk Tiles device can add it to their book.")
                }
            }
            .navigationBarTitle("Page Options", displayMode: .inline)
            .navigationBarItems(
                leading: Button("Cancel") { presentationMode.wrappedValue.dismiss() },
                trailing: Button("Save") {
                    savePageOptions()
                    presentationMode.wrappedValue.dismiss()
                }
                .font(.headline)
                .foregroundColor(Color(hex: "#008369"))
            )
        }
        .navigationViewStyle(StackNavigationViewStyle())
        .sheet(item: $activeSheet) { sheet in
            switch sheet {
            case .picker(let request):
                ImagePicker(sourceType: request.source) { img in
                    let sized = img.downscaled(maxDimension: 2048)
                    scenePhoto = sized
                    scenePhotoData = sized.jpegData(compressionQuality: 0.85)
                }
            case .share(let url):
                ActivityView(items: [url])
            }
        }
        .onAppear(perform: loadCurrentPage)
    }

    // MARK: - Scene picture

    @ViewBuilder
    private var scenePictureSection: some View {
        Section(
            header: Text("Scene Picture"),
            footer: Text("Hotspots sit on top of this picture. Changing it keeps the hotspots exactly where they are.")
        ) {
            ScenePicturePicker(image: $scenePhoto, data: $scenePhotoData) { request in
                activeSheet = .picker(request)
            }
        }
    }

    // MARK: - Share

    /// Writes the page to a real file first. A share sheet handed a raw string
    /// offers "copy" and little else; handed a .json file it offers Messages,
    /// Mail, AirDrop and Files, which is what sharing a board actually means.
    private func sharePage() {
        let page = store.currentPage
        let safeTitle = page.title
            .lowercased()
            .replacingOccurrences(of: " ", with: "_")
            .filter { $0.isLetter || $0.isNumber || $0 == "_" }
        let name = "talk_tiles_page_\(safeTitle.isEmpty ? "page" : safeTitle).json"
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(name)

        let encoder = JSONEncoder()
        encoder.outputFormatting = .prettyPrinted
        guard let data = try? encoder.encode(page), (try? data.write(to: url, options: .atomic)) != nil else { return }
        activeSheet = .share(url)
    }

    // MARK: - Load / Save

    private func loadCurrentPage() {
        let p = store.currentPage
        title = p.title
        // A page saved with a size no longer offered (a template's own number,
        // or one from an older build) would otherwise open on a blank picker.
        gridSize = gridSizes.contains(p.gridSize) ? p.gridSize : SymbolWordBank.nearestKeyCount(p.gridSize)
        bgHex = p.bgHex
        express = p.express
        scenePhotoData = p.sceneImageData
        scenePhoto = p.sceneImageData.flatMap { UIImage(data: $0) }
        keyboardKeys = SymbolWordBank.nearestKeyCount(p.keyboardKeys)
        keyboardGroups = p.keyboardGroups ?? SymbolWordBank.groups.map(\.id)
    }

    private func savePageOptions() {
        store.currentPage.title = title
        store.currentPage.gridSize = gridSize
        store.currentPage.bgHex = bgHex
        store.currentPage.express = express
        if store.currentPage.type == .scene {
            store.currentPage.sceneImageData = scenePhotoData
        }
        if store.currentPage.type == .keyboard {
            store.currentPage.keyboardKeys = keyboardKeys
            store.currentPage.keyboardGroups = keyboardGroups
        }
        store.save()
    }
}

/// The one colour palette in the app.
///
/// There used to be two: sixteen named swatches with a custom picker for a page
/// background, and ten unnamed circles with no custom picker for a button. Same
/// question, two different answers, and only one of them could reach a colour
/// that was not on the list. The Fitzgerald key colours the starter boards paint
/// buttons with were on neither, so a parent could not match a new button to the
/// ones already on the page.
public enum AppPalette {
    public static let swatches: [(String, String)] = [
        ("#FFFFFF", "White"),   ("#F4F5F8", "Paper"),   ("#B0B7BD", "Silver"),  ("#6C757D", "Grey"),
        ("#111111", "Black"),   ("#FFF9C4", "Yellow"),  ("#C8E6C9", "Green"),   ("#BBDEFB", "Blue"),
        ("#FFE0B2", "Orange"),  ("#F8BBD0", "Pink"),    ("#D1C4E9", "Purple"),  ("#FFCDD2", "Red"),
        ("#B8E8DB", "Mint"),    ("#004838", "Forest"),  ("#00A699", "Teal"),    ("#F4A261", "Apricot"),
        ("#EA695B", "Coral"),   ("#C4312A", "Brick"),   ("#1A237E", "Navy"),    ("#795548", "Cocoa")
    ]
}

/// Swatches and a custom picker, the two tabs the web version had. Used for
/// every colour in the app - page background, button colour, border and text -
/// so the same twenty swatches and the same custom picker answer the question
/// wherever it is asked.
public struct ColorChoiceView: View {
    public let title: String
    @Binding var hex: String
    @State private var tab: Int = 0

    public init(title: String, hex: Binding<String>) {
        self.title = title
        self._hex = hex
    }

    private var swatches: [(String, String)] { AppPalette.swatches }

    public var body: some View {
        VStack(spacing: 16) {
            Picker("", selection: $tab) {
                Text("Swatches").tag(0)
                Text("Picker").tag(1)
            }
            .pickerStyle(SegmentedPickerStyle())
            .padding(.horizontal)

            if tab == 0 {
                // Twenty swatches do not fit a short sheet in landscape, and a
                // grid that is merely clipped looks like a grid with four rows.
                ScrollView {
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 14), count: 4), spacing: 14) {
                        ForEach(swatches, id: \.0) { swatch, name in
                            Button(action: { hex = swatch }) {
                                VStack(spacing: 6) {
                                    RoundedRectangle(cornerRadius: 10)
                                        .fill(Color(hex: swatch))
                                        .frame(height: 56)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 10)
                                                .stroke(hex.caseInsensitiveCompare(swatch) == .orderedSame
                                                        ? Color(hex: "#008369") : Color(hex: "#CBD5E1"),
                                                        lineWidth: hex.caseInsensitiveCompare(swatch) == .orderedSame ? 4 : 1)
                                        )
                                    Text(name)
                                        .font(.system(size: 11, weight: .semibold))
                                        .foregroundColor(Color(hex: "#64748B"))
                                }
                            }
                            .buttonStyle(PlainButtonStyle())
                        }
                    }
                    .padding(.horizontal)
                    .padding(.bottom, 24)
                }
            } else {
                VStack(spacing: 18) {
                    ColorPicker("Pick any colour", selection: Binding(
                        get: { Color(hex: hex) },
                        set: { hex = $0.hexString }
                    ), supportsOpacity: false)
                    .font(.system(size: 17, weight: .semibold))

                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color(hex: hex))
                        .frame(height: 120)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(Color(hex: "#CBD5E1"), lineWidth: 1)
                        )
                    Text(hex.uppercased())
                        .font(.system(size: 15, weight: .bold, design: .monospaced))
                        .foregroundColor(Color(hex: "#64748B"))
                }
                .padding(.horizontal)

                Spacer()
            }
        }
        .padding(.top, 16)
        .navigationBarTitle(title, displayMode: .inline)
    }
}

/// The row that opens `ColorChoiceView`, with a swatch big enough to read.
/// A white background used to render as an all-but-invisible outline.
public struct ColorRow: View {
    public let title: String
    @Binding var hex: String

    public init(title: String, hex: Binding<String>) {
        self.title = title
        self._hex = hex
    }

    public var body: some View {
        NavigationLink(destination: ColorChoiceView(title: title, hex: $hex)) {
            HStack {
                Text(title)
                Spacer()
                Text(AppPalette.swatches.first { $0.0.caseInsensitiveCompare(hex) == .orderedSame }?.1
                     ?? hex.uppercased())
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "#64748B"))
                RoundedRectangle(cornerRadius: 6)
                    .fill(Color(hex: hex))
                    .frame(width: 48, height: 26)
                    .overlay(
                        RoundedRectangle(cornerRadius: 6)
                            .stroke(Color(hex: "#94A3B8"), lineWidth: 1)
                    )
            }
        }
    }
}

/// Kept so the name that already exists in the codebase still resolves.
public struct PageBackgroundPickerView: View {
    @Binding var bgHex: String

    public init(bgHex: Binding<String>) { self._bgHex = bgHex }

    public var body: some View {
        ColorChoiceView(title: "Page Background", hex: $bgHex)
    }
}

/// Plain UIActivityViewController wrapper - iOS 15 has no ShareLink.
public struct ActivityView: UIViewControllerRepresentable {
    public let items: [Any]

    public func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    public func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
