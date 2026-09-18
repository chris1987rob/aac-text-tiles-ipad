# AAC Text Tiles iPad

**AAC Text Tiles iPad** is a full-featured, pixel-faithful GoTalk Now style AAC (Augmentative and Alternative Communication) app built specifically for **Apple iPad / iPadOS**.

It operates **100% offline** on the iPad using Service Worker CacheStorage and IndexedDB, featuring an original in-house library of 550+ AAC symbol pictures (no third-party symbol licence), interactive Visual Scene Displays with sound hotspots, talking keyboards, sentence building, and the GoTalk 7.0 Quick Edit suite.

---

## 🌟 iPad Capabilities & Features

1. **Standard Button Grids (1–48 buttons)**:
   - Flexible layouts: 1, 2, 4, 9, 12, 16, 25, 36, or 48 buttons per page.
   - Dynamic label auto-fitting and font scaling (never clips or runs off buttons).
   - High contrast neon green border audio feedback (`#00e676`).

2. **Visual Scene Displays (VSDs)**:
   - Full-bleed photo or illustrated background scenes.
   - Interactive touch hotspots that play speech or recorded audio when tapped.
   - Hotspot editor to resize, reposition, and customize touch zones.
   - Built-in ready AAC scenes (*Living Room*, *Classroom*, *Playground*, *Kitchen*).

3. **Talking Keyboard Page**:
   - Full-screen QWERTY & ABC talking keyboard.
   - Word prediction chips (*"I want"*, *"Yes"*, *"No"*, *"Please"*, *"Help"*, *"Thank you"*, *"More"*, *"Stop"*).
   - High-fidelity text-to-speech engine.

4. **Symbol & Photo Library (550+ original pictures)**:
   - Every picture was generated in-house (Qwen-Image on our own GPU) in one consistent flat-vector style; nothing is licensed from a third party.
   - 16 categories with filter chips and section headers, ranked search, and a 🔊 button on every card to hear the word.
   - Instant search and category taxonomy chips (Core, Food, Feelings, Actions, Places, Health).
   - Live iPad camera capture and photo roll uploads.

5. **Express Sentence Builder**:
   - Top sentence bar accumulating tile label chips dynamically.
   - 1-tap full sentence speech playback with clear button.

6. **Sound It Out Phonics (GoTalk 7.0)**:
   - Syllable segmentation with animated voice playback.

7. **All 8 New Page Tools**:
   - Page Wizard, Online Template Gallery, Keyboard Page, My Templates, Import/Export (JSON & OBF), Duplicate Page, Blank Scene, and Blank Button Page.

---

## 📱 Step-by-Step iPad Installation Options

### Option 1: Native Xcode Build via MacBook Pro (Recommended for Native App)

1. **Clone this repository on your MacBook Pro**:
   ```bash
   git clone https://github.com/chris1987rob/aac-text-tiles-ipad.git
   cd aac-text-tiles-ipad
   ```
2. **Open Either Native Xcode Project**:
   * **Pure Native Apple SwiftUI**: `open AACTextTilesSwiftUI.xcodeproj` (Modern Apple SwiftUI + AVFoundation)
   * **Native Swift Container**: `open AACTextTilesiPad.xcodeproj` (Swift + WebKit bundle with the symbol pictures)
3. **Configure Free Signing**:
   * Select the project in the left pane -> **Signing & Capabilities**.
   * Under **Team**, select your Apple ID Personal Team.
4. **Connect iPad & Run**:
   * Plug your iPad into your MacBook Pro (or select iPad Simulator).
   * Press **`⌘R`** (Command + R) or tap the **Play (▶)** button.
   * The app compiles and installs directly onto your iPad!

*See [XCODE_GUIDE.md](XCODE_GUIDE.md) for full detailed step-by-step instructions.*

#### Pictures and voices in the SwiftUI app

The native app ships **two picture sets** and picks between them under
**Settings › Pictures** (the symbol picker also has a switch at the top):

| Set | Where | Stored on a button as | Spoken by |
|---|---|---|---|
| **Talk Tiles pictures** (default) — 2,210 originals drawn in-house | `AACTextTilesSwiftUI/TalkTilesSymbols/` + `catalog.json` | `tt:<id>` | Bella's recorded clip |
| **Mulberry Symbols** — 3,436, CC BY-SA 2.0 UK | `AACTextTilesSwiftUI/Symbols/` | `<name>` | the iPad voice |

**Bella** is the app's own recorded voice (`AACTextTilesSwiftUI/Voices/bella/`,
2,210 word clips + 74 board phrases, `index.json` keyed by the normalised
phrase). She is the default under **Settings › Voice**; a button whose words
match a clip plays it, a sentence made only of clip words is chained, and
anything else falls back to the iPad voice. A recording on the button always
wins.

Both folders are generated — never edit them by hand. After `aac-board`'s
`symbol_gen/write_symbols_data.py` refreshes `symbols_data.js` and `symbols/`
here, run:

```bash
python3 Tools/sync-native-assets.py                 # pictures + Bella
python3 Tools/sync-native-assets.py --voices bella,jake,maya
```

---

### Option 2: Safari PWA "Add to Home Screen" (No Mac Required)

1. **Start the local delivery server on this computer**:
   ```bash
   cd /home/mike/aac-text-tiles-ipad
   python3 serve.py
   ```
2. **On your iPad** (connected to the same local Wi-Fi):
   * Open **Safari** and navigate to the printed address: `http://<YOUR-IP>:8080`
   * Tap the **"Download Local Security Certificate"** button.
   * Open iPad **Settings → Profile Downloaded → Install**.
   * Go to **Settings → General → About → Certificate Trust Settings** and toggle trust **ON**.
3. **Install as Standalone App**:
   * Navigate to `https://<YOUR-IP>:8443` in Safari.
   * Tap the **Share button** (square with up arrow) in Safari.
   * Tap **"Add to Home Screen"**.
   * The app icon will appear on your iPad home screen!

> [!NOTE]
> Once installed via either method, the iPad runs 100% offline from its local storage. You can take the iPad anywhere without Wi-Fi!

---

## 🧪 Automated Testing
 
Run the headless iPad verification suites:
```bash
node test_ipad.js         # iPad layout, PWA tags, and features
node test_persistence.js  # Multi-page IndexedDB persistence & real controls
node test_buttons.js      # 240/240 button click coverage
```

---

## 📄 License & Attribution
* Symbol pictures are original works generated for this app (no third-party symbol licence).
* Software &copy; 2026 Talk Tiles AAC Team.
