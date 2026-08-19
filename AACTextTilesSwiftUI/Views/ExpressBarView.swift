import SwiftUI

public struct ExpressBarView: View {
    @ObservedObject public var store: AACStore

    public var body: some View {
        HStack(spacing: 8) {
            // Clickable Sentence Strip
            Button(action: {
                store.playExpressSentence()
            }) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        if store.expressChips.isEmpty {
                            Text("Tap tiles to build a sentence...")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(Color(hex: "#94A3B8"))
                        } else {
                            ForEach(Array(store.expressChips.enumerated()), id: \.offset) { _, chip in
                                Text(chip)
                                    .font(.system(size: 17, weight: .bold))
                                    .foregroundColor(Color(hex: "#006853"))
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(Color(hex: "#E6F4EA"))
                                    .cornerRadius(12)
                            }
                        }
                    }
                    .padding(.horizontal, 10)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .buttonStyle(PlainButtonStyle())

            // Luggage Tag Red Clear Button
            Button(action: {
                store.clearExpressChips()
            }) {
                Image(systemName: "xmark")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 44, height: 44)
                    .background(Color(hex: "#D32F2F"))
                    .cornerRadius(8)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.1), radius: 3, y: 2)
        .padding(.horizontal, 14)
        .padding(.top, 8)
    }
}
