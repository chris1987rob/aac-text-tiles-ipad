import SwiftUI

/// The board's look, in one place. Pastel and rounded: white top bar with round
/// buttons, a light-blue sentence pill, soft tiles on a pale ground. Introduced
/// 2026-09-13 from a reference picture the user supplied; every hex below is a
/// sample from it.
public enum BoardTheme {
    /// The ground the board sits on.
    public static let background = Color(hex: "#F4F7FB")
    /// The top bar.
    public static let bar = Color(hex: "#FFFFFF")
    /// Headline text: page names, sentence words, tile labels.
    public static let ink = Color(hex: "#1F2A44")
    /// Secondary text and icons on white.
    public static let inkSoft = Color(hex: "#5B6478")
    /// The orange Home / New Page circle.
    public static let accent = Color(hex: "#F5893B")
    /// The sentence pill.
    public static let sentence = Color(hex: "#D8ECFA")
    /// The grey circle the keyboard's backspace sits in.
    public static let chip = Color(hex: "#E4E9F0")
    /// The red X that clears the sentence.
    public static let clear = Color(hex: "#E5484D")
    /// The Speak Now ring.
    public static let rainbow = AngularGradient(
        gradient: Gradient(colors: [
            Color(hex: "#FF7A7A"), Color(hex: "#FFB35C"), Color(hex: "#FFE66D"),
            Color(hex: "#7AE582"), Color(hex: "#5CC8FF"), Color(hex: "#A78BFA"),
            Color(hex: "#FF7A7A")
        ]),
        center: .center
    )
}

/// A round white bar button with a soft shadow. Every control on the top bar
/// is one of these so the bar reads as one family.
public struct RoundBarButton: View {
    public let icon: String
    public var fill: Color = BoardTheme.bar
    public var tint: Color = BoardTheme.inkSoft
    public var size: CGFloat = 46
    public let label: String
    public let action: () -> Void

    public init(icon: String, fill: Color = BoardTheme.bar, tint: Color = BoardTheme.inkSoft,
                size: CGFloat = 46, label: String, action: @escaping () -> Void) {
        self.icon = icon; self.fill = fill; self.tint = tint; self.size = size
        self.label = label; self.action = action
    }

    public var body: some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: size * 0.42, weight: .bold))
                .foregroundColor(tint)
                .frame(width: size, height: size)
                .background(fill)
                .clipShape(Circle())
                .shadow(color: Color.black.opacity(0.10), radius: 4, y: 2)
                .contentShape(Circle())
        }
        .buttonStyle(PlainButtonStyle())
        .accessibilityLabel(label)
    }
}

/// The round "PLAY" button with the rainbow ring.
public struct SpeakNowButton: View {
    public var size: CGFloat = 50
    public let action: () -> Void

    public init(size: CGFloat = 50, action: @escaping () -> Void) {
        self.size = size; self.action = action
    }

    public var body: some View {
        Button(action: action) {
            VStack(spacing: 1) {
                Image(systemName: "play.fill")
                    .font(.system(size: size * 0.28, weight: .bold))
                    .foregroundColor(BoardTheme.ink)
                Text("PLAY")
                    .font(.system(size: size * 0.17, weight: .heavy))
                    .foregroundColor(BoardTheme.ink)
            }
            .frame(width: size, height: size)
            .background(Color.white)
            .clipShape(Circle())
            .overlay(Circle().stroke(BoardTheme.rainbow, lineWidth: size * 0.075))
            .shadow(color: Color.black.opacity(0.12), radius: 5, y: 3)
            .contentShape(Circle())
        }
        .buttonStyle(PlainButtonStyle())
        .accessibilityLabel("Play")
    }
}

/// The light-blue pill the words collect in: words spaced out on the
/// left, then the trailing controls, then the red X at the far right end -
/// the X sits on the far side of Play Now so a hand reaching for Play does not
/// wipe the sentence. Shared by the board's sentence bar and the symbol
/// keyboard's, so they look the same.
public struct SentencePill<Trailing: View>: View {
    public let words: [String]
    public let placeholder: String
    public let onClear: () -> Void
    public let onTap: () -> Void
    public let trailing: Trailing

    public init(words: [String], placeholder: String, onClear: @escaping () -> Void,
                onTap: @escaping () -> Void, @ViewBuilder trailing: () -> Trailing) {
        self.words = words; self.placeholder = placeholder
        self.onClear = onClear; self.onTap = onTap; self.trailing = trailing()
    }

    public var body: some View {
        HStack(spacing: 10) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 0) {
                    if words.isEmpty {
                        Text(placeholder)
                            .font(.system(size: 19, weight: .semibold))
                            .foregroundColor(BoardTheme.inkSoft.opacity(0.7))
                    } else {
                        Text(words.joined(separator: "  "))
                            .font(.system(size: 26, weight: .bold))
                            .foregroundColor(BoardTheme.ink)
                            .lineLimit(1)
                    }
                }
                .frame(minHeight: 44)
                .padding(.leading, 18)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentShape(Rectangle())
            .onTapGesture(perform: onTap)

            trailing

            RoundBarButton(icon: "xmark", fill: BoardTheme.clear, tint: .white,
                           size: 44, label: "Clear sentence", action: onClear)
                .padding(.trailing, 8)
        }
        .padding(.vertical, 5)
        .background(BoardTheme.sentence)
        .cornerRadius(27)
    }
}

/// The sentence bar: words collect here as tiles are pressed, Speak Now speaks
/// them, and the X clears them.
///
/// The strip used to be a `Button` wrapping the `ScrollView`, which is the one
/// place SwiftUI is allowed to swallow touches for its own scrolling. The
/// controls are plain buttons beside it now, with an explicit hit shape, so
/// nothing about the strip's content can decide whether X works.
public struct ExpressBarView: View {
    @ObservedObject public var store: AACStore

    public var body: some View {
        SentencePill(
            words: store.expressChips,
            placeholder: "Tap tiles to build a sentence",
            onClear: { store.clearExpressChips() },
            onTap: { store.playExpressSentence() }
        ) {
            SpeakNowButton { store.playExpressSentence() }
        }
        .padding(.horizontal, 14)
        .padding(.top, 8)
    }
}
