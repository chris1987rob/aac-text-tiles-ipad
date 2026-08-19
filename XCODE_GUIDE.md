# Xcode & MacBook Pro Setup Guide — AAC Text Tiles iPad

This guide explains how to clone and run **AAC Text Tiles iPad** on your **MacBook Pro** and deploy it directly onto your physical **iPad** using **Xcode**.

---

## 🚀 Quick Start in 4 Steps

### Step 1: Clone the Repository on your MacBook Pro
Open **Terminal** on your Mac and run:
```bash
git clone https://github.com/chris1987rob/aac-text-tiles-ipad.git
cd aac-text-tiles-ipad
```

---

### Step 2: Open the Project in Xcode
Double-click `AACTextTilesiPad.xcodeproj` or run from Terminal:
```bash
open AACTextTilesiPad.xcodeproj
```

---

### Step 3: Configure Free Apple Developer Signing
1. In the Xcode left navigator, click the top **`AACTextTilesiPad`** blue project icon.
2. Select the **`AACTextTilesiPad`** target under **TARGETS**.
3. Go to the **Signing & Capabilities** tab.
4. Check **Automatically manage signing**.
5. Under **Team**, select your Personal Apple ID team (or tap **Add Account...** and sign in with your free Apple ID).
6. Set the **Bundle Identifier** (e.g., `com.yourname.aactexttiles.ipad`).

---

### Step 4: Connect Your iPad & Run
1. Plug your **iPad** into your MacBook Pro using a USB-C or Lightning cable (or connect via Wi-Fi if enabled in Xcode).
2. At the top of the Xcode window, click the **Device Destination** dropdown and select your physical **iPad** (or an iPad Simulator like *iPad Pro 11-inch* or *iPad 10th Gen*).
3. Click the **Play / Run button (▶)** or press **`⌘R`** (Command + R).
4. Xcode will compile, install, and launch the app directly onto your iPad!

> [!TIP]
> **First-Time iPad Installation Note**:
> If iPadOS shows *"Untrusted Developer"*, on your iPad open:
> **Settings → General → VPN & Device Management → Tap your Apple ID → Trust "Your Name"**.

---

## 🛠 Features & Native iPad Optimizations Included

* **Dedicated iPad Architecture (`TARGETED_DEVICE_FAMILY = 2`)**: Optimized exclusively for iPadOS 15, 16, 17, and 18.
* **Hardware Mute Switch Bypass**: Configured with `AVAudioSession.Category.playback` so communication speech always plays through iPad speakers even when muted.
* **Zero Latency Offline Speech**: Dual-engine speech system using native `AVSpeechSynthesizer` Swift bridge and iOS Web Speech API.
* **3,600+ Bundled Vector Symbols**: All 3,436 official Mulberry AAC symbols bundled directly in the app bundle (`www/symbols/en/`).
* **Visual Scene Displays & Hotspots**: Interactive photo touch zones with recorded audio and text-to-speech.
* **Talking Keyboard & Phonics**: Full QWERTY keyboard with word prediction chips and Sound It Out syllable breakdown.
* **Apple Pencil & Touch Optimizations**: Double-tap zoom and rubber-banding suppression for steady communication access.
* **Smart / Magic Keyboard Support**: Direct number key triggers (1–9), spacebar sentence speech, arrow key navigation, and escape key home return.

---

## 📂 Project Directory Structure

```
aac-text-tiles-ipad/
├── AACTextTilesiPad.xcodeproj/   # Turnkey Xcode Project
│   └── project.pbxproj
├── AACTextTilesiPad/             # Native Swift iOS Container
│   ├── AppDelegate.swift         # Audio session & awake lifecycle
│   ├── SceneDelegate.swift       # iPadOS multi-window & Stage Manager
│   ├── ViewController.swift      # WKWebView container & native Swift speech bridge
│   ├── Info.plist                # Apple permissions & iPad orientations
│   ├── Base.lproj/               # Launch screen storyboard
│   ├── Assets.xcassets/          # iPad AppIcon sets (76pt, 152pt, 167pt, 1024pt)
│   └── www/                      # Bundled web assets (offline HTML, JS, SVG symbols)
├── index.html                    # Main AAC single-page application
├── symbols_data.js               # 3,436 Mulberry symbol index & taxonomy
├── symbols/en/                   # 3,436 Mulberry AAC vector SVGs
├── sw.js                         # Offline Service Worker
├── manifest.json                 # Apple PWA manifest
├── serve.py                      # Local Wi-Fi delivery server for Safari PWA install
└── test_ipad.js                  # Automated iPad verification suite
```

---

## 🧪 Automated Testing
To verify the app before deploying:
```bash
node test_ipad.js
```
