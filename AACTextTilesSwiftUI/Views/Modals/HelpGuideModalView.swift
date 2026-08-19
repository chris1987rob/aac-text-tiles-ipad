import SwiftUI

public struct HelpGuideModalView: View {
    @Environment(\.presentationMode) var presentationMode

    public var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("GoTalk Now / AAC Text Tiles Reference")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(Color(hex: "#008369"))
                        Text("Comprehensive SLP guide for communication modes and tools.")
                            .font(.subheadline)
                            .foregroundColor(Color(hex: "#64748B"))
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(hex: "#E6F4EA"))
                    .cornerRadius(12)

                    guideSection(
                        title: "1. Navigation & Modes",
                        content: "Player mode is for communication playback. Tapping any button speaks its phrase. Page Editor mode is for customizing layouts, symbols, and voice cues."
                    )
                    guideSection(
                        title: "2. Visual Scene Displays",
                        content: "Visual Scene Displays let you place interactive touch hotspots over items in a photo (e.g. cat, sofa, teacher desk). Tapping a hotspot speaks the word and flashes green feedback."
                    )
                    guideSection(
                        title: "3. Sound It Out Phonics",
                        content: "Sound It Out animates words syllable-by-syllable (e.g. but · ter · fly) before speaking the entire word."
                    )
                    guideSection(
                        title: "4. Talking Keyboard Page",
                        content: "Interactive QWERTY/ABC talking keyboard with quick phrase prediction chips (I want, Yes, No, Help, Please) and full text-to-speech playback."
                    )
                }
                .padding()
            }
            .navigationBarTitle("User Guide & Help", displayMode: .inline)
            .navigationBarItems(trailing: Button("Done") { presentationMode.wrappedValue.dismiss() })
        }
    }

    private func guideSection(title: String, content: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.headline)
                .foregroundColor(Color(hex: "#008369"))
            Text(content)
                .font(.body)
                .foregroundColor(Color(hex: "#334155"))
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color(hex: "#E2E8F0"), lineWidth: 1)
        )
    }
}
