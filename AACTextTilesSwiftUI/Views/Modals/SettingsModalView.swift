import SwiftUI

/// Settings for the adult setting the board up.
///
/// The Settings button on the home screen used to be `action: {}` — it looked
/// like a working control and did nothing at all. On an AAC app that is worse
/// than having no button, because a parent reasonably concludes something is
/// broken rather than absent.
public struct SettingsModalView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject public var store: AACStore

    @State private var confirmReset = false
    @State private var speechRate: Double = 0.5

    public init(store: AACStore) { self.store = store }

    public var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Speech"),
                        footer: Text("Slower speech is easier to follow, and is usually the right starting point for a child who is still learning the words.")) {
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text("Speaking speed")
                            Spacer()
                            Text(rateLabel).foregroundColor(.secondary)
                        }
                        Slider(value: $speechRate, in: 0.3...0.7, step: 0.05)
                            .onChange(of: speechRate) { newValue in
                                SpeechManager.shared.defaultRate = Float(newValue)
                                UserDefaults.standard.set(newValue, forKey: "aac.speechRate")
                            }
                    }
                    Button("Test the voice") {
                        SpeechManager.shared.speak("Hello. I can talk with this iPad.",
                                                   rate: Float(speechRate))
                    }
                }

                Section(header: Text("Board"),
                        footer: Text("Locking hides the editing controls so a child cannot rearrange the board by accident.")) {
                    Toggle("Lock editing", isOn: Binding(
                        get: { store.isLocked },
                        set: { locked in
                            store.isLocked = locked
                            if locked { store.isEditMode = false }
                        }
                    ))
                    HStack {
                        Text("Pages in this book")
                        Spacer()
                        Text("\(store.pages.count)").foregroundColor(.secondary)
                    }
                    HStack {
                        Text("Buttons filled in")
                        Spacer()
                        Text("\(store.pages.reduce(0) { $0 + $1.tiles.count })")
                            .foregroundColor(.secondary)
                    }
                    HStack {
                        Text("Talking spots")
                        Spacer()
                        Text("\(store.pages.reduce(0) { $0 + $1.hotspots.count })")
                            .foregroundColor(.secondary)
                    }
                }

                Section(header: Text("Danger zone"),
                        footer: Text("This erases every page, button and talking spot you have made and puts the original starter book back. It cannot be undone.")) {
                    Button("Reset to the starter book") { confirmReset = true }
                        .foregroundColor(.red)
                }
            }
            .navigationBarTitle("Settings", displayMode: .inline)
            .navigationBarItems(trailing: Button("Done") {
                store.saveNow()
                presentationMode.wrappedValue.dismiss()
            })
            .alert(isPresented: $confirmReset) {
                Alert(title: Text("Reset everything?"),
                      message: Text("Every page, button and talking spot you have made is erased and the starter book comes back."),
                      primaryButton: .destructive(Text("Reset")) {
                          store.resetToDefaults()
                          presentationMode.wrappedValue.dismiss()
                      },
                      secondaryButton: .cancel())
            }
        }
        .onAppear {
            let saved = UserDefaults.standard.double(forKey: "aac.speechRate")
            speechRate = saved > 0 ? saved : Double(SpeechManager.shared.defaultRate)
        }
    }

    private var rateLabel: String {
        switch speechRate {
        case ..<0.40: return "Slow"
        case ..<0.48: return "Fairly slow"
        case ..<0.56: return "Normal"
        case ..<0.64: return "Fairly fast"
        default:      return "Fast"
        }
    }
}
