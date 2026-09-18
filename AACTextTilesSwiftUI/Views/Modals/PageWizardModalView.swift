import SwiftUI

public struct PageWizardModalView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject public var store: AACStore

    @State private var title: String = "New Board"
    @State private var selectedType: PageType = .grid
    @State private var selectedGridSize: Int = 4
    @State private var selectedPreset: String = "Blank"

    // Scene pages are a picture with hotspots on it, so the picture is part of
    // making the page - not something to go hunting for in the editor after.
    @State private var scenePhoto: UIImage? = nil
    @State private var scenePhotoData: Data? = nil
    @State private var pickerRequest: ImagePickerRequest? = nil
    @State private var templateCategory: String = "All"

    // The symbol keyboard used to be the one page format the wizard asked
    // nothing about - you got a keyboard, and it was whatever the code said it
    // was. Its two real choices belong here, next to every other page format's.
    @State private var keyboardKeys: Int = SymbolWordBank.defaultKeyCount
    @State private var keyboardGroups: [String] = SymbolWordBank.groups.map(\.id)

    // Kept in step with `SymbolWordBank.keyCountOptions`, which the Symbol
    // Keyboard's size picker uses - the two page kinds ask "how big should the
    // pictures be" and must not answer with two different sets of numbers.
    private let gridSizes = SymbolWordBank.keyCountOptions

    public var body: some View {
        NavigationView {
            Form {
                Section(header: Text("1. Page Title & Format")) {
                    TextField("Page Title", text: $title)
                    Picker("Page Format", selection: $selectedType) {
                        ForEach(PageType.allCases, id: \.self) { type in
                            Text(type.displayName).tag(type)
                        }
                    }
                    .pickerStyle(SegmentedPickerStyle())
                }

                if selectedType == .grid {
                    Section(header: Text("2. Button Grid Layout")) {
                        Picker("Grid Size", selection: $selectedGridSize) {
                            ForEach(gridSizes, id: \.self) { size in
                                Text("\(size)").tag(size)
                            }
                        }
                        .pickerStyle(SegmentedPickerStyle())
                    }

                    Section(header: Text("3. Starter Content")) {
                        starterContentPicker
                    }
                }

                if selectedType == .scene {
                    scenePictureSection
                }

                if selectedType == .keyboard {
                    KeyboardSetupSections(keys: $keyboardKeys,
                                          groupIds: $keyboardGroups,
                                          stepNumber: 2)
                }
            }
            .sheet(item: $pickerRequest) { request in
                ImagePicker(sourceType: request.source) { img in
                    let sized = img.downscaled(maxDimension: 2048)
                    scenePhoto = sized
                    scenePhotoData = sized.jpegData(compressionQuality: 0.85)
                }
            }
            .onAppear {
                // Lets the branches of the wizard that are not the default
                // actually be opened and looked at:
                //   -openScreen wizard -wizardType scene
                //   -openScreen wizard -wizardType keyboard
                switch UserDefaults.standard.string(forKey: "wizardType") {
                case "scene":    selectedType = .scene
                case "keyboard": selectedType = .keyboard
                default:         break
                }
            }
            .navigationBarTitle("New Page Wizard", displayMode: .inline)
            .navigationBarItems(
                leading: Button("Cancel") { presentationMode.wrappedValue.dismiss() },
                trailing: Button("Create Page") {
                    createPage()
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
    }

    // MARK: - Starter content

    /// The ready-made boards used to live on the Home screen as a shelf you
    /// tapped to bolt a board onto the book. They belong here: this is the
    /// screen that makes a page, and picking a starter board is a choice about
    /// the page being made, not a separate errand. The plain "Start from"
    /// dropdown that used to sit here hid 28 boards behind one line of text.
    @ViewBuilder
    private var starterContentPicker: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                categoryChip("All")
                ForEach(PageTemplateCatalog.categories, id: \.self) { categoryChip($0) }
            }
            .padding(.vertical, 4)
        }

        LazyVGrid(
            columns: [GridItem(.adaptive(minimum: 156), spacing: 12)],
            spacing: 12
        ) {
            blankCard
            ForEach(visibleTemplates) { templateCard($0) }
        }
        .padding(.vertical, 6)
    }

    private var visibleTemplates: [PageTemplate] {
        templateCategory == "All"
            ? PageTemplateCatalog.all
            : PageTemplateCatalog.templates(in: templateCategory)
    }

    @ViewBuilder
    private func categoryChip(_ name: String) -> some View {
        let selected = templateCategory == name
        Button(action: { templateCategory = name }) {
            Text(name)
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(selected ? .white : Color(hex: "#475569"))
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                .background(selected ? Color(hex: "#008369") : Color(hex: "#F1F5F9"))
                .cornerRadius(16)
        }
        .buttonStyle(PlainButtonStyle())
    }

    private var blankCard: some View {
        let selected = selectedPreset == "Blank"
        return Button(action: { selectedPreset = "Blank" }) {
            VStack(alignment: .leading, spacing: 6) {
                Text("⬜").font(.system(size: 30))
                Text("Blank Grid")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(Color(hex: "#1E293B"))
                Text("Empty buttons to fill in yourself")
                    .font(.system(size: 11))
                    .foregroundColor(Color(hex: "#64748B"))
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
                Spacer(minLength: 0)
            }
            .padding(12)
            .frame(height: 150, alignment: .topLeading)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)
            .cornerRadius(14)
            .overlay(cardBorder(selected: selected, accent: "#94A3B8"))
        }
        .buttonStyle(PlainButtonStyle())
    }

    private func templateCard(_ template: PageTemplate) -> some View {
        let selected = selectedPreset == template.id
        return Button(action: { selectedPreset = template.id }) {
            VStack(alignment: .leading, spacing: 6) {
                Text(template.icon).font(.system(size: 30))
                Text(template.title)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(Color(hex: "#1E293B"))
                    .lineLimit(1)
                Text(template.summary)
                    .font(.system(size: 11))
                    .foregroundColor(Color(hex: "#64748B"))
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
                Spacer(minLength: 0)
                Text("\(template.buttonCount) buttons")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color(hex: template.accent))
                    .cornerRadius(6)
            }
            .padding(12)
            .frame(height: 150, alignment: .topLeading)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)
            .cornerRadius(14)
            .overlay(cardBorder(selected: selected, accent: template.accent))
        }
        .buttonStyle(PlainButtonStyle())
    }

    @ViewBuilder
    private func cardBorder(selected: Bool, accent: String) -> some View {
        ZStack(alignment: .topTrailing) {
            RoundedRectangle(cornerRadius: 14)
                .stroke(selected ? Color(hex: "#008369") : Color(hex: accent).opacity(0.45),
                        lineWidth: selected ? 3 : 1.5)
            if selected {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 20))
                    .foregroundColor(Color(hex: "#008369"))
                    .background(Circle().fill(Color.white))
                    .padding(8)
            }
        }
    }

    // MARK: - Scene picture

    @ViewBuilder
    private var scenePictureSection: some View {
        Section(
            header: Text("2. Scene Picture"),
            footer: Text(scenePhoto == nil
                         ? "Optional - a page with no picture opens on a plain backdrop, and you can add one later from Page Options. Hotspots are placed on top of this picture."
                         : "Hotspots are placed on top of this picture. You can replace it later from Page Options.")
        ) {
            ScenePicturePicker(image: $scenePhoto, data: $scenePhotoData) { request in
                pickerRequest = request
            }
        }
    }

    private var chosenTemplate: PageTemplate? {
        PageTemplateCatalog.all.first { $0.id == selectedPreset }
    }

    /// Every preset now writes real buttons. Previously only "Food" did, so
    /// picking "Core Words" or "Feelings" produced an empty grid with a
    /// promising name.
    private func createPage() {
        var newPage: PageModel
        if selectedType == .grid, let template = chosenTemplate {
            newPage = template.makePage()
            newPage.title = title.isEmpty ? template.title : title
            // The chosen grid wins over the template's own, but never so small
            // that it would hide buttons the template put on the page.
            newPage.gridSize = max(selectedGridSize, template.buttonCount)
        } else {
            newPage = PageModel(title: title, type: selectedType, gridSize: selectedGridSize)
            if selectedType == .scene {
                newPage.sceneImageData = scenePhotoData
            }
            if selectedType == .keyboard {
                newPage.keyboardKeys = keyboardKeys
                newPage.keyboardGroups = keyboardGroups
                // "New Board" is a grid's name. A keyboard left unnamed says
                // what it is in the page list instead.
                if title.isEmpty || title == "New Board" {
                    newPage.title = "Symbol Keyboard"
                }
            }
        }
        store.pages.append(newPage)
        store.currentPageIndex = store.pages.count - 1
        store.save()
    }
}
