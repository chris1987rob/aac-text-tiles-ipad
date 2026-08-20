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
    @State private var auditoryCues: Bool = false

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
                // Section 1: Page Details
                Section(header: Label("Page Title & Format", systemImage: "doc.text.fill")) {
                    TextField("Page Title", text: $title)
                        .font(.system(size: 18, weight: .semibold))
                    HStack {
                        Text("Page Type")
                            .foregroundColor(Color(hex: "#64748B"))
                        Spacer()
                        Text(store.currentPage.type.rawValue)
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(Color(hex: "#008369"))
                    }
                }

                // Section 2: Grid Size (if grid page)
                if store.currentPage.type == .grid {
                    Section(header: Label("Button Grid Layout", systemImage: "square.grid.3x3.fill")) {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 10) {
                                ForEach(gridSizes, id: \.self) { size in
                                    Button(action: { gridSize = size }) {
                                        Text("\(size)")
                                            .font(.system(size: 17, weight: .bold))
                                            .foregroundColor(gridSize == size ? .white : Color(hex: "#1E293B"))
                                            .frame(width: 48, height: 44)
                                            .background(gridSize == size ? Color(hex: "#008369") : Color(hex: "#F1F5F9"))
                                            .cornerRadius(10)
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 10)
                                                    .stroke(gridSize == size ? Color(hex: "#006853") : Color(hex: "#CBD5E1"), lineWidth: 1.5)
                                            )
                                    }
                                    .buttonStyle(PlainButtonStyle())
                                }
                            }
                            .padding(.vertical, 4)
                        }
                    }
                }

                // Section 3: Background Color
                Section(header: Label("Page Background Color", systemImage: "paintpalette.fill")) {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(bgColors, id: \.0) { hex, name in
                                Button(action: { bgHex = hex }) {
                                    VStack(spacing: 4) {
                                        Circle()
                                            .fill(Color(hex: hex))
                                            .frame(width: 40, height: 40)
                                            .overlay(
                                                Circle()
                                                    .stroke(bgHex == hex ? Color(hex: "#008369") : Color(hex: "#CBD5E1"), lineWidth: bgHex == hex ? 3.5 : 1)
                                            )
                                            .shadow(color: Color.black.opacity(0.1), radius: 2, y: 1)
                                        Text(name)
                                            .font(.system(size: 11, weight: .semibold))
                                            .foregroundColor(Color(hex: "#64748B"))
                                    }
                                }
                                .buttonStyle(PlainButtonStyle())
                            }
                        }
                        .padding(.vertical, 6)
                    }
                }

                // Section 4: Editor Feature Toggles (with Icons)
                Section(header: Label("Page Features & Behavior", systemImage: "switch.2")) {
                    // Express Sentence Bar Toggle
                    HStack(spacing: 12) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 8)
                                .fill(Color(hex: "#E6F4EA"))
                                .frame(width: 36, height: 36)
                            Image(systemName: "bolt.fill")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(Color(hex: "#008369"))
                        }
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Express Sentence Bar")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(Color(hex: "#1E293B"))
                            Text("Top word strip to assemble sentences")
                                .font(.system(size: 13))
                                .foregroundColor(Color(hex: "#64748B"))
                        }
                        Spacer()
                        Toggle("", isOn: $express)
                            .labelsHidden()
                    }
                    .padding(.vertical, 4)

                    // Page Enabled Toggle
                    HStack(spacing: 12) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 8)
                                .fill(Color(hex: "#E0F2FE"))
                                .frame(width: 36, height: 36)
                            Image(systemName: "eye.fill")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(Color(hex: "#0284C7"))
                        }
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Page Enabled in Book")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(Color(hex: "#1E293B"))
                            Text("Include in player arrow navigation")
                                .font(.system(size: 13))
                                .foregroundColor(Color(hex: "#64748B"))
                        }
                        Spacer()
                        Toggle("", isOn: $enabled)
                            .labelsHidden()
                    }
                    .padding(.vertical, 4)

                    // Auditory Scan Cues Toggle
                    HStack(spacing: 12) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 8)
                                .fill(Color(hex: "#FEF3C7"))
                                .frame(width: 36, height: 36)
                            Image(systemName: "speaker.wave.2.fill")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(Color(hex: "#D97706"))
                        }
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Scanning Auditory Cues")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(Color(hex: "#1E293B"))
                            Text("Audio cues when scanning buttons")
                                .font(.system(size: 13))
                                .foregroundColor(Color(hex: "#64748B"))
                        }
                        Spacer()
                        Toggle("", isOn: $auditoryCues)
                            .labelsHidden()
                    }
                    .padding(.vertical, 4)

                    // Lock Page Modifications Toggle
                    HStack(spacing: 12) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 8)
                                .fill(Color(hex: "#FEE2E2"))
                                .frame(width: 36, height: 36)
                            Image(systemName: "lock.fill")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(Color(hex: "#DC2626"))
                        }
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Lock Page from Edits")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(Color(hex: "#1E293B"))
                            Text("Prevent accidental changes in player mode")
                                .font(.system(size: 13))
                                .foregroundColor(Color(hex: "#64748B"))
                        }
                        Spacer()
                        Toggle("", isOn: $isLocked)
                            .labelsHidden()
                    }
                    .padding(.vertical, 4)
                }
            }
            .navigationBarTitle("Page Options & Settings", displayMode: .inline)
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
