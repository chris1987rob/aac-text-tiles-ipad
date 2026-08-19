import SwiftUI

public struct KeyboardPageView: View {
    @ObservedObject public var store: AACStore
    @State private var typedText: String = ""

    private let quickPhrases = ["I want", "Yes", "No", "Please", "Help", "Thank you", "More", "Stop", "I like"]
    private let row1 = ["Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"]
    private let row2 = ["A", "S", "D", "F", "G", "H", "J", "K", "L"]
    private let row3 = ["Z", "X", "C", "V", "B", "N", "M"]

    public var body: some View {
        VStack(spacing: 12) {
            // Quick Prediction Phrase Strip
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(quickPhrases, id: \.self) { phrase in
                        Button(action: {
                            typedText += phrase + " "
                        }) {
                            Text(phrase)
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(Color(hex: "#006853"))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(Color(hex: "#E6F4EA"))
                                .cornerRadius(18)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 18)
                                        .stroke(Color(hex: "#008369"), lineWidth: 1)
                                )
                        }
                    }
                }
                .padding(.horizontal, 14)
            }

            // Typing Display Bar & Actions
            HStack(spacing: 10) {
                Text(typedText.isEmpty ? "Tap keys to type and speak..." : typedText)
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundColor(typedText.isEmpty ? Color(hex: "#94A3B8") : Color(hex: "#1E293B"))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                    .background(Color.white)
                    .cornerRadius(12)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color(hex: "#CBD5E1"), lineWidth: 1)
                    )

                Button(action: { typedText = "" }) {
                    Text("✕ Clear")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 12)
                        .background(Color(hex: "#D32F2F"))
                        .cornerRadius(10)
                }

                Button(action: {
                    if !typedText.isEmpty {
                        typedText.removeLast()
                    }
                }) {
                    Text("⌫ Backspace")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(Color(hex: "#1E293B"))
                        .padding(.horizontal, 14)
                        .padding(.vertical, 12)
                        .background(Color(hex: "#E2E8F0"))
                        .cornerRadius(10)
                }

                Button(action: {
                    SpeechManager.shared.speak(typedText)
                }) {
                    HStack(spacing: 6) {
                        Image(systemName: "play.fill")
                        Text("Speak")
                    }
                    .font(.system(size: 17, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 12)
                    .background(Color(hex: "#00A86B"))
                    .cornerRadius(10)
                }
            }
            .padding(.horizontal, 14)

            // QWERTY Keyboard
            VStack(spacing: 8) {
                // Row 1
                HStack(spacing: 6) {
                    ForEach(row1, id: \.self) { key in
                        keyButton(key)
                    }
                }
                // Row 2
                HStack(spacing: 6) {
                    ForEach(row2, id: \.self) { key in
                        keyButton(key)
                    }
                }
                // Row 3
                HStack(spacing: 6) {
                    ForEach(row3, id: \.self) { key in
                        keyButton(key)
                    }
                }
                // Space Bar
                HStack {
                    Button(action: { typedText += " " }) {
                        Text("Space")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(Color(hex: "#1E293B"))
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(Color.white)
                            .cornerRadius(8)
                            .shadow(color: Color.black.opacity(0.1), radius: 2, y: 1)
                    }
                }
                .padding(.horizontal, 80)
            }
            .padding(.horizontal, 14)
            .padding(.bottom, 10)
        }
        .padding(.vertical, 10)
    }

    private func keyButton(_ key: String) -> some View {
        Button(action: {
            typedText += key
        }) {
            Text(key)
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(Color(hex: "#1E293B"))
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(Color.white)
                .cornerRadius(8)
                .shadow(color: Color.black.opacity(0.12), radius: 2, y: 1)
        }
    }
}
