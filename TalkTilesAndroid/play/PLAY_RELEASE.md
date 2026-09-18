# Talk Tiles — Google Play release kit

Everything technical is done and in this folder. What remains is done by hand
in the Play Console by the account owner. The business-side decisions
(personal vs LLC account, paid tier, accounts/cloud backup later) are in
`~/aac-board/PLAY_STORE_LAUNCH_PLAN.md`; this file is only about getting **this
build** live.

## What is ready

| Item | Where | Status |
|---|---|---|
| App Bundle to upload | `TalkTilesAndroid/app/build/outputs/bundle/release/app-release.aab` (copy: `~/Desktop/TalkTiles-Android-v1.2.aab`) | built, ~101 MB |
| Same build as an APK for sideloading/testing | `~/Desktop/TalkTiles-Android-v1.2.apk` | installed on the Alcatel tablet |
| Package name (permanent once uploaded) | `com.talktiles.app` | set |
| versionCode / versionName | 3 / 1.2 | set — bump both for every upload |
| targetSdk / compileSdk | 36 / 36 (Play's 2026 requirement) | set |
| minSdk | 26 (Android 8.0) | set |
| Upload/signing key | `~/aac-board/aac.keystore`, alias `aacboard` | signs the bundle; **back this file up somewhere private** |
| Code shrinking (R8) | on, with kotlinx.serialization keep rules | verified: book saves/loads after a restart |
| Edge-to-edge (forced from targetSdk 35) | handled | verified on Android 15 |
| 64-bit / 16 KB page size | no native code, nothing to do | ok |
| Privacy policy | `docs/privacy.html` → https://chris1987rob.github.io/aac-text-tiles-ipad/privacy.html | published via GitHub Pages |
| Store icon 512×512 | `play/listing/icon-512.png` | ready |
| Feature graphic 1024×500 | `play/listing/feature-graphic-1024x500.png` | ready |
| Screenshots (800×1280, from the tablet) | `play/listing/01-home.png` … `07-settings.png` | ready — use for phone AND 7-inch tablet slots |
| Listing text | below | ready to paste |

## Console steps, in order

1. **Create the app** — Play Console › Create app. Name `Talk Tiles`, default language English (US), App, Free.
2. **Set up your app** (left sidebar "Dashboard" checklist):
   - *App access*: All functionality is available without special access.
   - *Ads*: No, the app has no ads.
   - *Content rating*: fill the IARC questionnaire — category **Utility / Productivity / Communication**; answer No to everything (no violence, no user interaction with others, no sharing of location, no purchases). Result will be Everyone.
   - *Target audience and content*: pick the ages the app is for. **Recommendation: 13 and over / adults** — the account holder (parent, teacher, therapist) sets it up; children use it under supervision. Choosing an under-13 age group puts the app in the Families programme with extra requirements (teacher-approved content, more review). Answer "No" to "Is your app designed for children?" if you take that recommendation. Either way answer No to "unintentionally appealing to children" only if you picked 13+.
   - *News app*: No. *COVID-19*: No. *Government app*: No. *Financial features*: No.
   - *Data safety*: see the answers below.
   - *Privacy policy*: paste the GitHub Pages URL above.
   - *Health*: it is an assistive/AAC app — if the "Health apps" declaration asks, select "Medical / accessibility" and describe: "AAC picture-communication board for people who cannot speak. No health data collected."
3. **Store listing** — paste the text below, upload the icon, feature graphic and screenshots (at least 2 for Phone; also add them under 7-inch tablet). Category: **Medical** (or Education). Contact email: your Gmail. 
4. **Testing first** — Play Console › Testing › **Internal testing** › Create release › upload `app-release.aab`. Choose **Play App Signing** when asked (Google keeps the final key; your keystore becomes the upload key). Add yourself as a tester by email, install from the link, check it opens.
5. **Closed testing (personal accounts only)** — new personal developer accounts must run a closed test with **12 testers for 14 continuous days** before production is unlocked. Create a closed track, invite 12 Gmail addresses (family, colleagues, the school), have them install and keep it installed. After 14 days apply for production access in the console.
6. **Production** — Production › Create release › the same bundle (or a newer one with a higher versionCode) › release notes below › Review › Start rollout. Review takes hours to a few days for a first submission.

## Store listing text

**App name:** Talk Tiles

**Short description (80 chars max):**
Picture communication board that talks. Offline, no account, your own voice.

**Full description:**
Talk Tiles is a picture-communication (AAC) board for people who cannot speak, and for the parents, teachers and therapists who build boards for them.

Tap a picture and it speaks. Turn on the sentence bar and words collect as you tap – "I want", "Eat", "Pizza" – then press PLAY to hear the whole sentence.

WHAT YOU GET
• 2,200+ original pictures, every one spoken by Bella, a warm recorded voice – not a robot
• 3,400+ Mulberry symbols as a second picture set
• 28 ready-made boards: core words, yes/no, feelings, food, help, school, bedtime, animals and more
• A symbol keyboard: build sentences from pictures grouped as People, Actions, Describing, Things, Social, Questions
• Visual scenes: put a photo of the living room, the playground or the classroom on a page and add talking spots to it
• Your own photos and your own recorded voice on any button – a picture of the real dog, Mom saying "Mom"
• Saved Buttons: build a button once, drop it on any page
• Grids from 1 to 48 buttons, colours that follow the Fitzgerald key used across AAC systems

MADE FOR UNSTEADY HANDS
• Hold-to-speak ignores a resting hand or a brushing sleeve
• Pause-before-repeat stops a tremor turning one press into five
• Speak on release lets a child slide to the right button before committing
• Child lock hides the editor behind a PIN

PRIVATE BY DESIGN
Everything stays on the device. No account, no sign-in, no ads, no analytics, no internet needed. Back up the whole book to a single file you keep, and restore it on any tablet or on Talk Tiles for iPad – the two apps share one book format.

**Release notes (v1.2):**
First release of Talk Tiles for Android: picture boards, symbol keyboard, visual scenes, Bella's recorded voice, your own photos and recordings, child lock, backup and restore.

## Data safety form — answers

- Does your app collect or share any of the required user data types? **No.**
- Is all of the user data collected by your app encrypted in transit? (not asked when nothing is collected)
- Do you provide a way for users to request that their data is deleted? Not applicable — nothing is collected; uninstalling deletes everything.

Why "No" is correct: photos and recordings are stored only inside the app's private storage on the device and are never transmitted to the developer or anyone else. Play's definition of "collect" is data sent off the device. The only time data leaves is when the user explicitly shares a backup file through an app of their choosing, which is user-initiated and does not count.

## Permissions declaration (if the console asks)

- `RECORD_AUDIO` — "Lets a parent record their own voice onto a button. Used only while the Record button is held; nothing is stored except the clip the user makes."
- `CAMERA` — "Lets a parent put a photo of a real object or person on a button or use one as a scene background."

## Building the next version

```bash
cd ~/aac-text-tiles-ipad/TalkTilesAndroid
# bump versionCode (+1) and versionName in app/build.gradle.kts
./gradlew bundleRelease          # -> app/build/outputs/bundle/release/app-release.aab
./gradlew assembleRelease        # -> the matching APK for the tablet
```

The keystore password is read from `~/aac-board/.keystore-pass` (or `KS_PASS`); never commit it.
