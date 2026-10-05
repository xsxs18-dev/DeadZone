"""Erzeugt die Rüstungs-Texturen am Körper (64x32, Vanilla-Rüstungs-Layout): Militär, Gasmaske, Nachtsichtbrille.

Aufruf: python3 tools/gen_armor.py  (aus dem deadzone-Ordner)
"""
import os
import random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "deadzone", "textures", "entity", "equipment")
PREVIEW = os.environ.get("PREVIEW_DIR")


def hexc(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def faces(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h),
    }


HEAD = faces(0, 0, 8, 8, 8)
BODY = faces(16, 16, 8, 12, 4)
ARM = faces(40, 16, 4, 12, 4)
LEG = faces(0, 16, 4, 12, 4)

rnd = random.Random(5)
CAMO = [hexc("#4b5a2a"), hexc("#3a4520"), hexc("#6b6a3a"), hexc("#2e2a1c"), hexc("#55642e")]


def camo():
    return rnd.choice(CAMO)


def paint(img, part, rows_fn):
    for name, (x, y, w, h) in part.items():
        for fy in range(h):
            for fx in range(w):
                c = rows_fn(name, fx, fy, w, h)
                if c:
                    img.putpixel((x + fx, y + fy), c)


def military():
    top = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    # Helm: oben ganz, Seiten bis Augenhöhe, vorne nur Rand
    paint(top, HEAD, lambda n, x, y, w, h: camo() if n == "top" or (n in ("left", "right", "back") and y < 5)
          or (n == "front" and y < 2) else (hexc("#202418") if n == "front" and y == 2 else None))
    # Weste: Körper, Taschen
    def vest(n, x, y, w, h):
        if n in ("top", "bottom"):
            return camo()
        if n == "front" and y in (5, 6) and x in (1, 2, 5, 6):
            return hexc("#2a2a20")
        if n == "front" and y == 0 and x in (2, 5):
            return hexc("#1a1a14")
        return camo() if y < 10 else hexc("#3a3a2a")
    paint(top, BODY, vest)
    # Ärmel und Schulterpolster
    paint(top, ARM, lambda n, x, y, w, h: camo() if (n == "top" or y < 4) else None)
    # Stiefel: unteres Viertel der Beine
    paint(top, LEG, lambda n, x, y, w, h: (hexc("#2a1e14") if y < h - 1 else hexc("#141010")) if (n == "bottom" or y >= 8) else None)

    legs = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    paint(legs, LEG, lambda n, x, y, w, h: camo() if (n == "top" or y < 9) else None)
    paint(legs, BODY, lambda n, x, y, w, h: (hexc("#2a2a20") if y == 10 else camo()) if (n not in ("top",) and y >= 9) else None)
    return top, legs


def gas_mask():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    strap = hexc("#202020")
    rubber = hexc("#2c2f2c")

    def head(n, x, y, w, h):
        if n == "front":
            if y == 3 and x in (1, 2, 5, 6):
                return hexc("#9dff8a")
            if y == 3:
                return rubber
            if 4 <= y <= 7:
                if y >= 5 and x in (3, 4):
                    return hexc("#707570") if y < 7 else hexc("#909590")
                return rubber
            return None
        if n in ("left", "right") and y in (3, 4):
            return strap
        if n == "back" and y == 3:
            return strap
        return None
    paint(img, HEAD, head)
    return img


def nv_goggles():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def head(n, x, y, w, h):
        if n == "front" and y in (2, 3, 4):
            if x in (1, 2, 5, 6) and y in (3, 4):
                return hexc("#3cdc3c") if y == 3 else hexc("#1e7a1e")
            return hexc("#1a1c1e")
        if n in ("left", "right", "back") and y == 3:
            return hexc("#2a2a2a")
        if n == "top" and y in (3, 4):
            return hexc("#2a2a2a")
        return None
    paint(img, HEAD, head)
    return img


def save(img, layer, name):
    folder = os.path.join(ROOT, layer)
    os.makedirs(folder, exist_ok=True)
    img.save(os.path.join(folder, name + ".png"))


if __name__ == "__main__":
    top, legs = military()
    save(top, "humanoid", "military")
    save(legs, "humanoid_leggings", "military")
    save(gas_mask(), "humanoid", "gas_mask")
    save(nv_goggles(), "humanoid", "nv_goggles")
    if PREVIEW:
        sheet = Image.new("RGBA", (64 * 4, 32), (60, 60, 70, 255))
        for i, im in enumerate((top, legs, gas_mask(), nv_goggles())):
            sheet.paste(im, (i * 64, 0), im)
        sheet.resize((64 * 4 * 4, 128), Image.NEAREST).save(os.path.join(PREVIEW, "armor_preview.png"))
    print("ok")
