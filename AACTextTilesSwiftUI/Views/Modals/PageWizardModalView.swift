import SwiftUI

public struct PageWizardModalView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject public var store: AACStore

    @State private var title: String = "New Board"
    @State private var selectedType: PageType = .grid
    @State private var selectedGridSize: Int = 4
    @State private var selectedPreset: String = "Core"

    private let gridSizes = [1, 2, 4, 9, 16, 25, 36]

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

                    Section(header: Text("3. Starter Content Presets")) {
                        Picker("Preset", selection: $selectedPreset) {
                            Text("⭐ Core Words").tag("Core")
                            Text("🍎 Meals & Drinks").tag("Food")
                            Text("😊 Feelings").tag("Feelings")
                            Text("Blank Grid").tag("Blank")
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

    private func createPage() {
        var newPage = PageModel(title: title, type: selectedType, gridSize: selectedGridSize)
        if selectedPreset == "Food" {
            newPage.tiles[1] = TileModel(id: 1, label: "Eat", tts: "Eat food", symbolName: "eat", bgHex: "#C8E6C9")
            newPage.tiles[2] = TileModel(id: 2, label: "Drink", tts: "Drink water", symbolName: "water", bgHex: "#BBDEFB")
            newPage.tiles[3] = TileModel(id: 3, label: "More", tts: "More please", symbolName: "more", bgHex: "#FFF9C4")
            newPage.tiles[4] = TileModel(id: 4, label: "Finished", tts: "I am all done", symbolName: "stop", bgHex: "#FFCDD2")
        }
        store.pages.append(newPage)
        store.currentPageIndex = store.pages.count - 1
        store.save()
    }
}
