import Foundation

/// One button inside a template.
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

/// Fitzgerald Key colours - the colour coding used across mainstream AAC
/// systems. Keeping to it means a child who has used another device or a
/// paper board finds words where they expect them.
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
    public let gridSize: Int
    public let tiles: [TemplateTile]

    public var buttonCount: Int { tiles.count }

    /// Builds a real page. Slots are 1-based and fill in reading order.
    public func makePage() -> PageModel {
        var page = PageModel(title: title, type: .grid, gridSize: gridSize)
        for (index, t) in tiles.enumerated() {
            let slot = index + 1
            page.tiles[slot] = TileModel(
                id: slot,
                label: t.label,
                tts: t.tts,
                symbolName: t.symbol,
                bgHex: t.color,
                borderHex: "#CBD5E1",
                labelHex: "#1E293B"
            )
        }
        return page
    }
}

/// Twenty starter boards covering the topics AAC users most commonly need.
///
/// Every template ships with real buttons. The gallery previously offered
/// four boards whose descriptions promised content and then installed an
/// empty grid, and the wizard's "Core Words" and "Feelings" presets did the
/// same - only "Food" ever wrote a tile.
public enum PageTemplateCatalog {

    public static let categories = ["Essential", "Daily Living", "Social", "Learning", "Fun"]

    public static let all: [PageTemplate] = [
        core, yesNo, helpRequests, feelings,
        food, drinks, bathroom, dressing, bedtime, medical,
        people, greetings, sensory, outings,
        school, bodyParts, schedule, weather,
        play, animals
    ]

    public static func templates(in category: String) -> [PageTemplate] {
        all.filter { $0.category == category }
    }

    // MARK: - Essential

    static let core = PageTemplate(
        id: "core", title: "Core Words",
        summary: "The high-frequency words that carry most of everyday speech",
        category: "Essential", gridSize: 25,
        tiles: [
            TemplateTile("I", "I", nil, TemplateColor.person),
            TemplateTile("you", "you", nil, TemplateColor.person),
            TemplateTile("it", "it", nil, TemplateColor.person),
            TemplateTile("my", "my", nil, TemplateColor.person),
            TemplateTile("want", "I want", nil, TemplateColor.verb),
            TemplateTile("go", "go", nil, TemplateColor.verb),
            TemplateTile("stop", "stop", "stop", TemplateColor.negative),
            TemplateTile("more", "more", "more", TemplateColor.verb),
            TemplateTile("help", "help me", "help", TemplateColor.verb),
            TemplateTile("like", "I like it", "love", TemplateColor.verb),
            TemplateTile("look", "look", nil, TemplateColor.verb),
            TemplateTile("make", "make", nil, TemplateColor.verb),
            TemplateTile("do", "do", nil, TemplateColor.verb),
            TemplateTile("put", "put", nil, TemplateColor.verb),
            TemplateTile("turn", "turn", nil, TemplateColor.verb),
            TemplateTile("yes", "yes", "yes", TemplateColor.social),
            TemplateTile("no", "no", "no", TemplateColor.negative),
            TemplateTile("all done", "I am all done", nil, TemplateColor.social),
            TemplateTile("again", "again", nil, TemplateColor.verb),
            TemplateTile("big", "big", nil, TemplateColor.descriptor),
            TemplateTile("little", "little", nil, TemplateColor.descriptor),
            TemplateTile("here", "here", nil, TemplateColor.descriptor),
            TemplateTile("there", "there", nil, TemplateColor.descriptor),
            TemplateTile("in", "in", nil, TemplateColor.descriptor),
            TemplateTile("out", "out", nil, TemplateColor.descriptor)
        ]
    )

    static let yesNo = PageTemplate(
        id: "yesno", title: "Yes / No",
        summary: "A first board - large targets for answering questions",
        category: "Essential", gridSize: 4,
        tiles: [
            TemplateTile("Yes", "Yes", "yes", TemplateColor.social),
            TemplateTile("No", "No", "no", TemplateColor.negative),
            TemplateTile("Maybe", "Maybe", nil, TemplateColor.descriptor),
            TemplateTile("I don't know", "I don't know", nil, TemplateColor.plain)
        ]
    )

    static let helpRequests = PageTemplate(
        id: "help", title: "Help & Requests",
        summary: "Ask for help, a break, or attention",
        category: "Essential", gridSize: 9,
        tiles: [
            TemplateTile("Help me", "Help me please", "help", TemplateColor.verb),
            TemplateTile("I need a break", "I need a break", nil, TemplateColor.verb),
            TemplateTile("Wait", "Wait please", nil, TemplateColor.verb),
            TemplateTile("Come here", "Come here please", nil, TemplateColor.verb),
            TemplateTile("I want that", "I want that", nil, TemplateColor.verb),
            TemplateTile("Not that", "Not that one", nil, TemplateColor.negative),
            TemplateTile("More please", "More please", "more", TemplateColor.verb),
            TemplateTile("All done", "I am all done", "stop", TemplateColor.social),
            TemplateTile("Thank you", "Thank you", nil, TemplateColor.social)
        ]
    )

    static let feelings = PageTemplate(
        id: "feelings", title: "Feelings",
        summary: "Name an emotion before it becomes behaviour",
        category: "Essential", gridSize: 12,
        tiles: [
            TemplateTile("Happy", "I feel happy", "happy", TemplateColor.descriptor),
            TemplateTile("Sad", "I feel sad", "sad", TemplateColor.descriptor),
            TemplateTile("Mad", "I feel mad", nil, TemplateColor.negative),
            TemplateTile("Scared", "I feel scared", nil, TemplateColor.descriptor),
            TemplateTile("Tired", "I feel tired", "sleep", TemplateColor.descriptor),
            TemplateTile("Excited", "I feel excited", nil, TemplateColor.descriptor),
            TemplateTile("Silly", "I feel silly", nil, TemplateColor.descriptor),
            TemplateTile("Calm", "I feel calm", nil, TemplateColor.descriptor),
            TemplateTile("Sick", "I feel sick", nil, TemplateColor.negative),
            TemplateTile("Hurt", "Something hurts", nil, TemplateColor.negative),
            TemplateTile("Love you", "I love you", "love", TemplateColor.social),
            TemplateTile("Too loud", "It is too loud", nil, TemplateColor.negative)
        ]
    )

    // MARK: - Daily Living

    static let food = PageTemplate(
        id: "food", title: "Food & Snacks",
        summary: "Mealtime and snack choices",
        category: "Daily Living", gridSize: 16,
        tiles: [
            TemplateTile("Hungry", "I am hungry", "eat", TemplateColor.verb),
            TemplateTile("Eat", "I want to eat", "eat", TemplateColor.verb),
            TemplateTile("More", "More please", "more", TemplateColor.verb),
            TemplateTile("All done", "I am all done", "stop", TemplateColor.social),
            TemplateTile("Apple", "Apple", nil, TemplateColor.noun),
            TemplateTile("Banana", "Banana", nil, TemplateColor.noun),
            TemplateTile("Crackers", "Crackers", nil, TemplateColor.noun),
            TemplateTile("Cookie", "Cookie", nil, TemplateColor.noun),
            TemplateTile("Sandwich", "Sandwich", nil, TemplateColor.noun),
            TemplateTile("Pizza", "Pizza", nil, TemplateColor.noun),
            TemplateTile("Pasta", "Pasta", nil, TemplateColor.noun),
            TemplateTile("Cereal", "Cereal", nil, TemplateColor.noun),
            TemplateTile("Yogurt", "Yogurt", nil, TemplateColor.noun),
            TemplateTile("Cheese", "Cheese", nil, TemplateColor.noun),
            TemplateTile("I don't like it", "I don't like it", nil, TemplateColor.negative),
            TemplateTile("It's yummy", "That is yummy", nil, TemplateColor.descriptor)
        ]
    )

    static let drinks = PageTemplate(
        id: "drinks", title: "Drinks",
        summary: "Thirsty, and what to ask for",
        category: "Daily Living", gridSize: 9,
        tiles: [
            TemplateTile("Thirsty", "I am thirsty", "water", TemplateColor.verb),
            TemplateTile("Water", "Water please", "water", TemplateColor.noun),
            TemplateTile("Milk", "Milk please", nil, TemplateColor.noun),
            TemplateTile("Juice", "Juice please", nil, TemplateColor.noun),
            TemplateTile("Hot chocolate", "Hot chocolate please", nil, TemplateColor.noun),
            TemplateTile("Smoothie", "A smoothie please", nil, TemplateColor.noun),
            TemplateTile("Cup", "I need a cup", nil, TemplateColor.noun),
            TemplateTile("Straw", "I need a straw", nil, TemplateColor.noun),
            TemplateTile("More", "More please", "more", TemplateColor.verb)
        ]
    )

    static let bathroom = PageTemplate(
        id: "bathroom", title: "Bathroom",
        summary: "Private needs, said quickly",
        category: "Daily Living", gridSize: 9,
        tiles: [
            TemplateTile("Bathroom", "I need the bathroom", "bathroom", TemplateColor.verb),
            TemplateTile("Now please", "I need to go now", nil, TemplateColor.negative),
            TemplateTile("Wash hands", "I need to wash my hands", nil, TemplateColor.verb),
            TemplateTile("Toilet paper", "I need toilet paper", nil, TemplateColor.noun),
            TemplateTile("Help me", "I need help please", "help", TemplateColor.verb),
            TemplateTile("All done", "I am all done", "stop", TemplateColor.social),
            TemplateTile("Privacy please", "I would like privacy please", nil, TemplateColor.social),
            TemplateTile("Accident", "I had an accident", nil, TemplateColor.negative),
            TemplateTile("Change please", "I need changing please", nil, TemplateColor.negative)
        ]
    )

    static let dressing = PageTemplate(
        id: "dressing", title: "Getting Dressed",
        summary: "Clothes and how they feel",
        category: "Daily Living", gridSize: 12,
        tiles: [
            TemplateTile("Shirt", "Shirt", nil, TemplateColor.noun),
            TemplateTile("Pants", "Pants", nil, TemplateColor.noun),
            TemplateTile("Socks", "Socks", nil, TemplateColor.noun),
            TemplateTile("Shoes", "Shoes", nil, TemplateColor.noun),
            TemplateTile("Coat", "Coat", nil, TemplateColor.noun),
            TemplateTile("Hat", "Hat", nil, TemplateColor.noun),
            TemplateTile("Pajamas", "Pajamas", nil, TemplateColor.noun),
            TemplateTile("Too tight", "This is too tight", nil, TemplateColor.negative),
            TemplateTile("Too itchy", "This is too itchy", nil, TemplateColor.negative),
            TemplateTile("I'm cold", "I am cold", nil, TemplateColor.descriptor),
            TemplateTile("I'm hot", "I am hot", nil, TemplateColor.descriptor),
            TemplateTile("I can do it", "I can do it myself", nil, TemplateColor.social)
        ]
    )

    static let bedtime = PageTemplate(
        id: "bedtime", title: "Bedtime",
        summary: "The wind-down routine",
        category: "Daily Living", gridSize: 9,
        tiles: [
            TemplateTile("Tired", "I am tired", "sleep", TemplateColor.descriptor),
            TemplateTile("Bath", "I want a bath", nil, TemplateColor.verb),
            TemplateTile("Teeth", "Brush my teeth", nil, TemplateColor.verb),
            TemplateTile("Story", "Read me a story", "book", TemplateColor.verb),
            TemplateTile("Song", "Sing me a song", "music", TemplateColor.verb),
            TemplateTile("Cuddle", "I want a cuddle", "love", TemplateColor.social),
            TemplateTile("Light on", "Leave the light on", nil, TemplateColor.descriptor),
            TemplateTile("Not tired", "I am not tired yet", nil, TemplateColor.negative),
            TemplateTile("Goodnight", "Goodnight, I love you", nil, TemplateColor.social)
        ]
    )

    static let medical = PageTemplate(
        id: "medical", title: "Pain & Medical",
        summary: "Where it hurts and how much - for appointments and emergencies",
        category: "Daily Living", gridSize: 12,
        tiles: [
            TemplateTile("It hurts", "Something hurts", nil, TemplateColor.negative),
            TemplateTile("A little", "It hurts a little", nil, TemplateColor.descriptor),
            TemplateTile("A lot", "It hurts a lot", nil, TemplateColor.negative),
            TemplateTile("Head", "My head hurts", nil, TemplateColor.noun),
            TemplateTile("Tummy", "My tummy hurts", nil, TemplateColor.noun),
            TemplateTile("Ear", "My ear hurts", nil, TemplateColor.noun),
            TemplateTile("Throat", "My throat hurts", nil, TemplateColor.noun),
            TemplateTile("Tooth", "My tooth hurts", nil, TemplateColor.noun),
            TemplateTile("Sick", "I feel sick", nil, TemplateColor.negative),
            TemplateTile("Dizzy", "I feel dizzy", nil, TemplateColor.negative),
            TemplateTile("Medicine", "I need my medicine", nil, TemplateColor.noun),
            TemplateTile("Get my mom", "Please get my mom", nil, TemplateColor.social)
        ]
    )

    // MARK: - Social

    static let people = PageTemplate(
        id: "people", title: "People",
        summary: "Family and the people around every day",
        category: "Social", gridSize: 12,
        tiles: [
            TemplateTile("Mom", "Mom", nil, TemplateColor.person),
            TemplateTile("Dad", "Dad", nil, TemplateColor.person),
            TemplateTile("Grandma", "Grandma", nil, TemplateColor.person),
            TemplateTile("Grandpa", "Grandpa", nil, TemplateColor.person),
            TemplateTile("Brother", "My brother", nil, TemplateColor.person),
            TemplateTile("Sister", "My sister", nil, TemplateColor.person),
            TemplateTile("Me", "Me", nil, TemplateColor.person),
            TemplateTile("Friend", "My friend", nil, TemplateColor.person),
            TemplateTile("Teacher", "My teacher", nil, TemplateColor.person),
            TemplateTile("Doctor", "The doctor", nil, TemplateColor.person),
            TemplateTile("Where is", "Where is", nil, TemplateColor.question),
            TemplateTile("I miss you", "I miss you", "love", TemplateColor.social)
        ]
    )

    static let greetings = PageTemplate(
        id: "greetings", title: "Greetings & Chat",
        summary: "Opening and closing a conversation",
        category: "Social", gridSize: 12,
        tiles: [
            TemplateTile("Hi", "Hi!", nil, TemplateColor.social),
            TemplateTile("Bye", "Goodbye!", nil, TemplateColor.social),
            TemplateTile("My name is", "My name is", nil, TemplateColor.social),
            TemplateTile("How are you?", "How are you?", nil, TemplateColor.question),
            TemplateTile("I'm good", "I am good", nil, TemplateColor.descriptor),
            TemplateTile("Please", "Please", nil, TemplateColor.social),
            TemplateTile("Thank you", "Thank you", nil, TemplateColor.social),
            TemplateTile("Sorry", "I am sorry", nil, TemplateColor.social),
            TemplateTile("Excuse me", "Excuse me", nil, TemplateColor.social),
            TemplateTile("Your turn", "It is your turn", nil, TemplateColor.social),
            TemplateTile("Look at this", "Look at this!", nil, TemplateColor.verb),
            TemplateTile("Guess what", "Guess what!", nil, TemplateColor.social)
        ]
    )

    static let sensory = PageTemplate(
        id: "sensory", title: "Sensory & Regulation",
        summary: "Say what the body needs before it overflows",
        category: "Social", gridSize: 9,
        tiles: [
            TemplateTile("Too loud", "It is too loud", nil, TemplateColor.negative),
            TemplateTile("Too bright", "It is too bright", nil, TemplateColor.negative),
            TemplateTile("Too busy", "It is too busy here", nil, TemplateColor.negative),
            TemplateTile("I need space", "I need some space", nil, TemplateColor.verb),
            TemplateTile("Squeeze", "I want a tight squeeze", nil, TemplateColor.verb),
            TemplateTile("Quiet place", "I need a quiet place", nil, TemplateColor.verb),
            TemplateTile("Headphones", "I want my headphones", nil, TemplateColor.noun),
            TemplateTile("Break please", "I need a break please", nil, TemplateColor.verb),
            TemplateTile("I'm okay now", "I am okay now", nil, TemplateColor.social)
        ]
    )

    static let outings = PageTemplate(
        id: "outings", title: "Places We Go",
        summary: "Out in the community",
        category: "Social", gridSize: 12,
        tiles: [
            TemplateTile("Home", "I want to go home", "home", TemplateColor.noun),
            TemplateTile("School", "School", "school", TemplateColor.noun),
            TemplateTile("Park", "The park", nil, TemplateColor.noun),
            TemplateTile("Store", "The store", nil, TemplateColor.noun),
            TemplateTile("Car", "In the car", "bus", TemplateColor.noun),
            TemplateTile("Bus", "The bus", "bus", TemplateColor.noun),
            TemplateTile("Grandma's", "Grandma's house", nil, TemplateColor.noun),
            TemplateTile("Doctor", "The doctor", nil, TemplateColor.noun),
            TemplateTile("Swimming", "Swimming", nil, TemplateColor.noun),
            TemplateTile("Where are we going?", "Where are we going?", nil, TemplateColor.question),
            TemplateTile("Let's go", "Let's go", "bus", TemplateColor.verb),
            TemplateTile("I want to stay", "I want to stay", nil, TemplateColor.negative)
        ]
    )

    // MARK: - Learning

    static let school = PageTemplate(
        id: "school", title: "School",
        summary: "Classroom routine and asking for what you need",
        category: "Learning", gridSize: 16,
        tiles: [
            TemplateTile("Here", "I am here", nil, TemplateColor.social),
            TemplateTile("Raise hand", "I have my hand up", nil, TemplateColor.verb),
            TemplateTile("I know", "I know the answer", nil, TemplateColor.verb),
            TemplateTile("I need help", "I need help please", "help", TemplateColor.verb),
            TemplateTile("Bathroom", "May I go to the bathroom?", "bathroom", TemplateColor.verb),
            TemplateTile("Pencil", "I need a pencil", nil, TemplateColor.noun),
            TemplateTile("Paper", "I need paper", nil, TemplateColor.noun),
            TemplateTile("Book", "My book", "book", TemplateColor.noun),
            TemplateTile("Backpack", "My backpack", nil, TemplateColor.noun),
            TemplateTile("Snack", "Snack time", "eat", TemplateColor.noun),
            TemplateTile("Recess", "Recess", "play", TemplateColor.noun),
            TemplateTile("Finished", "I am finished", "stop", TemplateColor.social),
            TemplateTile("Too hard", "This is too hard", nil, TemplateColor.negative),
            TemplateTile("Say it again", "Please say it again", nil, TemplateColor.question),
            TemplateTile("My turn", "It is my turn", nil, TemplateColor.social),
            TemplateTile("Go home", "Time to go home", "home", TemplateColor.verb)
        ]
    )

    static let bodyParts = PageTemplate(
        id: "body", title: "My Body",
        summary: "Body parts - pairs with the pain board",
        category: "Learning", gridSize: 12,
        tiles: [
            TemplateTile("Head", "Head", nil, TemplateColor.noun),
            TemplateTile("Eyes", "Eyes", nil, TemplateColor.noun),
            TemplateTile("Ears", "Ears", nil, TemplateColor.noun),
            TemplateTile("Nose", "Nose", nil, TemplateColor.noun),
            TemplateTile("Mouth", "Mouth", nil, TemplateColor.noun),
            TemplateTile("Hands", "Hands", nil, TemplateColor.noun),
            TemplateTile("Arms", "Arms", nil, TemplateColor.noun),
            TemplateTile("Legs", "Legs", nil, TemplateColor.noun),
            TemplateTile("Feet", "Feet", nil, TemplateColor.noun),
            TemplateTile("Tummy", "Tummy", nil, TemplateColor.noun),
            TemplateTile("Back", "Back", nil, TemplateColor.noun),
            TemplateTile("Hurts here", "It hurts here", nil, TemplateColor.negative)
        ]
    )

    static let schedule = PageTemplate(
        id: "schedule", title: "My Day",
        summary: "What happens next - reduces transition anxiety",
        category: "Learning", gridSize: 12,
        tiles: [
            TemplateTile("First", "First", nil, TemplateColor.descriptor),
            TemplateTile("Next", "Next", nil, TemplateColor.descriptor),
            TemplateTile("Last", "Last", nil, TemplateColor.descriptor),
            TemplateTile("Wake up", "Wake up", nil, TemplateColor.verb),
            TemplateTile("Breakfast", "Breakfast", "eat", TemplateColor.noun),
            TemplateTile("School", "School", "school", TemplateColor.noun),
            TemplateTile("Lunch", "Lunch", "eat", TemplateColor.noun),
            TemplateTile("Play", "Play time", "play", TemplateColor.noun),
            TemplateTile("Dinner", "Dinner", "eat", TemplateColor.noun),
            TemplateTile("Bath", "Bath time", nil, TemplateColor.noun),
            TemplateTile("Bed", "Bed time", "sleep", TemplateColor.noun),
            TemplateTile("What's next?", "What is next?", nil, TemplateColor.question)
        ]
    )

    static let weather = PageTemplate(
        id: "weather", title: "Weather & Outside",
        summary: "A daily circle-time favourite",
        category: "Learning", gridSize: 9,
        tiles: [
            TemplateTile("Sunny", "It is sunny", nil, TemplateColor.descriptor),
            TemplateTile("Rainy", "It is raining", nil, TemplateColor.descriptor),
            TemplateTile("Cloudy", "It is cloudy", nil, TemplateColor.descriptor),
            TemplateTile("Snowy", "It is snowing", nil, TemplateColor.descriptor),
            TemplateTile("Windy", "It is windy", nil, TemplateColor.descriptor),
            TemplateTile("Hot", "It is hot", nil, TemplateColor.descriptor),
            TemplateTile("Cold", "It is cold", nil, TemplateColor.descriptor),
            TemplateTile("Go outside", "I want to go outside", nil, TemplateColor.verb),
            TemplateTile("Stay inside", "I want to stay inside", nil, TemplateColor.negative)
        ]
    )

    // MARK: - Fun

    static let play = PageTemplate(
        id: "play", title: "Play & Toys",
        summary: "Choosing an activity and playing with someone",
        category: "Fun", gridSize: 16,
        tiles: [
            TemplateTile("Play", "I want to play", "play", TemplateColor.verb),
            TemplateTile("My turn", "It is my turn", nil, TemplateColor.social),
            TemplateTile("Your turn", "It is your turn", nil, TemplateColor.social),
            TemplateTile("Again", "Again please", "more", TemplateColor.verb),
            TemplateTile("Bubbles", "Bubbles", nil, TemplateColor.noun),
            TemplateTile("Blocks", "Blocks", nil, TemplateColor.noun),
            TemplateTile("Ball", "Ball", nil, TemplateColor.noun),
            TemplateTile("Cars", "Cars", nil, TemplateColor.noun),
            TemplateTile("Puzzle", "Puzzle", nil, TemplateColor.noun),
            TemplateTile("Music", "Music please", "music", TemplateColor.noun),
            TemplateTile("Tablet", "I want the tablet", nil, TemplateColor.noun),
            TemplateTile("Outside", "Let's go outside", nil, TemplateColor.verb),
            TemplateTile("Swing", "I want to swing", nil, TemplateColor.verb),
            TemplateTile("Tickle", "Tickle me", nil, TemplateColor.verb),
            TemplateTile("Stop", "Stop please", "stop", TemplateColor.negative),
            TemplateTile("That's fun", "That is fun!", nil, TemplateColor.descriptor)
        ]
    )

    static let animals = PageTemplate(
        id: "animals", title: "Animals",
        summary: "A high-motivation board for early vocabulary",
        category: "Fun", gridSize: 16,
        tiles: [
            TemplateTile("Dog", "Dog", "dog", TemplateColor.noun),
            TemplateTile("Cat", "Cat", "cat", TemplateColor.noun),
            TemplateTile("Bird", "Bird", nil, TemplateColor.noun),
            TemplateTile("Fish", "Fish", nil, TemplateColor.noun),
            TemplateTile("Horse", "Horse", nil, TemplateColor.noun),
            TemplateTile("Cow", "Cow", nil, TemplateColor.noun),
            TemplateTile("Pig", "Pig", nil, TemplateColor.noun),
            TemplateTile("Sheep", "Sheep", nil, TemplateColor.noun),
            TemplateTile("Duck", "Duck", nil, TemplateColor.noun),
            TemplateTile("Rabbit", "Rabbit", nil, TemplateColor.noun),
            TemplateTile("Bear", "Bear", nil, TemplateColor.noun),
            TemplateTile("Lion", "Lion", nil, TemplateColor.noun),
            TemplateTile("Elephant", "Elephant", nil, TemplateColor.noun),
            TemplateTile("Monkey", "Monkey", nil, TemplateColor.noun),
            TemplateTile("I see a", "I see a", nil, TemplateColor.verb),
            TemplateTile("I like it", "I like it", "love", TemplateColor.descriptor)
        ]
    )
}
