import SwiftUI

public struct PagesNavigatorModalView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject public var store: AACStore
    @State private var deleteRefused = false

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
                                Text("\(page.type.displayName) • \(page.gridSize) buttons")
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
                    // Never let the book be emptied. A swipe could previously
                    // delete every page, leaving a communication book with
                    // nothing in it and no obvious way back - and a board is
                    // often hours of somebody's work.
                    guard store.pages.count - indices.count >= 1 else {
                        deleteRefused = true
                        return
                    }
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
            .alert(isPresented: $deleteRefused) {
                Alert(title: Text("Keep at least one page"),
                      message: Text("A communication book needs somewhere to put buttons. Add another page before removing this one."),
                      dismissButton: .default(Text("OK")))
            }
        }
        // On iPad a bare NavigationView defaults to the split-view style, so
        // inside a sheet it renders as a sidebar next to an empty detail pane
        // instead of one plain form. Every modal in this app is a single
        // column and must say so.
        .navigationViewStyle(StackNavigationViewStyle())
    }
}
