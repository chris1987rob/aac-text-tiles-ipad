import Foundation

public class SymbolCatalog: ObservableObject {
    public static let shared = SymbolCatalog()

    @Published public var allSymbols: [SymbolItem] = []

    private init() {
        loadStarterSymbols()
    }

    private func loadStarterSymbols() {
        allSymbols = [
            SymbolItem(id: "eat", name: "Eat / Food", category: "Food", keywords: ["eat", "food", "snack", "hungry", "meal"], emoji: "🍎"),
            SymbolItem(id: "water", name: "Water / Drink", category: "Drinks", keywords: ["water", "drink", "thirsty", "cup", "juice"], emoji: "💧"),
            SymbolItem(id: "yes", name: "Yes", category: "Core", keywords: ["yes", "yeah", "agree", "ok", "sure"], emoji: "✅"),
            SymbolItem(id: "no", name: "No", category: "Core", keywords: ["no", "stop", "dont", "nope", "never"], emoji: "❌"),
            SymbolItem(id: "help", name: "Help", category: "Core", keywords: ["help", "assist", "support", "please"], emoji: "🙋"),
            SymbolItem(id: "happy", name: "Happy", category: "Feelings", keywords: ["happy", "good", "smile", "glad", "joy"], emoji: "😊"),
            SymbolItem(id: "sad", name: "Sad", category: "Feelings", keywords: ["sad", "unhappy", "cry", "upset"], emoji: "😢"),
            SymbolItem(id: "more", name: "More", category: "Core", keywords: ["more", "again", "extra", "continue"], emoji: "➕"),
            SymbolItem(id: "stop", name: "Stop", category: "Core", keywords: ["stop", "finish", "done", "end"], emoji: "🛑"),
            SymbolItem(id: "home", name: "Home", category: "Places", keywords: ["home", "house", "room"], emoji: "🏠"),
            SymbolItem(id: "school", name: "School", category: "Places", keywords: ["school", "class", "learn"], emoji: "🏫"),
            SymbolItem(id: "bathroom", name: "Bathroom", category: "Daily", keywords: ["bathroom", "toilet", "wash", "potty"], emoji: "🚻"),
            SymbolItem(id: "play", name: "Play / Game", category: "Play", keywords: ["play", "game", "toy", "fun"], emoji: "🧸"),
            SymbolItem(id: "sleep", name: "Sleep / Rest", category: "Daily", keywords: ["sleep", "bed", "tired", "rest", "night"], emoji: "😴"),
            SymbolItem(id: "love", name: "Love / Like", category: "Feelings", keywords: ["love", "like", "favorite", "heart"], emoji: "❤️"),
            SymbolItem(id: "dog", name: "Dog / Puppy", category: "Animals", keywords: ["dog", "puppy", "pet", "bark"], emoji: "🐶"),
            SymbolItem(id: "cat", name: "Cat / Kitten", category: "Animals", keywords: ["cat", "kitten", "meow"], emoji: "🐱"),
            SymbolItem(id: "book", name: "Book / Read", category: "Daily", keywords: ["book", "read", "story"], emoji: "📖"),
            SymbolItem(id: "bus", name: "Bus / Ride", category: "Places", keywords: ["bus", "ride", "drive", "car"], emoji: "🚌"),
            SymbolItem(id: "music", name: "Music / Song", category: "Play", keywords: ["music", "song", "listen", "sing"], emoji: "🎵")
        ]
    }

    public func search(query: String, category: String = "All") -> [SymbolItem] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return allSymbols.filter { sym in
            let matchesCategory = (category == "All" || sym.category.lowercased() == category.lowercased())
            if q.isEmpty { return matchesCategory }
            let matchesName = sym.name.lowercased().contains(q)
            let matchesKeywords = sym.keywords.contains { $0.lowercased().contains(q) }
            return matchesCategory && (matchesName || matchesKeywords)
        }
    }
}
