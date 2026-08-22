import SwiftUI

/// The template gallery. Every card here installs a page with real buttons on
/// it - the previous four cards described their contents and then installed an
/// empty grid.
public struct OnlineGalleryModalView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject public var store: AACStore

    @State private var search: String = ""
    @State private var installed: String? = nil

    public var body: some View {
        NavigationView {
            List {
                ForEach(PageTemplateCatalog.categories, id: \.self) { category in
                    let matches = templates(in: category)
                    if !matches.isEmpty {
                        Section(header: Text(category)) {
                            ForEach(matches) { template in
                                templateCard(template)
                            }
                        }
                    }
                }

                if allMatches.isEmpty {
                    Text("No templates match \"\(search)\"")
                        .foregroundColor(Color(hex: "#64748B"))
                }
            }
            .searchable(text: $search, prompt: "Search templates")
            .navigationBarTitle("Template Gallery", displayMode: .inline)
            .navigationBarItems(trailing: Button("Done") { presentationMode.wrappedValue.dismiss() })
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }

    private var allMatches: [PageTemplate] {
        let q = search.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !q.isEmpty else { return PageTemplateCatalog.all }
        return PageTemplateCatalog.all.filter { t in
            t.title.lowercased().contains(q)
                || t.summary.lowercased().contains(q)
                || t.tiles.contains { $0.label.lowercased().contains(q) }
        }
    }

    private func templates(in category: String) -> [PageTemplate] {
        allMatches.filter { $0.category == category }
    }

    @ViewBuilder
    private func templateCard(_ template: PageTemplate) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(template.title)
                .font(.headline)
                .foregroundColor(Color(hex: "#1E293B"))
            Text(template.summary)
                .font(.subheadline)
                .foregroundColor(Color(hex: "#64748B"))
                .fixedSize(horizontal: false, vertical: true)

            // A glance at the actual words, so a card cannot promise content
            // it does not carry.
            Text(template.tiles.prefix(6).map { $0.label }.joined(separator: " · ")
                 + (template.buttonCount > 6 ? " …" : ""))
                .font(.system(size: 12))
                .foregroundColor(Color(hex: "#94A3B8"))
                .lineLimit(1)

            HStack {
                Text("\(template.buttonCount) buttons")
                    .font(.caption)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Color(hex: "#E2E8F0"))
                    .cornerRadius(6)
                Text("\(template.gridSize)-grid")
                    .font(.caption)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Color(hex: "#E2E8F0"))
                    .cornerRadius(6)
                Spacer()
                Button(action: { install(template) }) {
                    Text(installed == template.id ? "✓ Added" : "Add to Book")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 6)
                        .background(Color(hex: installed == template.id ? "#94A3B8" : "#008369"))
                        .cornerRadius(8)
                }
                .buttonStyle(PlainButtonStyle())
            }
        }
        .padding(.vertical, 6)
    }

    /// Stays open after adding so several boards can be installed in one go.
    private func install(_ template: PageTemplate) {
        store.pages.append(template.makePage())
        store.currentPageIndex = store.pages.count - 1
        store.save()
        withAnimation { installed = template.id }
    }
}
