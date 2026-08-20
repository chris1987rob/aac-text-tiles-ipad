#!/bin/bash
# Exercises the logic behind every toolbar, express-bar and editing control,
# against the app's real AACStore and models.   ./Tools/StoreTests/run.sh
set -e
cd "$(dirname "$0")/../.."
OUT=$(mktemp -d)
xcrun swiftc -O \
  Tools/StoreTests/Stub.swift \
  AACTextTilesSwiftUI/Models/*.swift \
  AACTextTilesSwiftUI/Services/AACStore.swift \
  Tools/StoreTests/main.swift \
  -o "$OUT/t"
"$OUT/t"; rm -rf "$OUT"
