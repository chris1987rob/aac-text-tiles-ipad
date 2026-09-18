import Foundation

/// One button inside a template. Every tile carries a picture - a button that
/// is only a word is unusable by a child who is not yet reading.
public struct TemplateTile {
    public let label: String
    public let tts: String
    public let symbol: String?
    public let color: String

    public init(_ label: String, _ tts: String, _ symbol: String? = nil, _ color: String = TemplateColor.noun) {
        self.label = label
        self.tts = tts
        self.symbol = symbol
        self.color = color
    }
}

/// Fitzgerald Key colours - the coding used across mainstream AAC systems, so a
/// child moving between devices or a paper board finds words where they expect.
public enum TemplateColor {
    public static let person     = "#FFF9C4" // yellow  - people, pronouns
    public static let verb       = "#C8E6C9" // green   - actions
    public static let descriptor = "#BBDEFB" // blue    - describing words
    public static let noun       = "#FFE0B2" // orange  - things
    public static let social     = "#F8BBD0" // pink    - social words
    public static let question   = "#D1C4E9" // purple  - questions
    public static let negative   = "#FFCDD2" // red     - stop, no, hurt
    public static let plain      = "#FFFFFF"
}

public struct PageTemplate: Identifiable {
    public let id: String
    public let title: String
    public let summary: String
    public let category: String
    public let icon: String
    public let accent: String
    public let gridSize: Int
    public let tiles: [TemplateTile]

    public var buttonCount: Int { tiles.count }

    public func makePage() -> PageModel {
        var page = PageModel(title: title, type: .grid, gridSize: gridSize)
        for (index, t) in tiles.enumerated() {
            let slot = index + 1
            page.tiles[slot] = TileModel(
                id: slot, label: t.label, tts: t.tts, symbolName: t.symbol,
                bgHex: t.color, borderHex: "#CBD5E1", labelHex: "#1E293B"
            )
        }
        return page
    }
}

public enum PageTemplateCatalog {

    public static let categories = ["Essential", "Everyday", "Feelings & Self", "Out & About", "Fun & Silly"]

    /// The five that go in the book by default - the boards that get used most.
    public static let popularIDs = ["core", "yesno", "feelings", "food", "help"]

    public static var popular: [PageTemplate] { popularIDs.compactMap { id in all.first { $0.id == id } } }
    public static var extras: [PageTemplate] { all.filter { !popularIDs.contains($0.id) } }

    public static func templates(in category: String) -> [PageTemplate] {
        all.filter { $0.category == category }
    }

    public static let all: [PageTemplate] = essential + everyday + feelingsSelf + outAbout + funSilly

    // MARK: - Essential

    static let essential: [PageTemplate] = [
        PageTemplate(id: "core", title: "Core Words", summary: "The words that carry most of everyday speech",
            category: "Essential", icon: "⭐", accent: "#F59E0B", gridSize: 25, tiles: [
            TemplateTile("I", "I", "🙋", TemplateColor.person),
            TemplateTile("you", "you", "👉", TemplateColor.person),
            TemplateTile("it", "it", "👌", TemplateColor.person),
            TemplateTile("my", "my", "🫵", TemplateColor.person),
            TemplateTile("want", "I want", "🤲", TemplateColor.verb),
            TemplateTile("go", "go", "🏃", TemplateColor.verb),
            TemplateTile("stop", "stop", "🛑", TemplateColor.negative),
            TemplateTile("more", "more", "➕", TemplateColor.verb),
            TemplateTile("help", "help me", "🙋", TemplateColor.verb),
            TemplateTile("like", "I like it", "❤️", TemplateColor.verb),
            TemplateTile("look", "look", "👀", TemplateColor.verb),
            TemplateTile("make", "make", "🔨", TemplateColor.verb),
            TemplateTile("do", "do", "✋", TemplateColor.verb),
            TemplateTile("put", "put", "📥", TemplateColor.verb),
            TemplateTile("turn", "turn", "🔄", TemplateColor.verb),
            TemplateTile("yes", "yes", "✅", TemplateColor.social),
            TemplateTile("no", "no", "❌", TemplateColor.negative),
            TemplateTile("all done", "I am all done", "🏁", TemplateColor.social),
            TemplateTile("again", "again", "🔁", TemplateColor.verb),
            TemplateTile("big", "big", "🐘", TemplateColor.descriptor),
            TemplateTile("little", "little", "🐜", TemplateColor.descriptor),
            TemplateTile("here", "here", "📍", TemplateColor.descriptor),
            TemplateTile("there", "there", "🗺️", TemplateColor.descriptor),
            TemplateTile("in", "in", "📦", TemplateColor.descriptor),
            TemplateTile("out", "out", "🚪", TemplateColor.descriptor)
        ]),

        PageTemplate(id: "yesno", title: "Yes / No", summary: "A first board - big targets for answering",
            category: "Essential", icon: "✅", accent: "#16A34A", gridSize: 4, tiles: [
            TemplateTile("Yes", "Yes", "✅", TemplateColor.social),
            TemplateTile("No", "No", "❌", TemplateColor.negative),
            TemplateTile("Maybe", "Maybe", "🤷", TemplateColor.descriptor),
            TemplateTile("I don't know", "I don't know", "❓", TemplateColor.question)
        ]),

        // Each tile is tinted the colour it names, so the board teaches the
        // word by looking like it - the one template where the Fitzgerald
        // coding gives way to the subject itself.
        PageTemplate(id: "colors", title: "Colors", summary: "Name and choose colours - each button is its own colour",
            category: "Essential", icon: "🎨", accent: "#E11D48", gridSize: 9, tiles: [
            TemplateTile("Red", "Red", "🔴", "#FFB4B4"),
            TemplateTile("Orange", "Orange", "🟠", "#FFD1A3"),
            TemplateTile("Yellow", "Yellow", "🟡", "#FFF3A3"),
            TemplateTile("Green", "Green", "🟢", "#BDEBC8"),
            TemplateTile("Blue", "Blue", "🔵", "#B9DDFF"),
            TemplateTile("Purple", "Purple", "🟣", "#DCC9FF"),
            TemplateTile("Pink", "Pink", "🌸", "#FFC9E3"),
            TemplateTile("Brown", "Brown", "🟤", "#E2C9B0"),
            TemplateTile("Black", "Black", "⚫", "#D6D9E0")
        ]),

        PageTemplate(id: "help", title: "Help & Requests", summary: "Ask for help, a break, or attention",
            category: "Essential", icon: "🙋", accent: "#0284C7", gridSize: 9, tiles: [
            TemplateTile("Help me", "Help me please", "🙋", TemplateColor.verb),
            TemplateTile("Break", "I need a break", "⏸️", TemplateColor.verb),
            TemplateTile("Wait", "Wait please", "✋", TemplateColor.verb),
            TemplateTile("Come here", "Come here please", "👋", TemplateColor.verb),
            TemplateTile("I want that", "I want that", "👉", TemplateColor.verb),
            TemplateTile("Not that", "Not that one", "🙅", TemplateColor.negative),
            TemplateTile("More please", "More please", "➕", TemplateColor.verb),
            TemplateTile("All done", "I am all done", "🏁", TemplateColor.social),
            TemplateTile("Thank you", "Thank you", "🙏", TemplateColor.social)
        ]),

        PageTemplate(id: "chat", title: "Let's Chat", summary: "Start a conversation, not just a request",
            category: "Essential", icon: "💬", accent: "#7C3AED", gridSize: 12, tiles: [
            TemplateTile("Hi!", "Hi!", "👋", TemplateColor.social),
            TemplateTile("Guess what", "Guess what!", "🤩", TemplateColor.social),
            TemplateTile("Look at this", "Look at this!", "👀", TemplateColor.verb),
            TemplateTile("What's that?", "What is that?", "❓", TemplateColor.question),
            TemplateTile("Me too", "Me too!", "🙌", TemplateColor.social),
            TemplateTile("That's funny", "That is funny", "😂", TemplateColor.descriptor),
            TemplateTile("Cool!", "That is so cool", "😎", TemplateColor.descriptor),
            TemplateTile("Tell me more", "Tell me more", "👂", TemplateColor.question),
            TemplateTile("Your turn", "Your turn", "🔄", TemplateColor.social),
            TemplateTile("I'm listening", "I am listening", "👂", TemplateColor.social),
            TemplateTile("No thanks", "No thank you", "🙅", TemplateColor.negative),
            TemplateTile("Bye!", "Goodbye!", "👋", TemplateColor.social)
        ])
    ]

    // MARK: - Everyday

    static let everyday: [PageTemplate] = [
        PageTemplate(id: "food", title: "Food & Snacks", summary: "Mealtimes and snack choices",
            category: "Everyday", icon: "🍎", accent: "#DC2626", gridSize: 16, tiles: [
            TemplateTile("Hungry", "I am hungry", "😋", TemplateColor.verb),
            TemplateTile("Eat", "I want to eat", "🍽️", TemplateColor.verb),
            TemplateTile("More", "More please", "➕", TemplateColor.verb),
            TemplateTile("All done", "I am all done", "🏁", TemplateColor.social),
            TemplateTile("Apple", "Apple", "🍎", TemplateColor.noun),
            TemplateTile("Banana", "Banana", "🍌", TemplateColor.noun),
            TemplateTile("Crackers", "Crackers", "🍘", TemplateColor.noun),
            TemplateTile("Cookie", "Cookie", "🍪", TemplateColor.noun),
            TemplateTile("Sandwich", "Sandwich", "🥪", TemplateColor.noun),
            TemplateTile("Pizza", "Pizza", "🍕", TemplateColor.noun),
            TemplateTile("Pasta", "Pasta", "🍝", TemplateColor.noun),
            TemplateTile("Cereal", "Cereal", "🥣", TemplateColor.noun),
            TemplateTile("Yogurt", "Yogurt", "🥛", TemplateColor.noun),
            TemplateTile("Cheese", "Cheese", "🧀", TemplateColor.noun),
            TemplateTile("Yuck", "I don't like it", "🤢", TemplateColor.negative),
            TemplateTile("Yummy", "That is yummy", "😋", TemplateColor.descriptor)
        ]),

        PageTemplate(id: "drinks", title: "Drinks", summary: "Thirsty, and what to ask for",
            category: "Everyday", icon: "🥤", accent: "#0891B2", gridSize: 9, tiles: [
            TemplateTile("Thirsty", "I am thirsty", "😰", TemplateColor.verb),
            TemplateTile("Water", "Water please", "💧", TemplateColor.noun),
            TemplateTile("Milk", "Milk please", "🥛", TemplateColor.noun),
            TemplateTile("Juice", "Juice please", "🧃", TemplateColor.noun),
            TemplateTile("Hot cocoa", "Hot chocolate please", "☕", TemplateColor.noun),
            TemplateTile("Smoothie", "A smoothie please", "🥤", TemplateColor.noun),
            TemplateTile("Cup", "I need a cup", "🥛", TemplateColor.noun),
            TemplateTile("Straw", "I need a straw", "🥤", TemplateColor.noun),
            TemplateTile("More", "More please", "➕", TemplateColor.verb)
        ]),

        PageTemplate(id: "bathroom", title: "Bathroom", summary: "Private needs, said fast",
            category: "Everyday", icon: "🚻", accent: "#6366F1", gridSize: 9, tiles: [
            TemplateTile("Bathroom", "I need the bathroom", "🚻", TemplateColor.verb),
            TemplateTile("Now!", "I need to go now", "🏃", TemplateColor.negative),
            TemplateTile("Wash hands", "I need to wash my hands", "🧼", TemplateColor.verb),
            TemplateTile("Toilet paper", "I need toilet paper", "🧻", TemplateColor.noun),
            TemplateTile("Help me", "I need help please", "🙋", TemplateColor.verb),
            TemplateTile("All done", "I am all done", "🏁", TemplateColor.social),
            TemplateTile("Privacy", "I would like privacy please", "🚪", TemplateColor.social),
            TemplateTile("Accident", "I had an accident", "😞", TemplateColor.negative),
            TemplateTile("Change", "I need changing please", "🔄", TemplateColor.negative)
        ]),

        PageTemplate(id: "morning", title: "Getting Ready", summary: "The morning routine, in order",
            category: "Everyday", icon: "🪥", accent: "#0EA5E9", gridSize: 12, tiles: [
            TemplateTile("Wake up", "Time to wake up", "🌅", TemplateColor.verb),
            TemplateTile("5 more minutes", "Five more minutes please", "😴", TemplateColor.negative),
            TemplateTile("Bathroom", "Bathroom first", "🚻", TemplateColor.verb),
            TemplateTile("Brush teeth", "Brush my teeth", "🪥", TemplateColor.verb),
            TemplateTile("Wash face", "Wash my face", "🧼", TemplateColor.verb),
            TemplateTile("Brush hair", "Brush my hair", "💇", TemplateColor.verb),
            TemplateTile("Shirt", "Shirt", "👕", TemplateColor.noun),
            TemplateTile("Pants", "Pants", "👖", TemplateColor.noun),
            TemplateTile("Socks", "Socks", "🧦", TemplateColor.noun),
            TemplateTile("Shoes", "Shoes", "👟", TemplateColor.noun),
            TemplateTile("Coat", "Coat", "🧥", TemplateColor.noun),
            TemplateTile("I can do it", "I can do it myself", "💪", TemplateColor.social)
        ]),

        PageTemplate(id: "bedtime", title: "Bedtime", summary: "The wind-down routine",
            category: "Everyday", icon: "🌙", accent: "#4F46E5", gridSize: 9, tiles: [
            TemplateTile("Tired", "I am tired", "😴", TemplateColor.descriptor),
            TemplateTile("Bath", "I want a bath", "🛁", TemplateColor.verb),
            TemplateTile("Teeth", "Brush my teeth", "🪥", TemplateColor.verb),
            TemplateTile("Story", "Read me a story", "📖", TemplateColor.verb),
            TemplateTile("Song", "Sing me a song", "🎵", TemplateColor.verb),
            TemplateTile("Cuddle", "I want a cuddle", "🤗", TemplateColor.social),
            TemplateTile("Light on", "Leave the light on", "💡", TemplateColor.descriptor),
            TemplateTile("Not tired", "I am not tired yet", "🙅", TemplateColor.negative),
            TemplateTile("Goodnight", "Goodnight, I love you", "🌙", TemplateColor.social)
        ]),

        PageTemplate(id: "myday", title: "My Day", summary: "What happens next - eases transitions",
            category: "Everyday", icon: "📅", accent: "#059669", gridSize: 12, tiles: [
            TemplateTile("First", "First", "1️⃣", TemplateColor.descriptor),
            TemplateTile("Next", "Next", "2️⃣", TemplateColor.descriptor),
            TemplateTile("Last", "Last", "3️⃣", TemplateColor.descriptor),
            TemplateTile("Wake up", "Wake up", "🌅", TemplateColor.verb),
            TemplateTile("Breakfast", "Breakfast", "🥣", TemplateColor.noun),
            TemplateTile("School", "School", "🏫", TemplateColor.noun),
            TemplateTile("Lunch", "Lunch", "🥪", TemplateColor.noun),
            TemplateTile("Play", "Play time", "🧸", TemplateColor.noun),
            TemplateTile("Dinner", "Dinner", "🍽️", TemplateColor.noun),
            TemplateTile("Bath", "Bath time", "🛁", TemplateColor.noun),
            TemplateTile("Bed", "Bed time", "🛏️", TemplateColor.noun),
            TemplateTile("What's next?", "What is next?", "❓", TemplateColor.question)
        ])
    ]

    // MARK: - Feelings & Self

    static let feelingsSelf: [PageTemplate] = [
        PageTemplate(id: "feelings", title: "Feelings", summary: "Name it before it becomes behaviour",
            category: "Feelings & Self", icon: "😊", accent: "#EAB308", gridSize: 12, tiles: [
            TemplateTile("Happy", "I feel happy", "😊", TemplateColor.descriptor),
            TemplateTile("Sad", "I feel sad", "😢", TemplateColor.descriptor),
            TemplateTile("Mad", "I feel mad", "😠", TemplateColor.negative),
            TemplateTile("Scared", "I feel scared", "😨", TemplateColor.descriptor),
            TemplateTile("Tired", "I feel tired", "😴", TemplateColor.descriptor),
            TemplateTile("Excited", "I feel excited", "🤩", TemplateColor.descriptor),
            TemplateTile("Silly", "I feel silly", "🤪", TemplateColor.descriptor),
            TemplateTile("Calm", "I feel calm", "😌", TemplateColor.descriptor),
            TemplateTile("Sick", "I feel sick", "🤒", TemplateColor.negative),
            TemplateTile("Hurt", "Something hurts", "🤕", TemplateColor.negative),
            TemplateTile("Love you", "I love you", "❤️", TemplateColor.social),
            TemplateTile("Proud", "I feel proud of me", "🏆", TemplateColor.descriptor)
        ]),

        PageTemplate(id: "angry", title: "When I'm Angry", summary: "A way out of a meltdown that isn't shouting",
            category: "Feelings & Self", icon: "😤", accent: "#DC2626", gridSize: 9, tiles: [
            TemplateTile("I'm angry", "I am angry right now", "😠", TemplateColor.negative),
            TemplateTile("Leave me", "Please leave me alone", "🚫", TemplateColor.negative),
            TemplateTile("Too much", "This is too much", "🥵", TemplateColor.negative),
            TemplateTile("Not fair", "That is not fair", "⚖️", TemplateColor.negative),
            TemplateTile("I need space", "I need space", "↔️", TemplateColor.verb),
            TemplateTile("Breathe", "Help me breathe", "🌬️", TemplateColor.verb),
            TemplateTile("Squeeze", "I want a tight squeeze", "🤗", TemplateColor.verb),
            TemplateTile("Talk later", "I will talk about it later", "⏰", TemplateColor.social),
            TemplateTile("I'm okay now", "I am okay now", "😌", TemplateColor.social)
        ]),

        PageTemplate(id: "sensory", title: "Sensory", summary: "Say what the body needs before it overflows",
            category: "Feelings & Self", icon: "🎧", accent: "#8B5CF6", gridSize: 9, tiles: [
            TemplateTile("Too loud", "It is too loud", "🔊", TemplateColor.negative),
            TemplateTile("Too bright", "It is too bright", "🔆", TemplateColor.negative),
            TemplateTile("Too busy", "It is too busy here", "😵", TemplateColor.negative),
            TemplateTile("Itchy", "My clothes are itchy", "🧥", TemplateColor.negative),
            TemplateTile("Headphones", "I want my headphones", "🎧", TemplateColor.noun),
            TemplateTile("Quiet place", "I need a quiet place", "🤫", TemplateColor.verb),
            TemplateTile("Blanket", "I want my blanket", "🧣", TemplateColor.noun),
            TemplateTile("Rock", "I want to rock", "🪑", TemplateColor.verb),
            TemplateTile("Better now", "I feel better now", "😌", TemplateColor.social)
        ]),

        PageTemplate(id: "medical", title: "Pain & Doctor", summary: "Where it hurts and how much",
            category: "Feelings & Self", icon: "🩺", accent: "#E11D48", gridSize: 12, tiles: [
            TemplateTile("It hurts", "Something hurts", "🤕", TemplateColor.negative),
            TemplateTile("A little", "It hurts a little", "🙂", TemplateColor.descriptor),
            TemplateTile("A lot", "It hurts a lot", "😫", TemplateColor.negative),
            TemplateTile("Head", "My head hurts", "🤯", TemplateColor.noun),
            TemplateTile("Tummy", "My tummy hurts", "🤢", TemplateColor.noun),
            TemplateTile("Ear", "My ear hurts", "👂", TemplateColor.noun),
            TemplateTile("Throat", "My throat hurts", "😷", TemplateColor.noun),
            TemplateTile("Tooth", "My tooth hurts", "🦷", TemplateColor.noun),
            TemplateTile("Dizzy", "I feel dizzy", "💫", TemplateColor.negative),
            TemplateTile("Medicine", "I need my medicine", "💊", TemplateColor.noun),
            TemplateTile("I'm scared", "I am scared", "😨", TemplateColor.negative),
            TemplateTile("Get my mom", "Please get my mom", "👩", TemplateColor.social)
        ]),

        PageTemplate(id: "favorites", title: "My Favourites", summary: "The things that are mine - identity, not requests",
            category: "Feelings & Self", icon: "⭐", accent: "#F59E0B", gridSize: 12, tiles: [
            TemplateTile("My favourite", "My favourite is", "⭐", TemplateColor.descriptor),
            TemplateTile("Colour", "My favourite colour", "🌈", TemplateColor.noun),
            TemplateTile("Food", "My favourite food", "🍕", TemplateColor.noun),
            TemplateTile("Animal", "My favourite animal", "🐶", TemplateColor.noun),
            TemplateTile("Show", "My favourite show", "📺", TemplateColor.noun),
            TemplateTile("Song", "My favourite song", "🎵", TemplateColor.noun),
            TemplateTile("Place", "My favourite place", "🏖️", TemplateColor.noun),
            TemplateTile("Person", "My favourite person", "🧑", TemplateColor.person),
            TemplateTile("Toy", "My favourite toy", "🧸", TemplateColor.noun),
            TemplateTile("I love it", "I love it so much", "❤️", TemplateColor.descriptor),
            TemplateTile("Not my favourite", "That is not my favourite", "🙅", TemplateColor.negative),
            TemplateTile("What's yours?", "What is your favourite?", "❓", TemplateColor.question)
        ]),

        PageTemplate(id: "people", title: "My People", summary: "Family and the people around every day",
            category: "Feelings & Self", icon: "👨‍👩‍👧", accent: "#D97706", gridSize: 12, tiles: [
            TemplateTile("Mom", "Mom", "👩", TemplateColor.person),
            TemplateTile("Dad", "Dad", "👨", TemplateColor.person),
            TemplateTile("Grandma", "Grandma", "👵", TemplateColor.person),
            TemplateTile("Grandpa", "Grandpa", "👴", TemplateColor.person),
            TemplateTile("Brother", "My brother", "👦", TemplateColor.person),
            TemplateTile("Sister", "My sister", "👧", TemplateColor.person),
            TemplateTile("Me", "Me", "🙋", TemplateColor.person),
            TemplateTile("Friend", "My friend", "🧑‍🤝‍🧑", TemplateColor.person),
            TemplateTile("Teacher", "My teacher", "👩‍🏫", TemplateColor.person),
            TemplateTile("Doctor", "The doctor", "🩺", TemplateColor.person),
            TemplateTile("Where is?", "Where is", "❓", TemplateColor.question),
            TemplateTile("I miss you", "I miss you", "🥺", TemplateColor.social)
        ])
    ]

    // MARK: - Out & About

    static let outAbout: [PageTemplate] = [
        PageTemplate(id: "places", title: "Places We Go", summary: "Out in the community",
            category: "Out & About", icon: "🗺️", accent: "#0D9488", gridSize: 12, tiles: [
            TemplateTile("Home", "I want to go home", "🏠", TemplateColor.noun),
            TemplateTile("School", "School", "🏫", TemplateColor.noun),
            TemplateTile("Park", "The park", "🛝", TemplateColor.noun),
            TemplateTile("Store", "The store", "🏪", TemplateColor.noun),
            TemplateTile("Car", "In the car", "🚗", TemplateColor.noun),
            TemplateTile("Bus", "The bus", "🚌", TemplateColor.noun),
            TemplateTile("Grandma's", "Grandma's house", "🏡", TemplateColor.noun),
            TemplateTile("Swimming", "Swimming", "🏊", TemplateColor.noun),
            TemplateTile("Library", "The library", "📚", TemplateColor.noun),
            TemplateTile("Where?", "Where are we going?", "❓", TemplateColor.question),
            TemplateTile("Let's go", "Let's go", "🚶", TemplateColor.verb),
            TemplateTile("Stay", "I want to stay", "🛑", TemplateColor.negative)
        ]),

        PageTemplate(id: "shop", title: "At the Store", summary: "Shopping without a meltdown in aisle five",
            category: "Out & About", icon: "🛒", accent: "#65A30D", gridSize: 12, tiles: [
            TemplateTile("Cart", "I want to push the cart", "🛒", TemplateColor.noun),
            TemplateTile("I want this", "I want this one", "👉", TemplateColor.verb),
            TemplateTile("How much?", "How much is it?", "💲", TemplateColor.question),
            TemplateTile("Too many people", "There are too many people", "😵", TemplateColor.negative),
            TemplateTile("Snacks", "Can we get snacks?", "🍪", TemplateColor.noun),
            TemplateTile("Bananas", "Bananas", "🍌", TemplateColor.noun),
            TemplateTile("Bread", "Bread", "🍞", TemplateColor.noun),
            TemplateTile("Milk", "Milk", "🥛", TemplateColor.noun),
            TemplateTile("Toys", "Can we look at toys?", "🧸", TemplateColor.noun),
            TemplateTile("Carry me", "Please carry me", "🤱", TemplateColor.verb),
            TemplateTile("Are we done?", "Are we done yet?", "❓", TemplateColor.question),
            TemplateTile("Let's go home", "Let's go home", "🏠", TemplateColor.verb)
        ]),

        PageTemplate(id: "restaurant", title: "Eating Out", summary: "Ordering for yourself at a restaurant",
            category: "Out & About", icon: "🍽️", accent: "#B45309", gridSize: 12, tiles: [
            TemplateTile("Table please", "A table please", "🪑", TemplateColor.social),
            TemplateTile("I'm ready", "I am ready to order", "✋", TemplateColor.social),
            TemplateTile("I want", "I would like", "🤲", TemplateColor.verb),
            TemplateTile("Pizza", "Pizza please", "🍕", TemplateColor.noun),
            TemplateTile("Burger", "A burger please", "🍔", TemplateColor.noun),
            TemplateTile("Fries", "Fries please", "🍟", TemplateColor.noun),
            TemplateTile("Chicken", "Chicken please", "🍗", TemplateColor.noun),
            TemplateTile("Water", "Water please", "💧", TemplateColor.noun),
            TemplateTile("Napkin", "A napkin please", "🧻", TemplateColor.noun),
            TemplateTile("Too hot", "This is too hot", "🥵", TemplateColor.negative),
            TemplateTile("It's good", "This is really good", "😋", TemplateColor.descriptor),
            TemplateTile("All finished", "I am all finished", "🏁", TemplateColor.social)
        ]),

        PageTemplate(id: "car", title: "Car Rides", summary: "The long-journey board",
            category: "Out & About", icon: "🚗", accent: "#475569", gridSize: 9, tiles: [
            TemplateTile("Are we there?", "Are we there yet?", "❓", TemplateColor.question),
            TemplateTile("How long?", "How much longer?", "⏰", TemplateColor.question),
            TemplateTile("Music", "Put on music please", "🎵", TemplateColor.verb),
            TemplateTile("Too loud", "It is too loud", "🔊", TemplateColor.negative),
            TemplateTile("Window", "Open the window", "🪟", TemplateColor.verb),
            TemplateTile("Car sick", "I feel car sick", "🤢", TemplateColor.negative),
            TemplateTile("Bathroom", "I need the bathroom", "🚻", TemplateColor.negative),
            TemplateTile("Snack", "Can I have a snack?", "🍪", TemplateColor.noun),
            TemplateTile("Look!", "Look out there!", "👀", TemplateColor.verb)
        ]),

        PageTemplate(id: "school", title: "School", summary: "Classroom routine and self-advocacy",
            category: "Out & About", icon: "🏫", accent: "#2563EB", gridSize: 16, tiles: [
            TemplateTile("I'm here", "I am here", "🙋", TemplateColor.social),
            TemplateTile("I know!", "I know the answer", "💡", TemplateColor.verb),
            TemplateTile("Help please", "I need help please", "🙋", TemplateColor.verb),
            TemplateTile("Bathroom", "May I go to the bathroom?", "🚻", TemplateColor.verb),
            TemplateTile("Pencil", "I need a pencil", "✏️", TemplateColor.noun),
            TemplateTile("Paper", "I need paper", "📄", TemplateColor.noun),
            TemplateTile("Book", "My book", "📖", TemplateColor.noun),
            TemplateTile("Backpack", "My backpack", "🎒", TemplateColor.noun),
            TemplateTile("Snack", "Snack time", "🍎", TemplateColor.noun),
            TemplateTile("Recess", "Recess", "🛝", TemplateColor.noun),
            TemplateTile("Finished", "I am finished", "🏁", TemplateColor.social),
            TemplateTile("Too hard", "This is too hard", "😖", TemplateColor.negative),
            TemplateTile("Say again", "Please say it again", "🔁", TemplateColor.question),
            TemplateTile("My turn", "It is my turn", "🔄", TemplateColor.social),
            TemplateTile("Sit with me", "Will you sit with me?", "🧑‍🤝‍🧑", TemplateColor.social),
            TemplateTile("Go home", "Time to go home", "🏠", TemplateColor.verb)
        ]),

        PageTemplate(id: "outside", title: "Outside & Weather", summary: "Playground, garden, and what the sky is doing",
            category: "Out & About", icon: "🌤️", accent: "#0284C7", gridSize: 16, tiles: [
            TemplateTile("Outside", "I want to go outside", "🌳", TemplateColor.verb),
            TemplateTile("Sunny", "It is sunny", "☀️", TemplateColor.descriptor),
            TemplateTile("Rainy", "It is raining", "🌧️", TemplateColor.descriptor),
            TemplateTile("Snowy", "It is snowing", "❄️", TemplateColor.descriptor),
            TemplateTile("Windy", "It is windy", "💨", TemplateColor.descriptor),
            TemplateTile("Hot", "I am hot", "🥵", TemplateColor.descriptor),
            TemplateTile("Cold", "I am cold", "🥶", TemplateColor.descriptor),
            TemplateTile("Swing", "I want to swing", "🛝", TemplateColor.verb),
            TemplateTile("Slide", "I want the slide", "🛝", TemplateColor.verb),
            TemplateTile("Run", "I want to run", "🏃", TemplateColor.verb),
            TemplateTile("Puddle", "Can I jump in the puddle?", "💦", TemplateColor.verb),
            TemplateTile("Bugs", "Look, a bug!", "🐛", TemplateColor.noun),
            TemplateTile("Flowers", "Flowers", "🌸", TemplateColor.noun),
            TemplateTile("Coat", "I need my coat", "🧥", TemplateColor.noun),
            TemplateTile("Five more minutes", "Five more minutes please", "⏰", TemplateColor.negative),
            TemplateTile("Go inside", "I want to go inside", "🏠", TemplateColor.verb)
        ])
    ]

    // MARK: - Fun & Silly

    static let funSilly: [PageTemplate] = [
        PageTemplate(id: "play", title: "Play & Toys", summary: "Choosing an activity, and playing with someone",
            category: "Fun & Silly", icon: "🧸", accent: "#DB2777", gridSize: 16, tiles: [
            TemplateTile("Play", "I want to play", "🧸", TemplateColor.verb),
            TemplateTile("My turn", "It is my turn", "🙋", TemplateColor.social),
            TemplateTile("Your turn", "It is your turn", "👉", TemplateColor.social),
            TemplateTile("Again!", "Again please", "🔁", TemplateColor.verb),
            TemplateTile("Bubbles", "Bubbles", "🫧", TemplateColor.noun),
            TemplateTile("Blocks", "Blocks", "🧱", TemplateColor.noun),
            TemplateTile("Ball", "Ball", "⚽", TemplateColor.noun),
            TemplateTile("Cars", "Cars", "🚗", TemplateColor.noun),
            TemplateTile("Puzzle", "Puzzle", "🧩", TemplateColor.noun),
            TemplateTile("Drawing", "I want to draw", "🖍️", TemplateColor.verb),
            TemplateTile("Tablet", "I want the tablet", "📱", TemplateColor.noun),
            TemplateTile("Hide and seek", "Let's play hide and seek", "🙈", TemplateColor.verb),
            TemplateTile("Chase me", "Chase me!", "🏃", TemplateColor.verb),
            TemplateTile("Tickle", "Tickle me", "🤣", TemplateColor.verb),
            TemplateTile("Stop", "Stop please", "🛑", TemplateColor.negative),
            TemplateTile("So fun!", "That is so fun!", "🥳", TemplateColor.descriptor)
        ]),

        PageTemplate(id: "silly", title: "Silly Talk", summary: "Jokes and nonsense - language is not only for needs",
            category: "Fun & Silly", icon: "🤪", accent: "#C026D3", gridSize: 12, tiles: [
            TemplateTile("That's silly", "That is so silly", "🤪", TemplateColor.descriptor),
            TemplateTile("Ha ha ha", "Ha ha ha!", "😂", TemplateColor.social),
            TemplateTile("Uh oh!", "Uh oh!", "😯", TemplateColor.social),
            TemplateTile("Oh no!", "Oh no!", "🙀", TemplateColor.social),
            TemplateTile("Yucky", "Ewww yucky", "🤢", TemplateColor.negative),
            TemplateTile("Stinky", "That is stinky", "💩", TemplateColor.descriptor),
            TemplateTile("Boo!", "Boo!", "👻", TemplateColor.social),
            TemplateTile("Wow!", "Wow!", "🤩", TemplateColor.social),
            TemplateTile("Whoops", "Whoopsie", "🙃", TemplateColor.social),
            TemplateTile("Silly face", "Make a silly face", "😝", TemplateColor.verb),
            TemplateTile("Knock knock", "Knock knock!", "🚪", TemplateColor.social),
            TemplateTile("You're funny", "You are so funny", "😆", TemplateColor.social)
        ]),

        PageTemplate(id: "bored", title: "I'm Bored", summary: "Turns a whine into a choice",
            category: "Fun & Silly", icon: "😑", accent: "#7C3AED", gridSize: 12, tiles: [
            TemplateTile("I'm bored", "I am bored", "😑", TemplateColor.descriptor),
            TemplateTile("What can I do?", "What can I do?", "❓", TemplateColor.question),
            TemplateTile("Play with me", "Will you play with me?", "🧑‍🤝‍🧑", TemplateColor.social),
            TemplateTile("Go outside", "Let's go outside", "🌳", TemplateColor.verb),
            TemplateTile("Draw", "I want to draw", "🖍️", TemplateColor.verb),
            TemplateTile("Build", "I want to build something", "🧱", TemplateColor.verb),
            TemplateTile("Read", "Read with me", "📖", TemplateColor.verb),
            TemplateTile("Music", "Put on music", "🎵", TemplateColor.verb),
            TemplateTile("Dance", "Let's dance", "💃", TemplateColor.verb),
            TemplateTile("Bake", "Let's bake something", "🧁", TemplateColor.verb),
            TemplateTile("Tablet", "Can I have the tablet?", "📱", TemplateColor.noun),
            TemplateTile("Nothing", "I don't want to do anything", "🙅", TemplateColor.negative)
        ]),

        PageTemplate(id: "party", title: "Party & Birthday", summary: "For the days that are a big deal",
            category: "Fun & Silly", icon: "🎂", accent: "#E11D48", gridSize: 12, tiles: [
            TemplateTile("Happy birthday", "Happy birthday!", "🎂", TemplateColor.social),
            TemplateTile("It's my birthday", "It is my birthday!", "🥳", TemplateColor.social),
            TemplateTile("Presents", "Presents!", "🎁", TemplateColor.noun),
            TemplateTile("Cake", "I want cake", "🍰", TemplateColor.noun),
            TemplateTile("Ice cream", "Ice cream please", "🍦", TemplateColor.noun),
            TemplateTile("Balloons", "Balloons", "🎈", TemplateColor.noun),
            TemplateTile("Sing", "Sing the song!", "🎤", TemplateColor.verb),
            TemplateTile("Blow candles", "I want to blow the candles", "🕯️", TemplateColor.verb),
            TemplateTile("Thank you", "Thank you so much", "🙏", TemplateColor.social),
            TemplateTile("Too loud", "It is too loud in here", "🔊", TemplateColor.negative),
            TemplateTile("I need a break", "I need a quiet break", "⏸️", TemplateColor.negative),
            TemplateTile("Best day", "This is the best day", "🌟", TemplateColor.descriptor)
        ]),

        PageTemplate(id: "animals", title: "Animals", summary: "High-motivation board for early vocabulary",
            category: "Fun & Silly", icon: "🐶", accent: "#16A34A", gridSize: 16, tiles: [
            TemplateTile("Dog", "Dog", "🐶", TemplateColor.noun),
            TemplateTile("Cat", "Cat", "🐱", TemplateColor.noun),
            TemplateTile("Bird", "Bird", "🐦", TemplateColor.noun),
            TemplateTile("Fish", "Fish", "🐟", TemplateColor.noun),
            TemplateTile("Horse", "Horse", "🐴", TemplateColor.noun),
            TemplateTile("Cow", "Cow", "🐮", TemplateColor.noun),
            TemplateTile("Pig", "Pig", "🐷", TemplateColor.noun),
            TemplateTile("Sheep", "Sheep", "🐑", TemplateColor.noun),
            TemplateTile("Duck", "Duck", "🦆", TemplateColor.noun),
            TemplateTile("Rabbit", "Rabbit", "🐰", TemplateColor.noun),
            TemplateTile("Bear", "Bear", "🐻", TemplateColor.noun),
            TemplateTile("Lion", "Lion", "🦁", TemplateColor.noun),
            TemplateTile("Elephant", "Elephant", "🐘", TemplateColor.noun),
            TemplateTile("Monkey", "Monkey", "🐵", TemplateColor.noun),
            TemplateTile("I see a", "I see a", "👀", TemplateColor.verb),
            TemplateTile("So cute", "That is so cute", "🥰", TemplateColor.descriptor)
        ]),

        PageTemplate(id: "screen", title: "Shows & Games", summary: "Screen time, negotiated in words",
            category: "Fun & Silly", icon: "📺", accent: "#4338CA", gridSize: 12, tiles: [
            TemplateTile("Tablet", "Can I have the tablet?", "📱", TemplateColor.noun),
            TemplateTile("TV", "Can we watch TV?", "📺", TemplateColor.noun),
            TemplateTile("My show", "I want my show", "🎬", TemplateColor.noun),
            TemplateTile("Game", "I want to play a game", "🎮", TemplateColor.noun),
            TemplateTile("Next one", "Play the next one", "⏭️", TemplateColor.verb),
            TemplateTile("Pause", "Pause please", "⏸️", TemplateColor.verb),
            TemplateTile("Louder", "Turn it up", "🔊", TemplateColor.verb),
            TemplateTile("Quieter", "Turn it down", "🔉", TemplateColor.verb),
            TemplateTile("Watch with me", "Watch with me", "🧑‍🤝‍🧑", TemplateColor.social),
            TemplateTile("Five more minutes", "Five more minutes please", "⏰", TemplateColor.negative),
            TemplateTile("Scary", "This is too scary", "😨", TemplateColor.negative),
            TemplateTile("All done", "I am all done", "🏁", TemplateColor.social)
        ])
    ]
}
