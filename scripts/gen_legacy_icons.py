# -*- coding: utf-8 -*-
"""
API 26 미만용 런처 비트맵 생성기.

adaptive-icon 벡터(ic_launcher_{background,foreground}.xml)와 **같은 도형**을
픽셀로 다시 그린다. 둘이 어긋나면 기기에 따라 아이콘이 달라 보이므로,
좌표·색 상수는 벡터 쪽 값을 그대로 옮겨 적었다.

실행: python gen_legacy_icons.py <res 디렉터리>
"""
import sys
import os
from PIL import Image, ImageDraw, ImageFilter

# ── 벡터와 공유하는 설계값 (108 viewport 기준) ──────────────────
VIEWPORT = 108.0

GREEN = (46, 204, 113)  # #2ECC71
RED = (238, 65, 45)     # #EE412D
BLUE = (50, 135, 240)   # #3287F0
INK = (22, 33, 43)

PAPER_TOP = (255, 255, 255)
PAPER_BOTTOM = (248, 246, 242)

# (색, 연한 테두리색, 마커 x, 마커 y)
MARKERS = [
    (GREEN, (145, 237, 185), 39.0, 39.0),
    (RED,   (255, 168, 156), 70.0, 56.0),
    (BLUE,  (145, 195, 252), 49.0, 73.0),
]

MARKER_RADIUS = 9.5
MARKER_STROKE = 1.8
LINE_WIDTH = 1.8
GLOW_RADIUS = 26.0
GLOW_ALPHA = 0x38

# 레거시 아이콘은 런처가 마스크를 씌우지 않는다. adaptive 아이콘이 보통 보여주는
# 중앙 영역만 잘라 써야 같은 구도로 보인다.
LEGACY_CROP = 84.0

SUPERSAMPLE = 8

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def render_design(size):
    """108x108 설계를 size x size 픽셀로 그린다."""
    k = size / VIEWPORT

    def s(v):
        return v * k

    base = Image.new("RGB", (size, size), PAPER_TOP)

    # 종이 세로 그라디언트
    draw = ImageDraw.Draw(base)
    for y in range(size):
        t = y / max(size - 1, 1)
        draw.line(
            [(0, y), (size, y)],
            fill=tuple(
                round(PAPER_TOP[i] + (PAPER_BOTTOM[i] - PAPER_TOP[i]) * t)
                for i in range(3)
            ),
        )

    # 마커 클릭 발광 — 부드러운 가우시안 글로우
    for color, _, mx, my in MARKERS:
        glow = Image.new("L", (size, size), 0)
        ImageDraw.Draw(glow).ellipse(
            [s(mx - GLOW_RADIUS), s(my - GLOW_RADIUS), s(mx + GLOW_RADIUS), s(my + GLOW_RADIUS)],
            fill=GLOW_ALPHA,
        )
        glow = glow.filter(ImageFilter.GaussianBlur(s(GLOW_RADIUS) / 2.2))
        base = Image.composite(Image.new("RGB", (size, size), color), base, glow)

    draw = ImageDraw.Draw(base)

    # 십자선 — 아이콘 끝에서 끝까지 (테두리와 같은 연한 색)
    line_px = max(round(s(LINE_WIDTH)), 1)
    for _, rim_color, mx, my in MARKERS:
        draw.line([(0, s(my)), (size, s(my))], fill=rim_color, width=line_px)
        draw.line([(s(mx), 0), (s(mx), size)], fill=rim_color, width=line_px)

    # 마커 — 또렷하고 선명한 단색 원 + 같은 계열 연한 색 테두리
    stroke_px = max(round(s(MARKER_STROKE)), 1)
    for color, rim_color, mx, my in MARKERS:
        r = s(MARKER_RADIUS)
        draw.ellipse(
            [s(mx) - r, s(my) - r, s(mx) + r, s(my) + r],
            fill=color,
            outline=rim_color,
            width=stroke_px,
        )

    return base


def rounded_mask(size, radius_ratio):
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [0, 0, size - 1, size - 1], radius=round(size * radius_ratio), fill=255
    )
    return mask


def circle_mask(size):
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size - 1, size - 1], fill=255)
    return mask


def build(target_size, mask_fn):
    big = round(VIEWPORT * SUPERSAMPLE)
    design = render_design(big)

    inset = round((VIEWPORT - LEGACY_CROP) / 2 * SUPERSAMPLE)
    cropped = design.crop((inset, inset, big - inset, big - inset))
    scaled = cropped.resize((target_size, target_size), Image.LANCZOS)

    out = scaled.convert("RGBA")
    out.putalpha(mask_fn(target_size))
    return out


def main():
    res_dir = sys.argv[1]
    for folder, px in DENSITIES.items():
        out_dir = os.path.join(res_dir, folder)
        os.makedirs(out_dir, exist_ok=True)

        build(px, lambda n: rounded_mask(n, 0.22)).save(
            os.path.join(out_dir, "ic_launcher.webp"), "WEBP", lossless=True
        )
        build(px, circle_mask).save(
            os.path.join(out_dir, "ic_launcher_round.webp"), "WEBP", lossless=True
        )
        print(f"{folder}: {px}px")

    # 확인용 크게 한 장 (저장소에는 안 들어간다)
    build(512, lambda n: rounded_mask(n, 0.22)).save(
        os.path.join(os.path.dirname(__file__), "icon_preview_512.png")
    )
    print("preview: icon_preview_512.png")


if __name__ == "__main__":
    main()
