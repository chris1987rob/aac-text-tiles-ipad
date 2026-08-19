import SwiftUI

public struct HotspotEditorModalView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject public var store: AACStore
    public let hotspot: HotspotModel

    @State private var label: String = ""
    @State private var tts: String = ""
    @State private var style: HotspotStyle = .invisible
    @State private var action: HotspotAction = .tts

    public var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Hotspot Label")) {
                    TextField("Name on Photo (e.g. Cat, Sofa, TV)", text: $label)
                }

                Section(header: Text("Speech Action")) {
                    Picker("Action Type", selection: $action) {
                        ForEach(HotspotAction.allCases, id: \.self) { act in
                            Text(act.rawValue).tag(act)
                        }
                    }
                    .pickerStyle(SegmentedPickerStyle())

                    if action == .tts {
                        TextField("What to speak when tapped", text: $tts)
                    }
                }

                Section(header: Text("Visual Style in Player Mode")) {
                    Picker("Style", selection: $style) {
                        ForEach(HotspotStyle.allCases, id: \.self) { st in
                            Text(st.rawValue).tag(st)
                        }
                    }
                    .pickerStyle(SegmentedPickerStyle())
                }

                Section {
                    Button("🗑 Delete Hotspot") {
                        store.currentPage.hotspots.removeAll { $0.id == hotspot.id }
                        store.save()
                        presentationMode.wrappedValue.dismiss()
                    }
                    .foregroundColor(.red)
                }
            }
            .navigationBarTitle("Edit Hotspot Zone", displayMode: .inline)
            .navigationBarItems(
                leading: Button("Cancel") { presentationMode.wrappedValue.dismiss() },
                trailing: Button("Save") {
                    saveHotspot()
                    presentationMode.wrappedValue.dismiss()
                }
                .font(.headline)
                .foregroundColor(Color(hex: "#008369"))
            )
        }
        .onAppear {
            label = hotspot.label
            tts = hotspot.tts
            style = hotspot.style
            action = hotspot.action
        }
    }

    private func saveHotspot() {
        if let idx = store.currentPage.hotspots.firstIndex(where: { $0.id == hotspot.id }) {
            store.currentPage.hotspots[idx].label = label
            store.currentPage.hotspots[idx].tts = tts.isEmpty ? label : tts
            store.currentPage.hotspots[idx].style = style
            store.currentPage.hotspots[idx].action = action
            store.save()
        }
    }
}
