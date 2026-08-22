import SwiftUI

public struct PageWizardModalView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject public var store: AACStore

    @State private var title: String = "New Board"
    @State private var selectedType: PageType = .grid
    @State private var selectedGridSize: Int = 4
    @State private var selectedPreset: String = "Blank"

    private let gridSizes = [1, 2, 4, 9, 16, 25, 36, 48]

    public var body: some View {
        NavigationView {
            Form {
                Section(header: Text("1. Page Title & Format")) {
                    TextField("Page Title", text: $title)
                    Picker("Page Format", selection: $selectedType) {
                        ForEach(PageType.allCases, id: \.self) { type in
                            Text(type.rawValue).tag(type)
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
                        Picker("Start from", selection: $selectedPreset) {
                            Text("Blank Grid").tag("Blank")
                            ForEach(PageTemplateCatalog.all) { template in
                                Text("\(template.title) (\(template.buttonCount))").tag(template.id)
                            }
                        }
                        if let t = chosenTemplate {
                            Text(t.summary)
                                .font(.system(size: 13))
                                .foregroundColor(Color(hex: "#64748B"))
                        }
                    }
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
        }
        store.pages.append(newPage)
        store.currentPageIndex = store.pages.count - 1
        store.save()
    }
}
