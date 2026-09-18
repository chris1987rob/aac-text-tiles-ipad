#!/bin/bash
# Builds and installs "Talk Tiles Lite" - the same app under a second bundle id.
#
# Why a second flavour: the main app holds a board the family has invested real
# time in. A separate bundle id gets a separate data container, so new work can
# be tried without risking that board. Same reason com.chris1987rob.talktiles2
# exists on the device.
#
#   ./Tools/build-lite.sh            build and install
#   ./Tools/build-lite.sh --build    build only
set -e
cd "$(dirname "$0")/.."

UDID=d2162460fab717ed3b5b90ba2cc8d612f953b80c
BUNDLE=com.chris1987rob.talktileslite
NAME="Talk Tiles Lite"
OUT="${TMPDIR:-/tmp}/talktiles-lite-build"

echo "==> Building $NAME ($BUNDLE)"
xcodebuild -project AACTextTilesSwiftUI.xcodeproj -scheme AACTextTilesSwiftUI \
  -configuration Release -destination 'generic/platform=iOS' \
  -derivedDataPath "$OUT" -allowProvisioningUpdates \
  PRODUCT_BUNDLE_IDENTIFIER="$BUNDLE" \
  APP_DISPLAY_NAME="$NAME" \
  build

APP="$OUT/Build/Products/Release-iphoneos/AACTextTilesSwiftUI.app"
echo "==> Built: $(/usr/libexec/PlistBuddy -c 'Print :CFBundleDisplayName' "$APP/Info.plist") / $(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$APP/Info.plist")"
[ "$1" = "--build" ] && exit 0

# No --justlaunch: the iPad runs iPadOS 17 and Xcode 14.2 has no matching
# DeveloperDiskImage, so auto-launch always fails even though the install is
# fine. Tap the icon on the iPad instead.
echo "==> Installing to the iPad"
npx --yes ios-deploy@1.12.2 --id "$UDID" --bundle "$APP" --no-wifi --timeout 30
echo "==> Done - tap '$NAME' on the iPad."
