"""Erzeugt den Titelbildschirm: Logo, Untertitel, Panorama (zerstörte Skyline unter Blutmond), Buttons, Mod-Icon.

Aufruf: python3 tools/gen_title.py  (aus dem deadzone-Ordner)
"""
import math
import os
import random
from PIL import Image, ImageDraw, ImageFilter

RES = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
MC = os.path.join(RES, "assets", "minecraft", "textures", "gui")
PREVIEW = os.environ.get("PREVIEW_DIR")

FONT5x7 = {
    "D": ["1111.", "1...1", "1...1", "1...1", "1...1", "1...1", "1111."],
    "E": ["11111", "1....", "1....", "1111.", "1....", "1....", "11111"],
    "A": [".111.", "1...1", "1...1", "11111", "1...1", "1...1", "1...1"],
    "Z": ["11111", "....1", "...1.", "..1..", ".1...", "1....", "11111"],
    "O": [".111.", "1...1", "1...1", "1...1", "1...1", "1...1", ".111."],
    "N": ["1...1", "11..1", "1.1.1", "1.1.1", "1..11", "1...1", "1...1"],
}

FONT3x5 = {
    "Z": ["111", "..1", ".1.", "1..", "111"],
    "O": ["111", "1.1", "1.1", "1.1", "111"],
    "M": ["1.1", "111", "111", "1.1", "1.1"],
    "B": ["11.", "1.1", "11.", "1.1", "11."],
    "I": ["111", ".1.", ".1.", ".1.", "111"],
    "E": ["111", "1..", "11.", "1..", "111"],
    "A": [".1.", "1.1", "111", "1.1", "1.1"],
    "P": ["11.", "1.1", "11.", "1..", "1.."],
    "C": [".11", "1..", "1..", "1..", ".11"],
    "L": ["1..", "1..", "1..", "1..", "111"],
    "Y": ["1.1", "1.1", ".1.", ".1.", ".1."],
    "S": [".11", "1..", ".1.", "..1", "11."],
    " ": ["...", "...", "...", "...", "..."],
}


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


# --------------------------------------------------------------------------- Logo

def logo():
    # logische Auflösung 256x64, Textur 1024x256 (4x)
    scale = 4
    W, H = 256, 64
    cell = 4
    text = "DEADZONE"
    letter_w = 5 * cell
    gap = 3
    total = len(text) * letter_w + (len(text) - 1) * gap
    x0 = (W - total) // 2
    y0 = 4
    rnd = random.Random(7)

    mask = Image.new("L", (W, H), 0)
    md = ImageDraw.Draw(mask)
    for i, ch in enumerate(text):
        g = FONT5x7[ch]
        lx = x0 + i * (letter_w + gap)
        for gy, row in enumerate(g):
            for gx, v in enumerate(row):
                if v == "1":
                    md.rectangle([lx + gx * cell, y0 + gy * cell, lx + gx * cell + cell - 1, y0 + gy * cell + cell - 1], fill=255)

    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    m = mask.load()
    depth = 4
    # 3D-Kante nach unten rechts
    for d in range(depth, 0, -1):
        for y in range(H):
            for x in range(W):
                if m[x, y] and x + d < W and y + d < H:
                    shade = 40 - d * 4
                    px[x + d, y + d] = (shade + 20, shade // 3, shade // 3, 255)
    # Blutstropfen
    for x in range(W):
        for y in range(H - 1, -1, -1):
            if m[x, y]:
                if rnd.random() < 0.12:
                    length = rnd.randint(2, 9)
                    for k in range(1, length + 1):
                        if y + k < H and not m[x, y + k]:
                            a = 255 if k < length else 180
                            px[x, y + k] = (120 - k * 5, 8, 8, a)
                break
    # Vorderseite: Verlauf von hellrot nach dunkelrot, mit Rissen und Rauschen
    top_c = (226, 40, 30)
    bot_c = (110, 10, 10)
    for y in range(H):
        for x in range(W):
            if m[x, y]:
                t = (y - y0) / (7 * cell)
                c = lerp(top_c, bot_c, max(0.0, min(1.0, t)))
                n = rnd.uniform(0.85, 1.12)
                c = tuple(max(0, min(255, int(v * n))) for v in c)
                px[x, y] = c + (255,)
    # Risse
    for _ in range(26):
        x = rnd.randrange(W)
        y = rnd.randrange(y0, y0 + 7 * cell)
        for _ in range(rnd.randint(3, 8)):
            if 0 <= x < W and 0 <= y < H and m[x, y]:
                px[x, y] = (40, 4, 4, 255)
            x += rnd.choice((-1, 0, 1))
            y += 1
    # Lichtkante oben
    for y in range(1, H):
        for x in range(W):
            if m[x, y] and not m[x, y - 1]:
                px[x, y] = (255, 120, 100, 255)
    big = img.resize((W * scale, H * scale), Image.NEAREST)
    os.makedirs(os.path.join(MC, "title"), exist_ok=True)
    big.save(os.path.join(MC, "title", "minecraft.png"))
    big.save(os.path.join(MC, "title", "minceraft.png"))
    return img


def edition():
    # logisch 128x16, Textur 512x64
    W, H = 512, 64
    cell = 6
    text = "ZOMBIE APOCALYPSE"
    total = len(text) * 4 * cell - cell
    x0 = (W - total) // 2
    y0 = 10
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for i, ch in enumerate(text):
        g = FONT3x5[ch]
        lx = x0 + i * 4 * cell
        for gy, row in enumerate(g):
            for gx, v in enumerate(row):
                if v == "1":
                    x, y = lx + gx * cell, y0 + gy * cell
                    d.rectangle([x + 3, y + 3, x + cell + 2, y + cell + 2], fill=(20, 0, 0, 255))
                    d.rectangle([x, y, x + cell - 1, y + cell - 1], fill=(235, 225, 205, 255))
    img.save(os.path.join(MC, "title", "edition.png"))
    return img


# --------------------------------------------------------------------------- Panorama

def skyline(phi, rnd_cache={}):
    """Höhenwinkel (Grad) der Gebäudesilhouette in Richtung phi (0..360). Nahtlos, weil periodisch."""
    phi %= 360
    width = 6.0
    idx = int(phi // width)
    if idx not in rnd_cache:
        r = random.Random(idx * 7919 + 13)
        base = r.uniform(4, 14)
        if r.random() < 0.25:
            base = r.uniform(18, 32)
        broken = r.random() < 0.5
        rnd_cache[idx] = (base, broken, r.uniform(0, width), r.random())
    base, broken, cut, seed = rnd_cache[idx]
    local = phi - idx * width
    h = base
    if local < 0.4 or local > width - 0.4:
        h = min(h, 1.5)
    if broken:
        # schräg abgebrochenes Dach
        h -= max(0.0, (local - cut)) * 0.9
    return max(1.2, h)


def window_lit(phi, elev):
    cx = int(phi * 3)
    cy = int(elev * 3)
    r = random.Random(cx * 92821 + cy * 68917)
    return r.random() < 0.06


def sky_color(elev):
    # Horizont orange-rot, oben fast schwarz-violett
    t = max(0.0, min(1.0, elev / 60.0))
    if t < 0.15:
        return lerp((150, 40, 20), (90, 15, 20), t / 0.15)
    return lerp((90, 15, 20), (12, 6, 16), (t - 0.15) / 0.85)


def panorama():
    S = 512
    moon_phi, moon_elev, moon_r = 30.0, 22.0, 6.0
    out = []
    folder = os.path.join(MC, "title", "background")
    os.makedirs(folder, exist_ok=True)
    rnd = random.Random(3)
    for face in range(6):
        img = Image.new("RGB", (S, S))
        px = img.load()
        for y in range(S):
            for x in range(S):
                u = 2 * (x + 0.5) / S - 1
                v = 1 - 2 * (y + 0.5) / S
                if face < 4:
                    phi = face * 90 + math.degrees(math.atan(u))
                    elev = math.degrees(math.atan2(v, math.sqrt(1 + u * u)))
                elif face == 4:
                    phi = math.degrees(math.atan2(u, -v))
                    elev = math.degrees(math.atan2(1, math.sqrt(u * u + v * v)))
                else:
                    phi = math.degrees(math.atan2(u, v))
                    elev = -math.degrees(math.atan2(1, math.sqrt(u * u + v * v)))
                c = sky_color(elev)
                # Blutmond mit Schein
                dphi = (phi - moon_phi + 180) % 360 - 180
                dist = math.hypot(dphi * math.cos(math.radians(elev)), elev - moon_elev)
                if dist < moon_r:
                    shade = 1 - (dist / moon_r) * 0.25
                    c = (int(210 * shade), int(50 * shade), int(35 * shade))
                elif dist < moon_r * 4:
                    g = (1 - (dist - moon_r) / (moon_r * 3)) ** 2
                    c = lerp(c, (200, 60, 40), g * 0.5)
                # Sterne oben
                if elev > 25 and ((x * 73856093) ^ (y * 19349663) ^ face) % 997 == 0:
                    c = (200, 180, 190)
                if elev < 0:
                    # Boden: dunkel, Richtung Horizont rötlicher Dunst
                    c = lerp((40, 12, 12), (8, 4, 6), min(1.0, -elev / 30))
                h = skyline(phi)
                if 0 <= elev < h:
                    # Gebäude: fast schwarz, vereinzelt brennende Fenster
                    c = (16, 10, 12)
                    if elev > 1.5 and window_lit(phi, elev):
                        c = (230, 120, 40)
                # Rauchsäulen
                for sp in (75.0, 160.0, 250.0, 320.0):
                    dp = (phi - sp + 180) % 360 - 180
                    if elev > 0:
                        wobble = math.sin(elev * 0.2 + sp) * 2.5
                        spread = 1.5 + elev * 0.25
                        if abs(dp - wobble) < spread:
                            k = (1 - abs(dp - wobble) / spread) * max(0.0, 1 - elev / 70) * 0.55
                            c = lerp(c, (30, 22, 24), k)
                px[x, y] = c
        img = img.filter(ImageFilter.GaussianBlur(0.6))
        img.save(os.path.join(folder, f"panorama_{face}.png"), optimize=True)
        out.append(img)
    return out


# --------------------------------------------------------------------------- Buttons

def button(path, border, fill_top, fill_bottom, text_line):
    W, H = 200, 20
    img = Image.new("RGBA", (W, H))
    px = img.load()
    rnd = random.Random(hash(path) & 0xffff)
    for y in range(H):
        for x in range(W):
            t = y / (H - 1)
            c = lerp(fill_top, fill_bottom, t)
            n = rnd.uniform(0.9, 1.08)
            c = tuple(max(0, min(255, int(v * n))) for v in c)
            px[x, y] = c + (255,)
    for x in range(W):
        px[x, 0] = border + (255,)
        px[x, H - 1] = (0, 0, 0, 255)
        px[x, 1] = text_line + (255,)
    for y in range(H):
        px[0, y] = border + (255,)
        px[W - 1, y] = (0, 0, 0, 255)
    for y in range(1, H - 1):
        px[1, y] = text_line + (255,)
    # Rostflecken innen (außerhalb des 3px-Rands, damit 9-Slice sauber bleibt)
    for _ in range(40):
        x, y = rnd.randrange(4, W - 4), rnd.randrange(4, H - 4)
        px[x, y] = (fill_bottom[0] + 25, fill_bottom[1] + 8, fill_bottom[2], 255)
    folder = os.path.join(MC, "sprites", "widget")
    os.makedirs(folder, exist_ok=True)
    img.save(os.path.join(folder, path + ".png"))
    with open(os.path.join(folder, path + ".png.mcmeta"), "w") as f:
        f.write('{\n  "gui": {\n    "scaling": {\n      "type": "nine_slice",\n      "width": 200,\n      "height": 20,\n      "border": 3\n    }\n  }\n}\n')
    return img


def buttons():
    a = button("button", (70, 20, 20), (58, 48, 46), (34, 28, 28), (96, 80, 76))
    b = button("button_highlighted", (220, 40, 30), (92, 30, 26), (52, 16, 14), (255, 120, 90))
    c = button("button_disabled", (30, 30, 30), (36, 34, 34), (24, 22, 22), (44, 42, 42))
    return [a, b, c]


def icon(logo_img):
    img = Image.new("RGBA", (128, 128), (20, 8, 10, 255))
    d = ImageDraw.Draw(img)
    for r in range(64, 0, -1):
        c = lerp((120, 20, 16), (20, 8, 10), r / 64)
        d.ellipse([64 - r, 64 - r, 64 + r, 64 + r], fill=c + (255,))
    zombie = Image.open(os.path.join(RES, "assets", "deadzone", "textures", "entity", "zombie", "walker.png")).crop((8, 8, 16, 16))
    zombie = zombie.resize((64, 64), Image.NEAREST)
    img.paste(zombie, (32, 18), zombie)
    word = logo_img.crop((0, 0, 256, 40)).resize((128, 20), Image.NEAREST)
    img.paste(word, (0, 96), word)
    path = os.path.join(RES, "assets", "deadzone", "icon.png")
    img.save(path)
    return img


def splashes():
    lines = [
        "Überlebe die Nacht!", "Nicht beißen lassen!", "Tagsüber sind sie schwach!", "Kopfschuss!",
        "Such die grüne Flasche!", "18 Treffer... dann wird's eng", "Der Koloss kommt!", "Hörst du das Schreien?",
        "Munition ist alles", "Die Stadt ist gefallen", "Bunker gefunden!", "RPG > Zombies",
        "Survive the night!", "Aim for the head!", "Do not get bitten!", "Find the green bottle!",
        "Day 1 of the apocalypse", "They are faster at night", "Silence is survival", "Reload!",
    ]
    folder = os.path.join(RES, "assets", "minecraft", "texts")
    os.makedirs(folder, exist_ok=True)
    with open(os.path.join(folder, "splashes.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")


if __name__ == "__main__":
    lg = logo()
    ed = edition()
    pano = panorama()
    bts = buttons()
    ic = icon(lg)
    splashes()
    if PREVIEW:
        sheet = Image.new("RGBA", (1024, 900), (0, 0, 0, 255))
        strip = Image.new("RGB", (2048, 512))
        for i in range(4):
            strip.paste(pano[i], (i * 512, 0))
        sheet.paste(strip.resize((1024, 256)), (0, 0))
        big = lg.resize((768, 192), Image.NEAREST)
        sheet.paste(big, (128, 270), big)
        e = ed.resize((384, 48), Image.NEAREST)
        sheet.paste(e, (320, 440), e)
        for i, b in enumerate(bts):
            bb = b.resize((400, 40), Image.NEAREST)
            sheet.paste(bb, (40, 520 + i * 60), bb)
        sheet.paste(ic, (600, 520), ic)
        sheet.save(os.path.join(PREVIEW, "title_preview.png"))
    print("ok")
