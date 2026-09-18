import UIKit
import SwiftUI

/// The picture symbols shipped inside the app.
///
/// Mulberry Symbols, CC BY-SA 2.0 UK. Chosen over ARASAAC deliberately:
/// ARASAAC is BY-**NC**-SA, which would forbid ever selling or commercially
/// distributing this app. Mulberry permits commercial use, so the choice does
/// not quietly close a door.
///
/// Mulberry is aimed at adults with language difficulties, so it is strong on
/// concrete nouns and verbs and thin on a child's core words — it has no
/// "yes", "no", "please", "stop", "like" or "love". It sits alongside the emoji
/// picker rather than replacing it.
public enum SymbolLibrary {

    public static let attribution = "Mulberry Symbols © Garry Paxton and Steve Lee, licensed CC BY-SA 2.0 UK."
    public static let licenceURL = "https://creativecommons.org/licenses/by-sa/2.0/uk/"

    private static var cache: [String: UIImage] = [:]

    /// Every bundled symbol name, sorted. Read once from the bundle directory.
    public static let names: [String] = {
        guard let dir = Bundle.main.url(forResource: "Symbols", withExtension: nil),
              let items = try? FileManager.default.contentsOfDirectory(
                    at: dir, includingPropertiesForKeys: nil) else { return [] }
        return items
            .filter { $0.pathExtension.lowercased() == "png" }
            .map { $0.deletingPathExtension().lastPathComponent }
            .sorted()
    }()

    public static var isAvailable: Bool { !names.isEmpty }

    public static func image(named name: String) -> UIImage? {
        if let hit = cache[name] { return hit }
        guard let url = Bundle.main.url(forResource: name, withExtension: "png",
                                        subdirectory: "Symbols"),
              let data = try? Data(contentsOf: url),
              let img = UIImage(data: data) else { return nil }
        cache[name] = img
        return img
    }

    /// Turns `fire_engine_2` into `fire engine 2` for display and searching.
    public static func readable(_ name: String) -> String {
        name.replacingOccurrences(of: "_,_", with: " ")
            .replacingOccurrences(of: "_", with: " ")
            .trimmingCharacters(in: .whitespaces)
    }

    /// Ranked search. Exact and prefix matches come first, because typing
    /// "cat" should not bury the cat under "communicate" and "certificate".
    public static func search(_ query: String, limit: Int = 300) -> [String] {
        let q = query.trimmingCharacters(in: .whitespaces).lowercased()
        guard !q.isEmpty else { return Array(names.prefix(limit)) }

        var exact: [String] = [], prefix: [String] = [], word: [String] = [], loose: [String] = []
        for name in names {
            let r = readable(name).lowercased()
            if r == q { exact.append(name) }
            else if r.hasPrefix(q) { prefix.append(name) }
            else if r.split(separator: " ").contains(where: { $0.hasPrefix(q) }) { word.append(name) }
            else if r.contains(q) { loose.append(name) }
            if exact.count + prefix.count + word.count + loose.count >= limit * 2 { break }
        }
        return Array((exact + prefix + word + loose).prefix(limit))
    }
}

/// Searchable grid of bundled symbols.
public struct SymbolPickerView: View {
    public var onPick: (String) -> Void
    @Environment(\.presentationMode) private var presentation

    public init(onPick: @escaping (String) -> Void) { self.onPick = onPick }

    @State private var query = ""

    private var results: [String] { SymbolLibrary.search(query) }

    private let columns = [GridItem(.adaptive(minimum: 92), spacing: 12)]

    public var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                HStack {
                    Image(systemName: "magnifyingglass").foregroundColor(.secondary)
                    TextField("Search symbols", text: $query)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                    if !query.isEmpty {
                        Button { query = "" } label: {
                            Image(systemName: "xmark.circle.fill").foregroundColor(.secondary)
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                }
                .padding(10)
                .background(Color.secondary.opacity(0.12))
                .cornerRadius(10)
                .padding(.horizontal, 14)
                .padding(.top, 10)

                if !SymbolLibrary.isAvailable {
                    Spacer()
                    Text("No symbols are bundled with this build.")
                        .foregroundColor(.secondary)
                    Spacer()
                } else if results.isEmpty {
                    Spacer()
                    Text("Nothing matches “\(query)”.")
                        .foregroundColor(.secondary)
                    Spacer()
                } else {
                    ScrollView {
                        LazyVGrid(columns: columns, spacing: 12) {
                            ForEach(results, id: \.self) { name in
                                Button {
                                    onPick(name)
                                    presentation.wrappedValue.dismiss()
                                } label: {
                                    VStack(spacing: 4) {
                                        if let img = SymbolLibrary.image(named: name) {
                                            Image(uiImage: img)
                                                .resizable()
                                                .aspectRatio(contentMode: .fit)
                                                .frame(height: 62)
                                        } else {
                                            Color.clear.frame(height: 62)
                                        }
                                        Text(SymbolLibrary.readable(name))
                                            .font(.system(size: 11))
                                            .foregroundColor(.secondary)
                                            .lineLimit(2)
                                            .multilineTextAlignment(.center)
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding(6)
                                    .background(Color.secondary.opacity(0.08))
                                    .cornerRadius(10)
                                }
                                .buttonStyle(PlainButtonStyle())
                                .accessibilityLabel(SymbolLibrary.readable(name))
                            }
                        }
                        .padding(14)

                        Text(SymbolLibrary.attribution)
                            .font(.caption2)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 24)
                            .padding(.bottom, 20)
                    }
                }
            }
            .navigationBarTitle("Symbols", displayMode: .inline)
            .navigationBarItems(trailing: Button("Cancel") { presentation.wrappedValue.dismiss() })
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}
