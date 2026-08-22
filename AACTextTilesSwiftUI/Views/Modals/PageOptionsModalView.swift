import SwiftUI

public struct PageOptionsModalView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject public var store: AACStore

    @State private var title: String = ""
    @State private var gridSize: Int = 4
    @State private var bgHex: String = "#FFFFFF"
    @State private var express: Bool = false
    @State private var enabled: Bool = true
    @State private var isLocked: Bool = false

    private let gridSizes = [1, 2, 4, 9, 12, 16, 25, 36, 48]
    private let bgColors = [
        ("#FFFFFF", "White"),
        ("#FEF9C3", "Cream"),
        ("#DCFCE7", "Mint"),
        ("#E0F2FE", "Sky"),
        ("#F3E8FF", "Lavender"),
        ("#FFE4E6", "Rose"),
        ("#F1F5F9", "Slate"),
        ("#1E293B", "Dark")
    ]

    public var body: some View {
        NavigationView {
            Form {
                // Everything that describes the page itself, in one place.
                Section(header: Text("Page")) {
                    TextField("Page Title", text: $title)
                        .font(.system(size: 17, weight: .semibold))

                    if store.currentPage.type == .grid {
                        Picker("Buttons", selection: $gridSize) {
                            ForEach(gridSizes, id: \.self) { size in
                                Text("\(size)").tag(size)
                            }
                        }
                    }

                    colorRow()
                }

                // Three switches. They do not need illustrations.
                Section(header: Text("Behavior")) {
                    Toggle("Express sentence bar", isOn: $express)
                    Toggle("Include in page navigation", isOn: $enabled)
                    Toggle("Lock from edits", isOn: $isLocked)
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
        .onAppear {
            let p = store.currentPage
            title = p.title
            gridSize = p.gridSize
            bgHex = p.bgHex
            express = p.express
            enabled = p.enabled
            isLocked = store.isLocked
        }
    }

    /// Colour lives on one scrolling line instead of a titled section of its own.
    @ViewBuilder
    private func colorRow() -> some View {
        HStack(spacing: 10) {
            Text("Background")
            Spacer(minLength: 8)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(bgColors, id: \.0) { hex, name in
                        Button(action: { bgHex = hex }) {
                            Circle()
                                .fill(Color(hex: hex))
                                .frame(width: 30, height: 30)
                                .overlay(
                                    Circle().stroke(
                                        bgHex == hex ? Color(hex: "#008369") : Color(hex: "#CBD5E1"),
                                        lineWidth: bgHex == hex ? 3 : 1
                                    )
                                )
                        }
                        .buttonStyle(PlainButtonStyle())
                        .accessibilityLabel(name)
                    }
                }
                .padding(.vertical, 6)
            }
        }
    }

    private func savePageOptions() {
        store.currentPage.title = title
        store.currentPage.gridSize = gridSize
        store.currentPage.bgHex = bgHex
        store.currentPage.express = express
        store.currentPage.enabled = enabled
        store.isLocked = isLocked
        store.save()
    }
}
