import SwiftUI

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

                // Section 2: Symbol Selection
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

                // Section 3: Color Scheme
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

                // Section 4: Word Size
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
        .onAppear {
            if let tile = store.currentPage.tiles[slotId.value] {
                label = tile.label
                tts = tile.tts
                selectedSymbol = tile.symbolName
                selectedBgHex = tile.bgHex
                selectedBorderHex = tile.borderHex
                labelSize = tile.labelSize
                isSoundItOut = tile.isSoundItOut
            }
        }
    }

    private func symbolChip(name: String, emoji: String) -> some View {
        Button(action: {
            selectedSymbol = name
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

    private func saveTile() {
        let updated = TileModel(
            id: slotId.value,
            label: label,
            tts: tts.isEmpty ? label : tts,
            symbolName: selectedSymbol,
            bgHex: selectedBgHex,
            borderHex: selectedBorderHex,
            labelSize: labelSize,
            isSoundItOut: isSoundItOut
        )
        store.currentPage.tiles[slotId.value] = updated
        store.save()
    }
}
