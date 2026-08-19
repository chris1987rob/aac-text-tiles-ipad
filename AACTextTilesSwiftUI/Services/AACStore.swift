import Foundation
import SwiftUI

public class AACStore: ObservableObject {
    @Published public var pages: [PageModel] = []
    @Published public var currentPageIndex: Int = 0
    @Published public var isEditMode: Bool = false
    @Published public var isLocked: Bool = false
    @Published public var expressChips: [String] = []
    @Published public var customTemplates: [PageModel] = []

    public var currentPage: PageModel {
        get {
            guard !pages.isEmpty, currentPageIndex >= 0, currentPageIndex < pages.count else {
                return PageModel(title: "Default")
            }
            return pages[currentPageIndex]
        }
        set {
            if !pages.isEmpty, currentPageIndex >= 0, currentPageIndex < pages.count {
                pages[currentPageIndex] = newValue
                save()
            }
        }
    }

    public init() {
        loadPages()
    }

    public func loadPages() {
        // Built-in starter communication book
        self.pages = [
            // Page 1: Colors 4-Grid
            PageModel(
                title: "Colors",
                type: .grid,
                gridSize: 4,
                bgHex: "#FFFFFF",
                express: false,
                tiles: [
                    1: TileModel(id: 1, label: "Red", tts: "Red", bgHex: "#FF4D4D", borderHex: "#D32F2F", labelHex: "#FFFFFF"),
                    2: TileModel(id: 2, label: "Orange", tts: "Orange", bgHex: "#FFA500", borderHex: "#E65100", labelHex: "#FFFFFF"),
                    3: TileModel(id: 3, label: "Yellow", tts: "Yellow", bgHex: "#FFEB3B", borderHex: "#FBC02D", labelHex: "#1E293B"),
                    4: TileModel(id: 4, label: "Green", tts: "Green", bgHex: "#4CAF50", borderHex: "#2E7D32", labelHex: "#FFFFFF")
                ]
            ),
            // Page 2: Yes / No 2-Grid
            PageModel(
                title: "Yes / No",
                type: .grid,
                gridSize: 2,
                bgHex: "#FFFFFF",
                express: false,
                tiles: [
                    1: TileModel(id: 1, label: "YES", tts: "Yes", symbolName: "yes", bgHex: "#C8E6C9", borderHex: "#2E7D32", labelHex: "#1B5E20", labelSize: 1.4),
                    2: TileModel(id: 2, label: "NO", tts: "No", symbolName: "no", bgHex: "#FFCDD2", borderHex: "#C62828", labelHex: "#B71C1C", labelSize: 1.4)
                ]
            ),
            // Page 3: Core 9-Grid (Express Mode)
            PageModel(
                title: "Core Words",
                type: .grid,
                gridSize: 9,
                bgHex: "#FFFFFF",
                express: true,
                tiles: [
                    1: TileModel(id: 1, label: "I want", tts: "I want", symbolName: "help", bgHex: "#E1BEE7", borderHex: "#8E24AA", labelHex: "#4A148C"),
                    2: TileModel(id: 2, label: "Eat", tts: "Eat food", symbolName: "eat", bgHex: "#C8E6C9", borderHex: "#388E3C", labelHex: "#1B5E20"),
                    3: TileModel(id: 3, label: "Drink", tts: "Drink water", symbolName: "water", bgHex: "#BBDEFB", borderHex: "#1976D2", labelHex: "#0D47A1"),
                    4: TileModel(id: 4, label: "Help", tts: "Please help me", symbolName: "help", bgHex: "#FFF59D", borderHex: "#FBC02D", labelHex: "#1E293B"),
                    5: TileModel(id: 5, label: "Play", tts: "Play games", symbolName: "play", bgHex: "#FFE0B2", borderHex: "#F57C00", labelHex: "#E65100"),
                    6: TileModel(id: 6, label: "Bathroom", tts: "I need to go to the bathroom", symbolName: "bathroom", bgHex: "#D1C4E9", borderHex: "#5E35B1", labelHex: "#311B92"),
                    7: TileModel(id: 7, label: "More", tts: "More please", symbolName: "more", bgHex: "#C8E6C9", borderHex: "#388E3C", labelHex: "#1B5E20"),
                    8: TileModel(id: 8, label: "Stop", tts: "Stop now", symbolName: "stop", bgHex: "#FFCDD2", borderHex: "#D32F2F", labelHex: "#B71C1C"),
                    9: TileModel(id: 9, label: "Happy", tts: "I feel happy", symbolName: "happy", bgHex: "#FFF9C4", borderHex: "#FBC02D", labelHex: "#1E293B")
                ]
            ),
            // Page 4: Visual Scene Living Room
            PageModel(
                title: "Living Room Scene",
                type: .scene,
                gridSize: 4,
                bgHex: "#FFFFFF",
                scenePresetKey: "living-room",
                hotspots: [
                    HotspotModel(id: 1, x: 18, y: 50, w: 46, h: 36, label: "Sofa / Couch", tts: "I want to sit on the couch and relax."),
                    HotspotModel(id: 2, x: 66, y: 20, w: 28, h: 35, label: "Television", tts: "Can we turn on the TV to watch a show?"),
                    HotspotModel(id: 3, x: 28, y: 64, w: 18, h: 18, label: "Sleeping Cat", tts: "Look at the cute orange cat sleeping!"),
                    HotspotModel(id: 4, x: 5, y: 35, w: 14, h: 50, label: "Lamp Light", tts: "Please turn on the lamp light."),
                    HotspotModel(id: 5, x: 21, y: 12, w: 31, h: 35, label: "Window", tts: "Look out the window, it is nice outside.")
                ]
            ),
            // Page 5: Talking Keyboard Page
            PageModel(
                title: "Talking Keyboard",
                type: .keyboard,
                gridSize: 1,
                bgHex: "#F8FAFC"
            )
        ]
    }

    public func nextPage() {
        if currentPageIndex < pages.count - 1 {
            currentPageIndex += 1
        } else {
            currentPageIndex = 0
        }
    }

    public func prevPage() {
        if currentPageIndex > 0 {
            currentPageIndex -= 1
        } else {
            currentPageIndex = pages.count - 1
        }
    }

    public func addExpressChip(_ chip: String) {
        expressChips.append(chip)
    }

    public func clearExpressChips() {
        expressChips.removeAll()
    }

    public func playExpressSentence() {
        let sentence = expressChips.joined(separator: " ")
        if !sentence.isEmpty {
            SpeechManager.shared.speak(sentence)
        }
    }

    public func save() {
        // Persist to UserDefaults or local JSON
    }
}
