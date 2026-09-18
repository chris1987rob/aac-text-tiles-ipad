package com.talktiles.tablet

// The 28 starter boards, transcribed from the iPad app's PageTemplateCatalog.swift
// by a script - keep the two in step by re-running Tools/port-templates.py
// rather than editing this file by hand.

/** One button inside a template. Every tile carries a picture. */
data class TemplateTile(val label: String, val tts: String, val symbol: String?, val color: String)

private fun T(label: String, tts: String, symbol: String? = null, color: String = TemplateColor.noun) =
    TemplateTile(label, tts, symbol, color)

/** Fitzgerald Key colours - the coding used across mainstream AAC systems. */
object TemplateColor {
    const val person     = "#FFF9C4" // yellow  - people, pronouns
    const val verb       = "#C8E6C9" // green   - actions
    const val descriptor = "#BBDEFB" // blue    - describing words
    const val noun       = "#FFE0B2" // orange  - things
    const val social     = "#F8BBD0" // pink    - social words
    const val question   = "#D1C4E9" // purple  - questions
    const val negative   = "#FFCDD2" // red     - stop, no, hurt
    const val plain      = "#FFFFFF"
}

data class PageTemplate(
    val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val icon: String,
    val accent: String,
    val gridSize: Int,
    val tiles: List<TemplateTile>
) {
    val buttonCount: Int get() = tiles.size

    fun makePage(): PageModel {
        val tiles = LinkedHashMap<Int, TileModel>()
        this.tiles.forEachIndexed { index, t ->
            val slot = index + 1
            tiles[slot] = TileModel(id = slot, label = t.label, tts = t.tts, symbolName = t.symbol,
                bgHex = t.color, borderHex = "#CBD5E1", labelHex = "#1E293B")
        }
        return PageModel(title = title, type = PageType.GRID, gridSize = gridSize, tiles = tiles)
    }
}

object PageTemplateCatalog {

    val categories = listOf("Essential", "Everyday", "Feelings & Self", "Out & About", "Fun & Silly")

    /** The five that go in the book by default - the boards that get used most. */
    val popularIDs = listOf("core", "yesno", "feelings", "food", "help")

    val popular: List<PageTemplate> get() = popularIDs.mapNotNull { id -> all.firstOrNull { it.id == id } }
    val extras: List<PageTemplate> get() = all.filter { it.id !in popularIDs }

    fun templates(inCategory: String): List<PageTemplate> = all.filter { it.category == inCategory }

    val all: List<PageTemplate> by lazy { essential + everyday + feelingsSelf + outAbout + funSilly }

    // MARK: - Essential

    val essential: List<PageTemplate> = listOf(
        PageTemplate(id = "core", title = "Core Words", summary = "The words that carry most of everyday speech",
            category = "Essential", icon = "⭐", accent = "#F59E0B", gridSize = 25, tiles = listOf(
            T("I", "I", "🙋", TemplateColor.person),
            T("you", "you", "👉", TemplateColor.person),
            T("it", "it", "👌", TemplateColor.person),
            T("my", "my", "🫵", TemplateColor.person),
            T("want", "I want", "🤲", TemplateColor.verb),
            T("go", "go", "🏃", TemplateColor.verb),
            T("stop", "stop", "🛑", TemplateColor.negative),
            T("more", "more", "➕", TemplateColor.verb),
            T("help", "help me", "🙋", TemplateColor.verb),
            T("like", "I like it", "❤️", TemplateColor.verb),
            T("look", "look", "👀", TemplateColor.verb),
            T("make", "make", "🔨", TemplateColor.verb),
            T("do", "do", "✋", TemplateColor.verb),
            T("put", "put", "📥", TemplateColor.verb),
            T("turn", "turn", "🔄", TemplateColor.verb),
            T("yes", "yes", "✅", TemplateColor.social),
            T("no", "no", "❌", TemplateColor.negative),
            T("all done", "I am all done", "🏁", TemplateColor.social),
            T("again", "again", "🔁", TemplateColor.verb),
            T("big", "big", "🐘", TemplateColor.descriptor),
            T("little", "little", "🐜", TemplateColor.descriptor),
            T("here", "here", "📍", TemplateColor.descriptor),
            T("there", "there", "🗺️", TemplateColor.descriptor),
            T("in", "in", "📦", TemplateColor.descriptor),
            T("out", "out", "🚪", TemplateColor.descriptor)
        )),

        PageTemplate(id = "yesno", title = "Yes / No", summary = "A first board - big targets for answering",
            category = "Essential", icon = "✅", accent = "#16A34A", gridSize = 4, tiles = listOf(
            T("Yes", "Yes", "✅", TemplateColor.social),
            T("No", "No", "❌", TemplateColor.negative),
            T("Maybe", "Maybe", "🤷", TemplateColor.descriptor),
            T("I don't know", "I don't know", "❓", TemplateColor.question)
        )),

        // Each tile is tinted the colour it names, so the board teaches the
        // word by looking like it - the one template where the Fitzgerald
        // coding gives way to the subject itself.
        PageTemplate(id = "colors", title = "Colors", summary = "Name and choose colours - each button is its own colour",
            category = "Essential", icon = "🎨", accent = "#E11D48", gridSize = 9, tiles = listOf(
            T("Red", "Red", "🔴", "#FFB4B4"),
            T("Orange", "Orange", "🟠", "#FFD1A3"),
            T("Yellow", "Yellow", "🟡", "#FFF3A3"),
            T("Green", "Green", "🟢", "#BDEBC8"),
            T("Blue", "Blue", "🔵", "#B9DDFF"),
            T("Purple", "Purple", "🟣", "#DCC9FF"),
            T("Pink", "Pink", "🌸", "#FFC9E3"),
            T("Brown", "Brown", "🟤", "#E2C9B0"),
            T("Black", "Black", "⚫", "#D6D9E0")
        )),

        PageTemplate(id = "help", title = "Help & Requests", summary = "Ask for help, a break, or attention",
            category = "Essential", icon = "🙋", accent = "#0284C7", gridSize = 9, tiles = listOf(
            T("Help me", "Help me please", "🙋", TemplateColor.verb),
            T("Break", "I need a break", "⏸️", TemplateColor.verb),
            T("Wait", "Wait please", "✋", TemplateColor.verb),
            T("Come here", "Come here please", "👋", TemplateColor.verb),
            T("I want that", "I want that", "👉", TemplateColor.verb),
            T("Not that", "Not that one", "🙅", TemplateColor.negative),
            T("More please", "More please", "➕", TemplateColor.verb),
            T("All done", "I am all done", "🏁", TemplateColor.social),
            T("Thank you", "Thank you", "🙏", TemplateColor.social)
        )),

        PageTemplate(id = "chat", title = "Let's Chat", summary = "Start a conversation, not just a request",
            category = "Essential", icon = "💬", accent = "#7C3AED", gridSize = 12, tiles = listOf(
            T("Hi!", "Hi!", "👋", TemplateColor.social),
            T("Guess what", "Guess what!", "🤩", TemplateColor.social),
            T("Look at this", "Look at this!", "👀", TemplateColor.verb),
            T("What's that?", "What is that?", "❓", TemplateColor.question),
            T("Me too", "Me too!", "🙌", TemplateColor.social),
            T("That's funny", "That is funny", "😂", TemplateColor.descriptor),
            T("Cool!", "That is so cool", "😎", TemplateColor.descriptor),
            T("Tell me more", "Tell me more", "👂", TemplateColor.question),
            T("Your turn", "Your turn", "🔄", TemplateColor.social),
            T("I'm listening", "I am listening", "👂", TemplateColor.social),
            T("No thanks", "No thank you", "🙅", TemplateColor.negative),
            T("Bye!", "Goodbye!", "👋", TemplateColor.social)
        )),
    )

    // MARK: - Everyday

    val everyday: List<PageTemplate> = listOf(
        PageTemplate(id = "food", title = "Food & Snacks", summary = "Mealtimes and snack choices",
            category = "Everyday", icon = "🍎", accent = "#DC2626", gridSize = 16, tiles = listOf(
            T("Hungry", "I am hungry", "😋", TemplateColor.verb),
            T("Eat", "I want to eat", "🍽️", TemplateColor.verb),
            T("More", "More please", "➕", TemplateColor.verb),
            T("All done", "I am all done", "🏁", TemplateColor.social),
            T("Apple", "Apple", "🍎", TemplateColor.noun),
            T("Banana", "Banana", "🍌", TemplateColor.noun),
            T("Crackers", "Crackers", "🍘", TemplateColor.noun),
            T("Cookie", "Cookie", "🍪", TemplateColor.noun),
            T("Sandwich", "Sandwich", "🥪", TemplateColor.noun),
            T("Pizza", "Pizza", "🍕", TemplateColor.noun),
            T("Pasta", "Pasta", "🍝", TemplateColor.noun),
            T("Cereal", "Cereal", "🥣", TemplateColor.noun),
            T("Yogurt", "Yogurt", "🥛", TemplateColor.noun),
            T("Cheese", "Cheese", "🧀", TemplateColor.noun),
            T("Yuck", "I don't like it", "🤢", TemplateColor.negative),
            T("Yummy", "That is yummy", "😋", TemplateColor.descriptor)
        )),

        PageTemplate(id = "drinks", title = "Drinks", summary = "Thirsty, and what to ask for",
            category = "Everyday", icon = "🥤", accent = "#0891B2", gridSize = 9, tiles = listOf(
            T("Thirsty", "I am thirsty", "😰", TemplateColor.verb),
            T("Water", "Water please", "💧", TemplateColor.noun),
            T("Milk", "Milk please", "🥛", TemplateColor.noun),
            T("Juice", "Juice please", "🧃", TemplateColor.noun),
            T("Hot cocoa", "Hot chocolate please", "☕", TemplateColor.noun),
            T("Smoothie", "A smoothie please", "🥤", TemplateColor.noun),
            T("Cup", "I need a cup", "🥛", TemplateColor.noun),
            T("Straw", "I need a straw", "🥤", TemplateColor.noun),
            T("More", "More please", "➕", TemplateColor.verb)
        )),

        PageTemplate(id = "bathroom", title = "Bathroom", summary = "Private needs, said fast",
            category = "Everyday", icon = "🚻", accent = "#6366F1", gridSize = 9, tiles = listOf(
            T("Bathroom", "I need the bathroom", "🚻", TemplateColor.verb),
            T("Now!", "I need to go now", "🏃", TemplateColor.negative),
            T("Wash hands", "I need to wash my hands", "🧼", TemplateColor.verb),
            T("Toilet paper", "I need toilet paper", "🧻", TemplateColor.noun),
            T("Help me", "I need help please", "🙋", TemplateColor.verb),
            T("All done", "I am all done", "🏁", TemplateColor.social),
            T("Privacy", "I would like privacy please", "🚪", TemplateColor.social),
            T("Accident", "I had an accident", "😞", TemplateColor.negative),
            T("Change", "I need changing please", "🔄", TemplateColor.negative)
        )),

        PageTemplate(id = "morning", title = "Getting Ready", summary = "The morning routine, in order",
            category = "Everyday", icon = "🪥", accent = "#0EA5E9", gridSize = 12, tiles = listOf(
            T("Wake up", "Time to wake up", "🌅", TemplateColor.verb),
            T("5 more minutes", "Five more minutes please", "😴", TemplateColor.negative),
            T("Bathroom", "Bathroom first", "🚻", TemplateColor.verb),
            T("Brush teeth", "Brush my teeth", "🪥", TemplateColor.verb),
            T("Wash face", "Wash my face", "🧼", TemplateColor.verb),
            T("Brush hair", "Brush my hair", "💇", TemplateColor.verb),
            T("Shirt", "Shirt", "👕", TemplateColor.noun),
            T("Pants", "Pants", "👖", TemplateColor.noun),
            T("Socks", "Socks", "🧦", TemplateColor.noun),
            T("Shoes", "Shoes", "👟", TemplateColor.noun),
            T("Coat", "Coat", "🧥", TemplateColor.noun),
            T("I can do it", "I can do it myself", "💪", TemplateColor.social)
        )),

        PageTemplate(id = "bedtime", title = "Bedtime", summary = "The wind-down routine",
            category = "Everyday", icon = "🌙", accent = "#4F46E5", gridSize = 9, tiles = listOf(
            T("Tired", "I am tired", "😴", TemplateColor.descriptor),
            T("Bath", "I want a bath", "🛁", TemplateColor.verb),
            T("Teeth", "Brush my teeth", "🪥", TemplateColor.verb),
            T("Story", "Read me a story", "📖", TemplateColor.verb),
            T("Song", "Sing me a song", "🎵", TemplateColor.verb),
            T("Cuddle", "I want a cuddle", "🤗", TemplateColor.social),
            T("Light on", "Leave the light on", "💡", TemplateColor.descriptor),
            T("Not tired", "I am not tired yet", "🙅", TemplateColor.negative),
            T("Goodnight", "Goodnight, I love you", "🌙", TemplateColor.social)
        )),

        PageTemplate(id = "myday", title = "My Day", summary = "What happens next - eases transitions",
            category = "Everyday", icon = "📅", accent = "#059669", gridSize = 12, tiles = listOf(
            T("First", "First", "1️⃣", TemplateColor.descriptor),
            T("Next", "Next", "2️⃣", TemplateColor.descriptor),
            T("Last", "Last", "3️⃣", TemplateColor.descriptor),
            T("Wake up", "Wake up", "🌅", TemplateColor.verb),
            T("Breakfast", "Breakfast", "🥣", TemplateColor.noun),
            T("School", "School", "🏫", TemplateColor.noun),
            T("Lunch", "Lunch", "🥪", TemplateColor.noun),
            T("Play", "Play time", "🧸", TemplateColor.noun),
            T("Dinner", "Dinner", "🍽️", TemplateColor.noun),
            T("Bath", "Bath time", "🛁", TemplateColor.noun),
            T("Bed", "Bed time", "🛏️", TemplateColor.noun),
            T("What's next?", "What is next?", "❓", TemplateColor.question)
        )),
    )

    // MARK: - Feelings & Self

    val feelingsSelf: List<PageTemplate> = listOf(
        PageTemplate(id = "feelings", title = "Feelings", summary = "Name it before it becomes behaviour",
            category = "Feelings & Self", icon = "😊", accent = "#EAB308", gridSize = 12, tiles = listOf(
            T("Happy", "I feel happy", "😊", TemplateColor.descriptor),
            T("Sad", "I feel sad", "😢", TemplateColor.descriptor),
            T("Mad", "I feel mad", "😠", TemplateColor.negative),
            T("Scared", "I feel scared", "😨", TemplateColor.descriptor),
            T("Tired", "I feel tired", "😴", TemplateColor.descriptor),
            T("Excited", "I feel excited", "🤩", TemplateColor.descriptor),
            T("Silly", "I feel silly", "🤪", TemplateColor.descriptor),
            T("Calm", "I feel calm", "😌", TemplateColor.descriptor),
            T("Sick", "I feel sick", "🤒", TemplateColor.negative),
            T("Hurt", "Something hurts", "🤕", TemplateColor.negative),
            T("Love you", "I love you", "❤️", TemplateColor.social),
            T("Proud", "I feel proud of me", "🏆", TemplateColor.descriptor)
        )),

        PageTemplate(id = "angry", title = "When I'm Angry", summary = "A way out of a meltdown that isn't shouting",
            category = "Feelings & Self", icon = "😤", accent = "#DC2626", gridSize = 9, tiles = listOf(
            T("I'm angry", "I am angry right now", "😠", TemplateColor.negative),
            T("Leave me", "Please leave me alone", "🚫", TemplateColor.negative),
            T("Too much", "This is too much", "🥵", TemplateColor.negative),
            T("Not fair", "That is not fair", "⚖️", TemplateColor.negative),
            T("I need space", "I need space", "↔️", TemplateColor.verb),
            T("Breathe", "Help me breathe", "🌬️", TemplateColor.verb),
            T("Squeeze", "I want a tight squeeze", "🤗", TemplateColor.verb),
            T("Talk later", "I will talk about it later", "⏰", TemplateColor.social),
            T("I'm okay now", "I am okay now", "😌", TemplateColor.social)
        )),

        PageTemplate(id = "sensory", title = "Sensory", summary = "Say what the body needs before it overflows",
            category = "Feelings & Self", icon = "🎧", accent = "#8B5CF6", gridSize = 9, tiles = listOf(
            T("Too loud", "It is too loud", "🔊", TemplateColor.negative),
            T("Too bright", "It is too bright", "🔆", TemplateColor.negative),
            T("Too busy", "It is too busy here", "😵", TemplateColor.negative),
            T("Itchy", "My clothes are itchy", "🧥", TemplateColor.negative),
            T("Headphones", "I want my headphones", "🎧", TemplateColor.noun),
            T("Quiet place", "I need a quiet place", "🤫", TemplateColor.verb),
            T("Blanket", "I want my blanket", "🧣", TemplateColor.noun),
            T("Rock", "I want to rock", "🪑", TemplateColor.verb),
            T("Better now", "I feel better now", "😌", TemplateColor.social)
        )),

        PageTemplate(id = "medical", title = "Pain & Doctor", summary = "Where it hurts and how much",
            category = "Feelings & Self", icon = "🩺", accent = "#E11D48", gridSize = 12, tiles = listOf(
            T("It hurts", "Something hurts", "🤕", TemplateColor.negative),
            T("A little", "It hurts a little", "🙂", TemplateColor.descriptor),
            T("A lot", "It hurts a lot", "😫", TemplateColor.negative),
            T("Head", "My head hurts", "🤯", TemplateColor.noun),
            T("Tummy", "My tummy hurts", "🤢", TemplateColor.noun),
            T("Ear", "My ear hurts", "👂", TemplateColor.noun),
            T("Throat", "My throat hurts", "😷", TemplateColor.noun),
            T("Tooth", "My tooth hurts", "🦷", TemplateColor.noun),
            T("Dizzy", "I feel dizzy", "💫", TemplateColor.negative),
            T("Medicine", "I need my medicine", "💊", TemplateColor.noun),
            T("I'm scared", "I am scared", "😨", TemplateColor.negative),
            T("Get my mom", "Please get my mom", "👩", TemplateColor.social)
        )),

        PageTemplate(id = "favorites", title = "My Favourites", summary = "The things that are mine - identity, not requests",
            category = "Feelings & Self", icon = "⭐", accent = "#F59E0B", gridSize = 12, tiles = listOf(
            T("My favourite", "My favourite is", "⭐", TemplateColor.descriptor),
            T("Colour", "My favourite colour", "🌈", TemplateColor.noun),
            T("Food", "My favourite food", "🍕", TemplateColor.noun),
            T("Animal", "My favourite animal", "🐶", TemplateColor.noun),
            T("Show", "My favourite show", "📺", TemplateColor.noun),
            T("Song", "My favourite song", "🎵", TemplateColor.noun),
            T("Place", "My favourite place", "🏖️", TemplateColor.noun),
            T("Person", "My favourite person", "🧑", TemplateColor.person),
            T("Toy", "My favourite toy", "🧸", TemplateColor.noun),
            T("I love it", "I love it so much", "❤️", TemplateColor.descriptor),
            T("Not my favourite", "That is not my favourite", "🙅", TemplateColor.negative),
            T("What's yours?", "What is your favourite?", "❓", TemplateColor.question)
        )),

        PageTemplate(id = "people", title = "My People", summary = "Family and the people around every day",
            category = "Feelings & Self", icon = "👨‍👩‍👧", accent = "#D97706", gridSize = 12, tiles = listOf(
            T("Mom", "Mom", "👩", TemplateColor.person),
            T("Dad", "Dad", "👨", TemplateColor.person),
            T("Grandma", "Grandma", "👵", TemplateColor.person),
            T("Grandpa", "Grandpa", "👴", TemplateColor.person),
            T("Brother", "My brother", "👦", TemplateColor.person),
            T("Sister", "My sister", "👧", TemplateColor.person),
            T("Me", "Me", "🙋", TemplateColor.person),
            T("Friend", "My friend", "🧑‍🤝‍🧑", TemplateColor.person),
            T("Teacher", "My teacher", "👩‍🏫", TemplateColor.person),
            T("Doctor", "The doctor", "🩺", TemplateColor.person),
            T("Where is?", "Where is", "❓", TemplateColor.question),
            T("I miss you", "I miss you", "🥺", TemplateColor.social)
        )),
    )

    // MARK: - Out & About

    val outAbout: List<PageTemplate> = listOf(
        PageTemplate(id = "places", title = "Places We Go", summary = "Out in the community",
            category = "Out & About", icon = "🗺️", accent = "#0D9488", gridSize = 12, tiles = listOf(
            T("Home", "I want to go home", "🏠", TemplateColor.noun),
            T("School", "School", "🏫", TemplateColor.noun),
            T("Park", "The park", "🛝", TemplateColor.noun),
            T("Store", "The store", "🏪", TemplateColor.noun),
            T("Car", "In the car", "🚗", TemplateColor.noun),
            T("Bus", "The bus", "🚌", TemplateColor.noun),
            T("Grandma's", "Grandma's house", "🏡", TemplateColor.noun),
            T("Swimming", "Swimming", "🏊", TemplateColor.noun),
            T("Library", "The library", "📚", TemplateColor.noun),
            T("Where?", "Where are we going?", "❓", TemplateColor.question),
            T("Let's go", "Let's go", "🚶", TemplateColor.verb),
            T("Stay", "I want to stay", "🛑", TemplateColor.negative)
        )),

        PageTemplate(id = "shop", title = "At the Store", summary = "Shopping without a meltdown in aisle five",
            category = "Out & About", icon = "🛒", accent = "#65A30D", gridSize = 12, tiles = listOf(
            T("Cart", "I want to push the cart", "🛒", TemplateColor.noun),
            T("I want this", "I want this one", "👉", TemplateColor.verb),
            T("How much?", "How much is it?", "💲", TemplateColor.question),
            T("Too many people", "There are too many people", "😵", TemplateColor.negative),
            T("Snacks", "Can we get snacks?", "🍪", TemplateColor.noun),
            T("Bananas", "Bananas", "🍌", TemplateColor.noun),
            T("Bread", "Bread", "🍞", TemplateColor.noun),
            T("Milk", "Milk", "🥛", TemplateColor.noun),
            T("Toys", "Can we look at toys?", "🧸", TemplateColor.noun),
            T("Carry me", "Please carry me", "🤱", TemplateColor.verb),
            T("Are we done?", "Are we done yet?", "❓", TemplateColor.question),
            T("Let's go home", "Let's go home", "🏠", TemplateColor.verb)
        )),

        PageTemplate(id = "restaurant", title = "Eating Out", summary = "Ordering for yourself at a restaurant",
            category = "Out & About", icon = "🍽️", accent = "#B45309", gridSize = 12, tiles = listOf(
            T("Table please", "A table please", "🪑", TemplateColor.social),
            T("I'm ready", "I am ready to order", "✋", TemplateColor.social),
            T("I want", "I would like", "🤲", TemplateColor.verb),
            T("Pizza", "Pizza please", "🍕", TemplateColor.noun),
            T("Burger", "A burger please", "🍔", TemplateColor.noun),
            T("Fries", "Fries please", "🍟", TemplateColor.noun),
            T("Chicken", "Chicken please", "🍗", TemplateColor.noun),
            T("Water", "Water please", "💧", TemplateColor.noun),
            T("Napkin", "A napkin please", "🧻", TemplateColor.noun),
            T("Too hot", "This is too hot", "🥵", TemplateColor.negative),
            T("It's good", "This is really good", "😋", TemplateColor.descriptor),
            T("All finished", "I am all finished", "🏁", TemplateColor.social)
        )),

        PageTemplate(id = "car", title = "Car Rides", summary = "The long-journey board",
            category = "Out & About", icon = "🚗", accent = "#475569", gridSize = 9, tiles = listOf(
            T("Are we there?", "Are we there yet?", "❓", TemplateColor.question),
            T("How long?", "How much longer?", "⏰", TemplateColor.question),
            T("Music", "Put on music please", "🎵", TemplateColor.verb),
            T("Too loud", "It is too loud", "🔊", TemplateColor.negative),
            T("Window", "Open the window", "🪟", TemplateColor.verb),
            T("Car sick", "I feel car sick", "🤢", TemplateColor.negative),
            T("Bathroom", "I need the bathroom", "🚻", TemplateColor.negative),
            T("Snack", "Can I have a snack?", "🍪", TemplateColor.noun),
            T("Look!", "Look out there!", "👀", TemplateColor.verb)
        )),

        PageTemplate(id = "school", title = "School", summary = "Classroom routine and self-advocacy",
            category = "Out & About", icon = "🏫", accent = "#2563EB", gridSize = 16, tiles = listOf(
            T("I'm here", "I am here", "🙋", TemplateColor.social),
            T("I know!", "I know the answer", "💡", TemplateColor.verb),
            T("Help please", "I need help please", "🙋", TemplateColor.verb),
            T("Bathroom", "May I go to the bathroom?", "🚻", TemplateColor.verb),
            T("Pencil", "I need a pencil", "✏️", TemplateColor.noun),
            T("Paper", "I need paper", "📄", TemplateColor.noun),
            T("Book", "My book", "📖", TemplateColor.noun),
            T("Backpack", "My backpack", "🎒", TemplateColor.noun),
            T("Snack", "Snack time", "🍎", TemplateColor.noun),
            T("Recess", "Recess", "🛝", TemplateColor.noun),
            T("Finished", "I am finished", "🏁", TemplateColor.social),
            T("Too hard", "This is too hard", "😖", TemplateColor.negative),
            T("Say again", "Please say it again", "🔁", TemplateColor.question),
            T("My turn", "It is my turn", "🔄", TemplateColor.social),
            T("Sit with me", "Will you sit with me?", "🧑‍🤝‍🧑", TemplateColor.social),
            T("Go home", "Time to go home", "🏠", TemplateColor.verb)
        )),

        PageTemplate(id = "outside", title = "Outside & Weather", summary = "Playground, garden, and what the sky is doing",
            category = "Out & About", icon = "🌤️", accent = "#0284C7", gridSize = 16, tiles = listOf(
            T("Outside", "I want to go outside", "🌳", TemplateColor.verb),
            T("Sunny", "It is sunny", "☀️", TemplateColor.descriptor),
            T("Rainy", "It is raining", "🌧️", TemplateColor.descriptor),
            T("Snowy", "It is snowing", "❄️", TemplateColor.descriptor),
            T("Windy", "It is windy", "💨", TemplateColor.descriptor),
            T("Hot", "I am hot", "🥵", TemplateColor.descriptor),
            T("Cold", "I am cold", "🥶", TemplateColor.descriptor),
            T("Swing", "I want to swing", "🛝", TemplateColor.verb),
            T("Slide", "I want the slide", "🛝", TemplateColor.verb),
            T("Run", "I want to run", "🏃", TemplateColor.verb),
            T("Puddle", "Can I jump in the puddle?", "💦", TemplateColor.verb),
            T("Bugs", "Look, a bug!", "🐛", TemplateColor.noun),
            T("Flowers", "Flowers", "🌸", TemplateColor.noun),
            T("Coat", "I need my coat", "🧥", TemplateColor.noun),
            T("Five more minutes", "Five more minutes please", "⏰", TemplateColor.negative),
            T("Go inside", "I want to go inside", "🏠", TemplateColor.verb)
        )),
    )

    // MARK: - Fun & Silly

    val funSilly: List<PageTemplate> = listOf(
        PageTemplate(id = "play", title = "Play & Toys", summary = "Choosing an activity, and playing with someone",
            category = "Fun & Silly", icon = "🧸", accent = "#DB2777", gridSize = 16, tiles = listOf(
            T("Play", "I want to play", "🧸", TemplateColor.verb),
            T("My turn", "It is my turn", "🙋", TemplateColor.social),
            T("Your turn", "It is your turn", "👉", TemplateColor.social),
            T("Again!", "Again please", "🔁", TemplateColor.verb),
            T("Bubbles", "Bubbles", "🫧", TemplateColor.noun),
            T("Blocks", "Blocks", "🧱", TemplateColor.noun),
            T("Ball", "Ball", "⚽", TemplateColor.noun),
            T("Cars", "Cars", "🚗", TemplateColor.noun),
            T("Puzzle", "Puzzle", "🧩", TemplateColor.noun),
            T("Drawing", "I want to draw", "🖍️", TemplateColor.verb),
            T("Tablet", "I want the tablet", "📱", TemplateColor.noun),
            T("Hide and seek", "Let's play hide and seek", "🙈", TemplateColor.verb),
            T("Chase me", "Chase me!", "🏃", TemplateColor.verb),
            T("Tickle", "Tickle me", "🤣", TemplateColor.verb),
            T("Stop", "Stop please", "🛑", TemplateColor.negative),
            T("So fun!", "That is so fun!", "🥳", TemplateColor.descriptor)
        )),

        PageTemplate(id = "silly", title = "Silly Talk", summary = "Jokes and nonsense - language is not only for needs",
            category = "Fun & Silly", icon = "🤪", accent = "#C026D3", gridSize = 12, tiles = listOf(
            T("That's silly", "That is so silly", "🤪", TemplateColor.descriptor),
            T("Ha ha ha", "Ha ha ha!", "😂", TemplateColor.social),
            T("Uh oh!", "Uh oh!", "😯", TemplateColor.social),
            T("Oh no!", "Oh no!", "🙀", TemplateColor.social),
            T("Yucky", "Ewww yucky", "🤢", TemplateColor.negative),
            T("Stinky", "That is stinky", "💩", TemplateColor.descriptor),
            T("Boo!", "Boo!", "👻", TemplateColor.social),
            T("Wow!", "Wow!", "🤩", TemplateColor.social),
            T("Whoops", "Whoopsie", "🙃", TemplateColor.social),
            T("Silly face", "Make a silly face", "😝", TemplateColor.verb),
            T("Knock knock", "Knock knock!", "🚪", TemplateColor.social),
            T("You're funny", "You are so funny", "😆", TemplateColor.social)
        )),

        PageTemplate(id = "bored", title = "I'm Bored", summary = "Turns a whine into a choice",
            category = "Fun & Silly", icon = "😑", accent = "#7C3AED", gridSize = 12, tiles = listOf(
            T("I'm bored", "I am bored", "😑", TemplateColor.descriptor),
            T("What can I do?", "What can I do?", "❓", TemplateColor.question),
            T("Play with me", "Will you play with me?", "🧑‍🤝‍🧑", TemplateColor.social),
            T("Go outside", "Let's go outside", "🌳", TemplateColor.verb),
            T("Draw", "I want to draw", "🖍️", TemplateColor.verb),
            T("Build", "I want to build something", "🧱", TemplateColor.verb),
            T("Read", "Read with me", "📖", TemplateColor.verb),
            T("Music", "Put on music", "🎵", TemplateColor.verb),
            T("Dance", "Let's dance", "💃", TemplateColor.verb),
            T("Bake", "Let's bake something", "🧁", TemplateColor.verb),
            T("Tablet", "Can I have the tablet?", "📱", TemplateColor.noun),
            T("Nothing", "I don't want to do anything", "🙅", TemplateColor.negative)
        )),

        PageTemplate(id = "party", title = "Party & Birthday", summary = "For the days that are a big deal",
            category = "Fun & Silly", icon = "🎂", accent = "#E11D48", gridSize = 12, tiles = listOf(
            T("Happy birthday", "Happy birthday!", "🎂", TemplateColor.social),
            T("It's my birthday", "It is my birthday!", "🥳", TemplateColor.social),
            T("Presents", "Presents!", "🎁", TemplateColor.noun),
            T("Cake", "I want cake", "🍰", TemplateColor.noun),
            T("Ice cream", "Ice cream please", "🍦", TemplateColor.noun),
            T("Balloons", "Balloons", "🎈", TemplateColor.noun),
            T("Sing", "Sing the song!", "🎤", TemplateColor.verb),
            T("Blow candles", "I want to blow the candles", "🕯️", TemplateColor.verb),
            T("Thank you", "Thank you so much", "🙏", TemplateColor.social),
            T("Too loud", "It is too loud in here", "🔊", TemplateColor.negative),
            T("I need a break", "I need a quiet break", "⏸️", TemplateColor.negative),
            T("Best day", "This is the best day", "🌟", TemplateColor.descriptor)
        )),

        PageTemplate(id = "animals", title = "Animals", summary = "High-motivation board for early vocabulary",
            category = "Fun & Silly", icon = "🐶", accent = "#16A34A", gridSize = 16, tiles = listOf(
            T("Dog", "Dog", "🐶", TemplateColor.noun),
            T("Cat", "Cat", "🐱", TemplateColor.noun),
            T("Bird", "Bird", "🐦", TemplateColor.noun),
            T("Fish", "Fish", "🐟", TemplateColor.noun),
            T("Horse", "Horse", "🐴", TemplateColor.noun),
            T("Cow", "Cow", "🐮", TemplateColor.noun),
            T("Pig", "Pig", "🐷", TemplateColor.noun),
            T("Sheep", "Sheep", "🐑", TemplateColor.noun),
            T("Duck", "Duck", "🦆", TemplateColor.noun),
            T("Rabbit", "Rabbit", "🐰", TemplateColor.noun),
            T("Bear", "Bear", "🐻", TemplateColor.noun),
            T("Lion", "Lion", "🦁", TemplateColor.noun),
            T("Elephant", "Elephant", "🐘", TemplateColor.noun),
            T("Monkey", "Monkey", "🐵", TemplateColor.noun),
            T("I see a", "I see a", "👀", TemplateColor.verb),
            T("So cute", "That is so cute", "🥰", TemplateColor.descriptor)
        )),

        PageTemplate(id = "screen", title = "Shows & Games", summary = "Screen time, negotiated in words",
            category = "Fun & Silly", icon = "📺", accent = "#4338CA", gridSize = 12, tiles = listOf(
            T("Tablet", "Can I have the tablet?", "📱", TemplateColor.noun),
            T("TV", "Can we watch TV?", "📺", TemplateColor.noun),
            T("My show", "I want my show", "🎬", TemplateColor.noun),
            T("Game", "I want to play a game", "🎮", TemplateColor.noun),
            T("Next one", "Play the next one", "⏭️", TemplateColor.verb),
            T("Pause", "Pause please", "⏸️", TemplateColor.verb),
            T("Louder", "Turn it up", "🔊", TemplateColor.verb),
            T("Quieter", "Turn it down", "🔉", TemplateColor.verb),
            T("Watch with me", "Watch with me", "🧑‍🤝‍🧑", TemplateColor.social),
            T("Five more minutes", "Five more minutes please", "⏰", TemplateColor.negative),
            T("Scary", "This is too scary", "😨", TemplateColor.negative),
            T("All done", "I am all done", "🏁", TemplateColor.social)
        )),
    )
}
