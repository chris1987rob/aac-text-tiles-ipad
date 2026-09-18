import Foundation

/// Settings that belong to the app rather than to one page, saved beside the
/// board so they survive a relaunch.
///
/// Before this existed, "Lock from edits" lived on the store as a plain
/// property: nothing read it and nothing wrote it to disk, so it reset to off
/// on every launch. A setting a parent flips before handing over the iPad has
/// to outlive the app being closed.
/// Which bundled picture set the symbol picker offers.
///
/// Two sets ship in the app. The Mulberry set is a licensed third-party
/// library (CC BY-SA 2.0 UK); the Talk Tiles set is our own pictures, drawn
/// in-house, with a recorded voice clip behind every one of them. A board
/// can mix both - the choice here only decides which set the picker searches
/// and which one the "Choose a Symbol" button opens on.
public enum SymbolSet: String, Codable, CaseIterable, Identifiable {
    case talkTiles = "talktiles"
    case mulberry  = "mulberry"

    public var id: String { rawValue }

    public var title: String {
        switch self {
        case .talkTiles: return "Talk Tiles pictures"
        case .mulberry:  return "Mulberry Symbols"
        }
    }

    public var blurb: String {
        switch self {
        case .talkTiles: return "Our own pictures. Every one has Bella's voice behind it."
        case .mulberry:  return "Mulberry Symbols, a licensed third-party set. Spoken by the iPad voice."
        }
    }
}

public struct AppSettings: Codable, Equatable {

    // MARK: Voice

    /// `AVSpeechSynthesisVoice.identifier`, or `SpokenText.bellaVoiceId` for
    /// the app's own recorded voice. Nil means "whatever the system gives us
    /// for English", which is what the app always used to do.
    public var voiceId: String? = SpokenText.bellaVoiceId
    public var speechRate: Double = 0.45

    // MARK: Pictures

    /// The set the symbol picker opens on. See `SymbolSet`.
    public var symbolSet: SymbolSet = .talkTiles

    // MARK: Child lock

    /// Hides every route into the editor. Getting back in needs the PIN.
    public var childLock: Bool = false

    /// Deliberately plain text. This exists to stop a child wandering into the
    /// editor, not to resist someone who has the iPad in hand — storing it in
    /// the Keychain would imply a level of protection this does not provide.
    public var lockPIN: String = "1234"

    // MARK: Touch access

    /// Seconds a button must be held before it counts. Zero is a normal tap.
    /// Above zero this is dwell activation: a hand resting on the screen, or a
    /// sleeve brushing it, no longer speaks.
    public var activationDelay: Double = 0

    /// Seconds before the same button will fire again. Stops a tremor turning
    /// one intended press into five.
    public var repeatLockout: Double = 0

    /// Fire when the finger lifts rather than when it lands, so a child can
    /// slide onto the right button before committing to it.
    public var activateOnRelease: Bool = false

    public init() {}

    // MARK: Decoding

    /// Every key is optional on the way in. A settings file written before a
    /// field existed (symbolSet arrived after voiceId, and more will follow)
    /// must still load - with synthesised decoding one missing key threw the
    /// whole file away, and with it the PIN, the voice and the child lock.
    /// The same goes for a book backup made on an older version.
    private enum CodingKeys: String, CodingKey {
        case voiceId, speechRate, symbolSet, childLock, lockPIN
        case activationDelay, repeatLockout, activateOnRelease
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        let base = AppSettings()
        // "System default" is stored as an explicit null, so it survives a
        // reload. A file with no key at all predates Bella and gets her.
        voiceId           = c.contains(.voiceId)
                          ? try c.decodeIfPresent(String.self, forKey: .voiceId) : base.voiceId
        speechRate        = try c.decodeIfPresent(Double.self,    forKey: .speechRate) ?? base.speechRate
        // A set name this version does not know (a file from a newer build)
        // falls back to the default rather than rejecting the whole file.
        symbolSet         = (try c.decodeIfPresent(String.self, forKey: .symbolSet))
                                .flatMap(SymbolSet.init(rawValue:)) ?? base.symbolSet
        childLock         = try c.decodeIfPresent(Bool.self,      forKey: .childLock) ?? base.childLock
        lockPIN           = try c.decodeIfPresent(String.self,    forKey: .lockPIN) ?? base.lockPIN
        activationDelay   = try c.decodeIfPresent(Double.self,    forKey: .activationDelay) ?? base.activationDelay
        repeatLockout     = try c.decodeIfPresent(Double.self,    forKey: .repeatLockout) ?? base.repeatLockout
        activateOnRelease = try c.decodeIfPresent(Bool.self,      forKey: .activateOnRelease) ?? base.activateOnRelease
    }

    public func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(voiceId, forKey: .voiceId)   // nil -> null, never omitted
        try c.encode(speechRate, forKey: .speechRate)
        try c.encode(symbolSet, forKey: .symbolSet)
        try c.encode(childLock, forKey: .childLock)
        try c.encode(lockPIN, forKey: .lockPIN)
        try c.encode(activationDelay, forKey: .activationDelay)
        try c.encode(repeatLockout, forKey: .repeatLockout)
        try c.encode(activateOnRelease, forKey: .activateOnRelease)
    }
}
