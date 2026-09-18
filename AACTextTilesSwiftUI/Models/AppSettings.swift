import Foundation

/// Settings that belong to the app rather than to one page, saved beside the
/// board so they survive a relaunch.
///
/// Before this existed, "Lock from edits" lived on the store as a plain
/// property: nothing read it and nothing wrote it to disk, so it reset to off
/// on every launch. A setting a parent flips before handing over the iPad has
/// to outlive the app being closed.
public struct AppSettings: Codable, Equatable {

    // MARK: Voice

    /// `AVSpeechSynthesisVoice.identifier`. Nil means "whatever the system
    /// gives us for English", which is what the app always used to do.
    public var voiceId: String? = nil
    public var speechRate: Double = 0.45

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
}
