import SwiftUI

public struct OnlineGalleryModalView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject public var store: AACStore

    public var body: some View {
        NavigationView {
            List {
                galleryCard(
                    title: "Medical & Healthcare Needs (9 Buttons)",
                    description: "Pain scale, symptoms, doctor, medicine, call nurse",
                    buttonCount: 9,
                    onInstall: {
                        installBoard(title: "Medical Needs", size: 9)
                    }
                )
                galleryCard(
                    title: "Feelings & Sensory Check-In (9 Buttons)",
                    description: "Emotions, tired, overwhelmed, sensory break, happy, calm",
                    buttonCount: 9,
                    onInstall: {
                        installBoard(title: "Feelings Check-In", size: 9)
                    }
                )
                galleryCard(
                    title: "Restaurant & Food Ordering (9 Buttons)",
                    description: "Pizza, burger, water, juice, napkin, bill please",
                    buttonCount: 9,
                    onInstall: {
                        installBoard(title: "Restaurant Dining", size: 9)
                    }
                )
                galleryCard(
                    title: "School & Classroom Routine (9 Buttons)",
                    description: "Raise hand, bathroom, pencil, backpack, recess, teacher",
                    buttonCount: 9,
                    onInstall: {
                        installBoard(title: "Classroom Routine", size: 9)
                    }
                )
            }
            .navigationBarTitle("Online AAC Template Gallery", displayMode: .inline)
            .navigationBarItems(trailing: Button("Done") { presentationMode.wrappedValue.dismiss() })
        }
    }

    private func galleryCard(title: String, description: String, buttonCount: Int, onInstall: @escaping () -> Void) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.headline)
                .foregroundColor(Color(hex: "#1E293B"))
            Text(description)
                .font(.subheadline)
                .foregroundColor(Color(hex: "#64748B"))
            HStack {
                Text("\(buttonCount) Buttons")
                    .font(.caption)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Color(hex: "#E2E8F0"))
                    .cornerRadius(6)
                Spacer()
                Button("📥 Add to Book", action: onInstall)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 6)
                    .background(Color(hex: "#008369"))
                    .cornerRadius(8)
            }
        }
        .padding(.vertical, 6)
    }

    private func installBoard(title: String, size: Int) {
        let page = PageModel(title: title, type: .grid, gridSize: size)
        store.pages.append(page)
        store.currentPageIndex = store.pages.count - 1
        store.save()
        presentationMode.wrappedValue.dismiss()
    }
}
