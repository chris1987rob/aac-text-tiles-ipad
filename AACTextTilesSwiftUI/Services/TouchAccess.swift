import Foundation

/// Applies the touch-access settings to a press.
///
/// These exist for a child whose aim or grip is unsteady - not a switch user,
/// an ordinary touch user who currently triggers words they did not mean. A
/// resting hand, a sleeve dragging across the board, or a tremor turning one
/// intended press into five are the three failures this guards against, and
/// none of them were handled before.
public final class TouchAccess: ObservableObject {
    public static let shared = TouchAccess()

    private var lastFired: [String: Date] = [:]

    private init() {}

    /// Whether a press on `key` is allowed to speak right now.
    ///
    /// The lockout is per button rather than global on purpose: a child
    /// deliberately building "more … more … more" is a different thing from a
    /// tremor firing the same tile five times, and a global lock would block
    /// legitimate fast sequences across different words.
    public func shouldFire(key: String, lockout: Double, now: Date = Date()) -> Bool {
        guard lockout > 0 else { return true }
        if let previous = lastFired[key], now.timeIntervalSince(previous) < lockout {
            return false
        }
        lastFired[key] = now
        return true
    }

    /// Called when a board is left, so a stale timestamp cannot silently
    /// swallow the first press on a page returned to later.
    public func reset() {
        lastFired.removeAll()
    }
}
