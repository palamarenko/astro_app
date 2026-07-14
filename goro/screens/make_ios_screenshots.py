"""
Resizes existing English Android screenshots to iOS App Store size.
Target: 1284 x 2778 px (iPhone 6.7" / 14 Pro Max)
Saves to: goro/screens/en/1284x2778/
"""

from pathlib import Path
from PIL import Image

SCRIPT_DIR = Path(__file__).parent
SRC_DIR = SCRIPT_DIR / "en"
OUT_DIR = SCRIPT_DIR / "en" / "1284x2778"
TARGET_SIZE = (1284, 2778)

OUT_DIR.mkdir(parents=True, exist_ok=True)

sources = sorted(SRC_DIR.glob("Screenshot_*.png"))
if not sources:
    print("No source screenshots found in", SRC_DIR)
    exit(1)

for i, src in enumerate(sources, start=1):
    img = Image.open(src).convert("RGB")

    # Scale to fill width, then center-crop vertically
    scale = TARGET_SIZE[0] / img.width
    new_h = int(img.height * scale)
    img = img.resize((TARGET_SIZE[0], new_h), Image.LANCZOS)

    if new_h >= TARGET_SIZE[1]:
        # Crop vertically centered
        top = (new_h - TARGET_SIZE[1]) // 2
        img = img.crop((0, top, TARGET_SIZE[0], top + TARGET_SIZE[1]))
    else:
        # Pad with black if too short
        canvas = Image.new("RGB", TARGET_SIZE, (0, 0, 0))
        canvas.paste(img, (0, (TARGET_SIZE[1] - new_h) // 2))
        img = canvas

    out_path = OUT_DIR / f"ios_{i:02d}.png"
    img.save(out_path, "PNG")
    print(f"Saved: {out_path.name}  ({TARGET_SIZE[0]}x{TARGET_SIZE[1]})")

print(f"\nDone — {len(sources)} screenshot(s) in {OUT_DIR}")
