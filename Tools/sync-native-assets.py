#!/usr/bin/env python3
"""Mirror the Talk Tiles pictures and voice clips into the native SwiftUI app.

The web copies (index.html, AACTextTilesiPad/www/) read symbols_data.js at
runtime. The SwiftUI app cannot, so this turns the same data into plain files
Xcode ships as folder references:

  AACTextTilesSwiftUI/TalkTilesSymbols/<id>.webp    one picture per symbol
  AACTextTilesSwiftUI/TalkTilesSymbols/catalog.json  id, label, tts, category, tags, emoji
  AACTextTilesSwiftUI/Voices/<voice>/<id>.mp3        one clip per symbol
  AACTextTilesSwiftUI/Voices/<voice>/phrases/*.mp3   clips for the built-in board phrases
  AACTextTilesSwiftUI/Voices/<voice>/index.json      normalised phrase -> clip file

Run it after aac-board's write_symbols_data.py has refreshed symbols_data.js
and symbols/ in this repo:

    python3 Tools/sync-native-assets.py            # pictures + Bella
    python3 Tools/sync-native-assets.py --voices bella,jake,maya

Files no longer in the catalogue are removed so the bundle never carries a
picture the app cannot name. The phrase normalisation here MUST match
SpokenText.normalisedPhrase in Swift - it is the key both sides look clips up by.
"""
import argparse
import json
import os
import re
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP = os.path.join(ROOT, "AACTextTilesSwiftUI")
SYMBOLS_JS = os.path.join(ROOT, "symbols_data.js")


def read_js_const(src, name):
    """Pull `const NAME = <json>;` out of symbols_data.js."""
    m = re.search(r"const %s = (.*?);\n" % re.escape(name), src, re.S)
    if not m:
        raise SystemExit("symbols_data.js has no %s" % name)
    return json.loads(m.group(1))


def normalise(text):
    """Lowercase, keep letters/digits/apostrophes, collapse the rest to spaces.

    Mirror of SpokenText.normalisedPhrase - keep the two in step."""
    s = (text or "").lower().replace("’", "'")
    s = re.sub(r"[^\w']+|_+", " ", s)
    return re.sub(r"\s+", " ", s).strip()


def sync_dir(dst, wanted):
    """Make `dst` hold exactly the files in `wanted` (name -> source path)."""
    os.makedirs(dst, exist_ok=True)
    copied = 0
    for name, src in wanted.items():
        out = os.path.join(dst, name)
        if (not os.path.exists(out)
                or os.path.getsize(out) != os.path.getsize(src)
                or os.path.getmtime(out) < os.path.getmtime(src)):
            shutil.copy2(src, out)
            copied += 1
    removed = 0
    for name in os.listdir(dst):
        p = os.path.join(dst, name)
        if os.path.isfile(p) and name not in wanted and not name.endswith(".json"):
            os.remove(p)
            removed += 1
    return copied, removed


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--voices", default="bella",
                    help="comma-separated voice ids from AAC_VOICES to bundle (default: bella)")
    args = ap.parse_args()

    src = open(SYMBOLS_JS, encoding="utf-8").read()
    symbols = read_js_const(src, "AAC_OFFICIAL_SYMBOLS")
    phrases = read_js_const(src, "AAC_PHRASE_CLIPS")
    voices = {v["id"]: v for v in read_js_const(src, "AAC_VOICES")}
    clip_ext = read_js_const(src, "AAC_CLIP_FORMAT")

    # --- pictures -------------------------------------------------------
    pics = {}
    catalog = []
    missing = []
    for s in symbols:
        img = os.path.join(ROOT, s["img"])
        if not os.path.isfile(img) or os.path.getsize(img) == 0:
            missing.append(s["id"])
            continue
        pics[s["id"] + ".webp"] = img
        catalog.append({
            "id": s["id"],
            "label": s["label"],
            "tts": s.get("tts") or s["label"],
            "category": s.get("category", ""),
            "tags": s.get("tags", []),
            "emoji": s.get("emoji", ""),
        })
    if missing:
        print("WARNING: %d symbols have no picture and were skipped: %s"
              % (len(missing), ", ".join(missing[:10])), file=sys.stderr)

    pic_dir = os.path.join(APP, "TalkTilesSymbols")
    copied, removed = sync_dir(pic_dir, pics)
    with open(os.path.join(pic_dir, "catalog.json"), "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, separators=(",", ":"))
    print("pictures: %d in catalogue, %d copied, %d stale removed -> %s"
          % (len(catalog), copied, removed, os.path.relpath(pic_dir, ROOT)))

    # --- voices ---------------------------------------------------------
    for vid in [v.strip() for v in args.voices.split(",") if v.strip()]:
        if vid not in voices:
            raise SystemExit("unknown voice %r; AAC_VOICES has %s" % (vid, ", ".join(voices)))
        vdir = os.path.join(ROOT, voices[vid]["dir"])
        out = os.path.join(APP, "Voices", vid)
        index = {}
        clips = {}
        for s in symbols:
            f = os.path.join(vdir, s["id"] + "." + clip_ext)
            if not os.path.isfile(f) or os.path.getsize(f) == 0:
                continue
            name = s["id"] + "." + clip_ext
            clips[name] = f
            key = normalise(s.get("tts") or s["label"])
            if key and key not in index:
                index[key] = name
        pclips = {}
        for p in phrases:
            # Phrase clips live beside the voice's symbol clips in <dir>/phrases/.
            f = os.path.join(vdir, "phrases", os.path.basename(p["audio"]))
            if not os.path.isfile(f) or os.path.getsize(f) == 0:
                continue
            name = os.path.basename(f)
            pclips[name] = f
            key = normalise(p["text"])
            if key and key not in index:
                index[key] = "phrases/" + name
        c1, r1 = sync_dir(out, clips)
        c2, r2 = sync_dir(os.path.join(out, "phrases"), pclips)
        with open(os.path.join(out, "index.json"), "w", encoding="utf-8") as f:
            json.dump({"voice": vid, "name": voices[vid]["name"],
                       "description": voices[vid].get("description", ""),
                       "format": clip_ext, "clips": index},
                      f, ensure_ascii=False, separators=(",", ":"), sort_keys=True)
        print("voice %s: %d symbol clips + %d phrase clips (%d copied, %d stale removed) -> %s"
              % (vid, len(clips), len(pclips), c1 + c2, r1 + r2, os.path.relpath(out, ROOT)))


if __name__ == "__main__":
    main()
