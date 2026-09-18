import SwiftUI

/// The in-app guide. Rewritten 2026-09-13 to describe the app as it actually
/// is - the old text still talked about a bottom toolbar, arrows and a
/// "layers" button that no longer exist. When a screen changes, change its
/// paragraph here in the same commit.
public struct HelpGuideModalView: View {
    @Environment(\.presentationMode) var presentationMode

    private struct Topic {
        let icon: String
        let color: String
        let title: String
        let body: String
    }

    private let topics: [Topic] = [
        Topic(icon: "play.fill", color: "#F5893B", title: "Player", body:
            "Player is the screen a child uses. Tap a tile and it speaks. On the top bar, the round arrow steps back one page, the orange house returns to this menu, and tapping the page name opens the list of every page in the book so you can jump straight to one. Nothing on the Player screen can change a page by accident."),
        Topic(icon: "text.bubble.fill", color: "#4DB2FF", title: "The sentence bar", body:
            "Pages with the sentence bar switched on collect words as tiles are tapped - \"I want + Eat + Pizza\". Press the round PLAY button to hear the whole sentence, or tap the words themselves. The red X clears it. Turn the bar on or off for any page in Page Options, under Behavior."),
        Topic(icon: "pencil", color: "#4DB2FF", title: "Page Editor", body:
            "Page Editor is the same board with editing switched on. Tap any tile - filled or empty - to open the button editor. On the top bar: the arrow steps back a page, the house goes home, the sliders open Page Options, and the orange + starts a new page. Tap the page name to rename it right there; the small arrow next to it opens the page list."),
        Topic(icon: "square.grid.2x2.fill", color: "#2EC98A", title: "Editing a button", body:
            "Two boxes at the top: \"On the button\" is the word shown on the tile, \"Voice says\" is what is spoken - leave it empty and the voice reads the button's word. Below that: Play Preview, record your own voice, a photo from the camera or library, 3,400 picture symbols to search, button and text colours, and word size. Saved Buttons keeps a finished button so you can drop it onto any page later."),
        Topic(icon: "plus", color: "#F5893B", title: "New pages and page kinds", body:
            "The + button opens the New Page Wizard. Give the page a name and pick a kind. A Standard Grid is a board of buttons - choose how many (1 to 48) and start blank or from a ready-made board. A Visual Scene Display is a photo with talking spots on it. A Symbol Keyboard is a keyboard made of pictures instead of letters, grouped as People, Actions, Describing, Things and so on - press pictures to build a sentence."),
        Topic(icon: "photo.fill", color: "#9B7BFF", title: "Visual scenes", body:
            "A scene page shows a photo - the living room, the playground - with invisible talking spots over things in it. In the editor, Add Hotspot puts a new spot on the picture; drag it into place and drag its corner handle to size it. Tap a spot to name it and choose what it says. The picture itself is changed in Page Options."),
        Topic(icon: "slider.horizontal.3", color: "#4DB2FF", title: "Page Options", body:
            "The sliders button in the editor. Rename the page, pick its background colour, change how many buttons a grid has, switch the sentence bar on, and Share This Page as a file another Talk Tiles iPad can add to its book."),
        Topic(icon: "arrow.down.to.line", color: "#2EC98A", title: "Downloads", body:
            "Ready-made boards - food, feelings, school, bedtime and more - each with pictures already on every button. Tap Add to Book and the board appears as a new page you can then change however you like."),
        Topic(icon: "speaker.wave.2.fill", color: "#9B7BFF", title: "Voice and touch", body:
            "In Settings you can choose the voice and how fast it speaks, and test it. Under Touch: Hold to speak makes a tile wait until the finger has rested on it, so a brushing hand does not talk; Pause before repeat stops one press landing five times; Speak when the finger lifts waits for the release."),
        Topic(icon: "lock.fill", color: "#FF6B6B", title: "Child Lock", body:
            "Lock editing in Settings hides every way into the Page Editor behind a PIN, so the board cannot be changed by the child using it. To stop the child leaving Talk Tiles altogether, use the iPad's own Guided Access: Settings > Accessibility > Guided Access, then triple-click the top button while Talk Tiles is open."),
        Topic(icon: "externaldrive.fill", color: "#FFC93C", title: "Backing up", body:
            "Settings > Backup writes the whole book - every page, button, photo and recording - to one file you can email to yourself or keep in Files. Restore reads it back onto any iPad. Do this whenever you have spent real time building, and always before changing iPads. Reset to the starter book, at the bottom of Settings, erases everything and cannot be undone."),
    ]

    public var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("TALK TILES GUIDE")
                            .font(.system(size: 24, weight: .bold))
                            .tracking(2.5)
                            .foregroundColor(BoardTheme.ink)
                        Text("How each part of the app works, for parents, teachers and therapists.")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(BoardTheme.inkSoft)
                    }
                    .padding(18)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(BoardTheme.sentence)
                    .cornerRadius(22)

                    ForEach(topics.indices, id: \.self) { i in
                        topicCard(topics[i])
                    }
                }
                .padding()
            }
            .background(BoardTheme.background.edgesIgnoringSafeArea(.all))
            .navigationBarTitle("Help", displayMode: .inline)
            .navigationBarItems(trailing: Button("Done") { presentationMode.wrappedValue.dismiss() })
        }
        // On iPad a bare NavigationView defaults to the split-view style, so
        // inside a sheet it renders as a sidebar next to an empty detail pane
        // instead of one plain form. Every modal in this app is a single
        // column and must say so.
        .navigationViewStyle(StackNavigationViewStyle())
    }

    private func topicCard(_ topic: Topic) -> some View {
        HStack(alignment: .top, spacing: 14) {
            Image(systemName: topic.icon)
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(.white)
                .frame(width: 44, height: 44)
                .background(Color(hex: topic.color))
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 6) {
                Text(topic.title)
                    .font(.system(size: 19, weight: .bold))
                    .foregroundColor(BoardTheme.ink)
                Text(topic.body)
                    .font(.system(size: 16))
                    .foregroundColor(Color(hex: "#334155"))
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .cornerRadius(20)
        .shadow(color: Color.black.opacity(0.05), radius: 5, y: 2)
    }
}
