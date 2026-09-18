import SwiftUI

/// Asks for the child-lock PIN before letting anyone into the editor.
///
/// The lock exists to stop a five-year-old rearranging a board they rely on,
/// not to resist someone determined who is holding the iPad. It is deliberately
/// a four-digit PIN and nothing more; dressing it up as security would promise
/// a protection it does not give.
public struct PinPromptView: View {
    @Environment(\.presentationMode) private var presentationMode
    public let expected: String
    public let onUnlocked: () -> Void

    @State private var entered: String = ""
    @State private var wrong: Bool = false

    public init(expected: String, onUnlocked: @escaping () -> Void) {
        self.expected = expected
        self.onUnlocked = onUnlocked
    }

    public var body: some View {
        NavigationView {
            VStack(spacing: 22) {
                Image(systemName: "lock.fill")
                    .font(.system(size: 42))
                    .foregroundColor(Color(hex: "#008369"))
                    .padding(.top, 32)

                Text("Enter the PIN to edit")
                    .font(.system(size: 22, weight: .bold))

                Text("This keeps the board safe from accidental changes.")
                    .font(.system(size: 15))
                    .foregroundColor(Color(hex: "#64748B"))
                    .multilineTextAlignment(.center)

                HStack(spacing: 14) {
                    ForEach(0..<4, id: \.self) { i in
                        Circle()
                            .fill(i < entered.count ? Color(hex: "#008369") : Color(hex: "#E2E8F0"))
                            .frame(width: 18, height: 18)
                    }
                }
                .padding(.vertical, 4)

                if wrong {
                    Text("That PIN is not right. Try again.")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(Color(hex: "#B91C1C"))
                }

                keypad

                Spacer(minLength: 0)
            }
            .padding(.horizontal, 24)
            .navigationBarTitle("Locked", displayMode: .inline)
            .navigationBarItems(trailing: Button("Cancel") {
                presentationMode.wrappedValue.dismiss()
            })
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }

    private var keypad: some View {
        VStack(spacing: 12) {
            ForEach([["1","2","3"], ["4","5","6"], ["7","8","9"], ["", "0", "⌫"]], id: \.self) { row in
                HStack(spacing: 12) {
                    ForEach(row, id: \.self) { key in
                        if key.isEmpty {
                            Color.clear.frame(width: 74, height: 58)
                        } else {
                            Button(action: { press(key) }) {
                                Text(key)
                                    .font(.system(size: 24, weight: .bold))
                                    .foregroundColor(Color(hex: "#1E293B"))
                                    .frame(width: 74, height: 58)
                                    .background(Color(hex: "#F1F5F9"))
                                    .cornerRadius(10)
                            }
                            .buttonStyle(PlainButtonStyle())
                        }
                    }
                }
            }
        }
        .frame(maxWidth: 300)
    }

    private func press(_ key: String) {
        wrong = false
        if key == "⌫" {
            if !entered.isEmpty { entered.removeLast() }
            return
        }
        guard entered.count < 4 else { return }
        entered += key
        guard entered.count == 4 else { return }

        if entered == expected {
            onUnlocked()
            presentationMode.wrappedValue.dismiss()
        } else {
            wrong = true
            entered = ""
        }
    }
}
