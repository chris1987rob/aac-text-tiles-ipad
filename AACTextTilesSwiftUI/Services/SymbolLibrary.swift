import UIKit
import SwiftUI

/// The picture symbols shipped inside the app - two sets behind one name.
///
/// **Talk Tiles pictures** (`tt:<id>`, `TalkTilesSymbols/<id>.webp`): our own
/// 2,210 pictures, one per vocabulary word, each with a Bella clip. The
/// default set.
///
/// **Mulberry Symbols** (`<name>`, `Symbols/<name>.png`), CC BY-SA 2.0 UK.
/// Chosen over ARASAAC deliberately: ARASAAC is BY-**NC**-SA, which would
/// forbid ever selling or commercially distributing this app. Mulberry is
/// aimed at adults with language difficulties, so it is strong on concrete
/// nouns and verbs and thin on a child's core words - it has no "yes", "no",
/// "please", "stop", "like" or "love".
///
/// A tile stores one name string (`TileModel.symbolName`). Talk Tiles names
/// carry the `tt:` prefix so "cat" (Mulberry) and "tt:cat" (ours) never
/// collide, and every board keeps working whichever set is picked in Settings.
public enum SymbolLibrary {

    public static let attribution = "Mulberry Symbols © Garry Paxton and Steve Lee, licensed CC BY-SA 2.0 UK."
    public static let licenceURL = "https://creativecommons.org/licenses/by-sa/2.0/uk/"

    /// Prefix on a `symbolName` that means "one of our own pictures".
    public static let talkTilesPrefix = "tt:"

    private static var cache: [String: UIImage] = [:]

    // MARK: Mulberry

    /// Every bundled Mulberry symbol name, sorted. Read once from the bundle directory.
    public static let names: [String] = {
        guard let dir = Bundle.main.url(forResource: "Symbols", withExtension: nil),
              let items = try? FileManager.default.contentsOfDirectory(
                    at: dir, includingPropertiesForKeys: nil) else { return [] }
        return items
            .filter { $0.pathExtension.lowercased() == "png" }
            .map { $0.deletingPathExtension().lastPathComponent }
            .sorted()
    }()

    /// True when the Mulberry set is in this build.
    public static var isAvailable: Bool { !names.isEmpty }

    // MARK: Sets

    public static func has(_ set: SymbolSet) -> Bool {
        switch set {
        case .talkTiles: return TalkTilesCatalog.isAvailable
        case .mulberry:  return isAvailable
        }
    }

    public static func count(of set: SymbolSet) -> Int {
        switch set {
        case .talkTiles: return TalkTilesCatalog.symbols.count
        case .mulberry:  return names.count
        }
    }

    /// True when at least one set made it into the build.
    public static var anyAvailable: Bool { SymbolSet.allCases.contains { has($0) } }

    /// The set a stored symbol name belongs to.
    public static func symbolSet(of name: String) -> SymbolSet {
        name.hasPrefix(talkTilesPrefix) ? .talkTiles : .mulberry
    }

    /// The stored name for one of our pictures.
    public static func talkTilesName(_ id: String) -> String { talkTilesPrefix + id }

    /// The catalogue entry behind a `tt:` name, nil for a Mulberry name.
    public static func talkTilesSymbol(named name: String) -> TalkTilesSymbol? {
        guard name.hasPrefix(talkTilesPrefix) else { return nil }
        return TalkTilesCatalog.byId[String(name.dropFirst(talkTilesPrefix.count))]
    }

    // MARK: Pictures

    public static func image(named name: String) -> UIImage? {
        if let hit = cache[name] { return hit }
        let url: URL?
        if name.hasPrefix(talkTilesPrefix) {
            url = Bundle.main.url(forResource: String(name.dropFirst(talkTilesPrefix.count)),
                                  withExtension: "webp", subdirectory: TalkTilesCatalog.folder)
        } else {
            url = Bundle.main.url(forResource: name, withExtension: "png", subdirectory: "Symbols")
        }
        guard let u = url, let data = try? Data(contentsOf: u),
              let img = UIImage(data: data) else { return nil }
        cache[name] = img
        return img
    }

    /// What to show under a picture: the catalogue label for one of ours,
    /// and `fire_engine_2` -> `fire engine 2` for a Mulberry name.
    public static func readable(_ name: String) -> String {
        if let sym = talkTilesSymbol(named: name) { return sym.label }
        return name.replacingOccurrences(of: "_,_", with: " ")
            .replacingOccurrences(of: "_", with: " ")
            .trimmingCharacters(in: .whitespaces)
    }

    // MARK: Search

    /// Ranked search over one set. Exact and prefix matches come first,
    /// because typing "cat" should not bury the cat under "communicate" and
    /// "certificate". Results are stored names (`tt:` prefixed for ours).
    public static func search(_ query: String, in set: SymbolSet,
                              category: String? = nil, limit: Int = 300) -> [String] {
        switch set {
        case .talkTiles:
            return TalkTilesCatalog.search(query, category: category, limit: limit).map { talkTilesName($0.id) }
        case .mulberry:
            return search(query, limit: limit)
        }
    }

    /// Mulberry-only search, kept for callers that predate the second set.
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
///
/// Opens on the set chosen in Settings; the segmented control at the top
/// switches between the two for this one pick without changing the setting.
public struct SymbolPickerView: View {
    public var onPick: (String) -> Void
    @Environment(\.presentationMode) private var presentation

    public init(set: SymbolSet = .talkTiles, onPick: @escaping (String) -> Void) {
        self.onPick = onPick
        _activeSet = State(initialValue: SymbolLibrary.has(set)
                           ? set
                           : (SymbolSet.allCases.first { SymbolLibrary.has($0) } ?? set))
    }

    @State private var activeSet: SymbolSet
    @State private var query = ""
    @State private var category: String? = nil

    private var results: [String] {
        SymbolLibrary.search(query, in: activeSet, category: activeSet == .talkTiles ? category : nil)
    }

    private let columns = [GridItem(.adaptive(minimum: 92), spacing: 12)]

    /// Both sets present: offer the switch. Only one: no control to confuse.
    private var setsInBuild: [SymbolSet] { SymbolSet.allCases.filter { SymbolLibrary.has($0) } }

    public var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                if setsInBuild.count > 1 {
                    Picker("Picture set", selection: $activeSet) {
                        ForEach(setsInBuild) { s in
                            Text("\(s.title) (\(SymbolLibrary.count(of: s)))").tag(s)
                        }
                    }
                    .pickerStyle(SegmentedPickerStyle())
                    .padding(.horizontal, 14)
                    .padding(.top, 10)
                }

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

                if activeSet == .talkTiles && !TalkTilesCatalog.categories.isEmpty {
                    categoryChips
                }

                if !SymbolLibrary.anyAvailable {
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

                        Text(activeSet == .mulberry
                             ? SymbolLibrary.attribution
                             : "Talk Tiles pictures are drawn in-house. Every one has Bella's voice behind it.")
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

    /// One row of category chips. "All" plus the catalogue's sixteen groups;
    /// a search box alone is no use to someone who does not know the word yet.
    @ViewBuilder
    private var categoryChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                categoryChip(title: "All", value: nil)
                ForEach(TalkTilesCatalog.categories, id: \.self) { c in
                    categoryChip(title: TalkTilesCatalog.categoryTitle(c), value: c)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
        }
    }

    private func categoryChip(title: String, value: String?) -> some View {
        let on = category == value
        return Button { category = value } label: {
            Text(title)
                .font(.system(size: 13, weight: on ? .semibold : .regular))
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(on ? Color(hex: "#008369") : Color.secondary.opacity(0.12))
                .foregroundColor(on ? .white : .primary)
                .cornerRadius(14)
        }
        .buttonStyle(PlainButtonStyle())
    }
}
