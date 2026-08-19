import SwiftUI

public struct PagesNavigatorModalView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject public var store: AACStore

    public var body: some View {
        NavigationView {
            List {
                ForEach(Array(store.pages.enumerated()), id: \.element.id) { index, page in
                    Button(action: {
                        store.currentPageIndex = index
                        presentationMode.wrappedValue.dismiss()
                    }) {
                        HStack(spacing: 12) {
                            Text("\(index + 1).")
                                .font(.headline)
                                .foregroundColor(Color(hex: "#008369"))
                                .frame(width: 32, alignment: .leading)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(page.title)
                                    .font(.headline)
                                    .foregroundColor(Color(hex: "#1E293B"))
                                Text("\(page.type.rawValue) • \(page.gridSize) buttons")
                                    .font(.caption)
                                    .foregroundColor(Color(hex: "#64748B"))
                            }
                            Spacer()
                            if index == store.currentPageIndex {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(Color(hex: "#008369"))
                            }
                        }
                    }
                }
                .onDelete { indices in
                    store.pages.remove(atOffsets: indices)
                    if store.currentPageIndex >= store.pages.count {
                        store.currentPageIndex = max(0, store.pages.count - 1)
                    }
                    store.save()
                }
            }
            .navigationBarTitle("Pages in this Book", displayMode: .inline)
            .navigationBarItems(
                leading: EditButton(),
                trailing: Button("Done") { presentationMode.wrappedValue.dismiss() }
            )
        }
    }
}
