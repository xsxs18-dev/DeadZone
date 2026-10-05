"""Erzeugt die 10 Zombie-Texturen (64x64, Vanilla-Zombie-Layout) plus Leucht-Augen.

Aufruf: python3 tools/gen_zombies.py  (aus dem deadzone-Ordner)
"""
import os
import random
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "deadzone", "textures", "entity", "zombie")
PREVIEW = os.environ.get("PREVIEW_DIR")


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)


# Würfel-UV: (u, v, w, h, d) -> Flächen
def faces(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


HEAD = faces(0, 0, 8, 8, 8)
HAT = faces(32, 0, 8, 8, 8)
BODY = faces(16, 16, 8, 12, 4)
ARM = faces(40, 16, 4, 12, 4)
LEG = faces(0, 16, 4, 12, 4)


class Skin:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.eyes = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.r = random.Random(seed)

    def px(self, x, y, c):
        self.img.putpixel((x, y), c)

    def get(self, x, y):
        return self.img.getpixel((x, y))

    def fill_part(self, part, color_fn):
        """color_fn(face, fx, fy, w, h) -> Farbe oder None"""
        for name, (x, y, w, h) in part.items():
            for fy in range(h):
                for fx in range(w):
                    c = color_fn(name, fx, fy, w, h)
                    if c is not None:
                        self.px(x + fx, y + fy, c)

    def noisy(self, base, amount=0.12):
        return shade(base, 1 + (self.r.random() - 0.5) * amount * 2)

    def splatter(self, part, color, count, faces_=("front", "back", "left", "right", "top")):
        for _ in range(count):
            name = self.r.choice(faces_)
            x, y, w, h = part[name]
            cx, cy = x + self.r.randrange(w), y + self.r.randrange(h)
            for dx, dy in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1)):
                if self.r.random() < (1.0 if (dx, dy) == (0, 0) else 0.45):
                    xx, yy = cx + dx, cy + dy
                    if x <= xx < x + w and y <= yy < y + h:
                        self.px(xx, yy, self.noisy(color, 0.15))

    def save(self, name):
        os.makedirs(OUT, exist_ok=True)
        self.img.save(os.path.join(OUT, name + ".png"))
        self.eyes.save(os.path.join(OUT, name + "_eyes.png"))


def face_front():
    return HEAD["front"]


def build(name, seed, skin, shirt, pants, shoes, eye_glow, eye_socket="#1a0f0f", hair=None,
          shirt_rows=12, sleeve_rows=5, pants_rows=12, blood=10, extra=None, mouth="#5a1010", skin_noise=0.14):
    s = Skin(seed)
    skin_c = hexc(skin)

    def skin_fn(face, fx, fy, w, h):
        c = s.noisy(skin_c, skin_noise)
        if face == "bottom":
            c = shade(c, 0.8)
        return c

    # Kopf
    s.fill_part(HEAD, skin_fn)
    if hair:
        hair_c = hexc(hair)

        def hair_fn(face, fx, fy, w, h):
            if face == "top":
                return s.noisy(hair_c, 0.2)
            if face in ("left", "right", "back") and fy < (3 if face != "back" else 6):
                return s.noisy(hair_c, 0.2)
            if face == "front" and fy < 2 and s.r.random() < 0.8:
                return s.noisy(hair_c, 0.2)
            return None
        s.fill_part(HEAD, hair_fn)

    # Gesicht
    fx0, fy0, _, _ = face_front()
    sock = hexc(eye_socket)
    glow = hexc(eye_glow)
    for ex in (1, 5):
        s.px(fx0 + ex, fy0 + 4, sock)
        s.px(fx0 + ex + 1, fy0 + 4, glow)
        s.px(fx0 + ex, fy0 + 3, shade(skin_c, 0.7))
        s.px(fx0 + ex + 1, fy0 + 3, shade(skin_c, 0.7))
        s.eyes.putpixel((fx0 + ex + 1, fy0 + 4), glow)
        s.eyes.putpixel((fx0 + ex, fy0 + 4), shade(glow, 0.55))
    s.px(fx0 + 3, fy0 + 5, shade(skin_c, 0.6))
    s.px(fx0 + 4, fy0 + 5, shade(skin_c, 0.6))
    m = hexc(mouth)
    for mx in range(2, 6):
        s.px(fx0 + mx, fy0 + 6, m if s.r.random() < 0.85 else shade(m, 1.4))
    s.px(fx0 + 2, fy0 + 7, shade(m, 0.8))
    s.px(fx0 + 5, fy0 + 7, shade(m, 0.8))

    # Körper / Arme / Beine
    shirt_c = hexc(shirt) if shirt else None
    pants_c = hexc(pants) if pants else None
    shoes_c = hexc(shoes) if shoes else None

    def body_fn(face, fx, fy, w, h):
        if shirt_c and (face in ("top", "bottom") or fy < shirt_rows):
            if s.r.random() < 0.07:
                return s.noisy(skin_c)  # Riss
            return s.noisy(shirt_c, 0.1)
        return skin_fn(face, fx, fy, w, h)

    def arm_fn(face, fx, fy, w, h):
        if shirt_c and (face == "top" or fy < sleeve_rows):
            return s.noisy(shirt_c, 0.1)
        return skin_fn(face, fx, fy, w, h)

    def leg_fn(face, fx, fy, w, h):
        if face == "bottom" or (shoes_c and fy >= h - 2):
            return s.noisy(shoes_c or hexc("#2a2a2a"), 0.1)
        if pants_c and (face == "top" or fy < pants_rows):
            if s.r.random() < 0.06:
                return s.noisy(skin_c)
            return s.noisy(pants_c, 0.1)
        return skin_fn(face, fx, fy, w, h)

    s.fill_part(BODY, body_fn)
    s.fill_part(ARM, arm_fn)
    s.fill_part(LEG, leg_fn)

    blood_c = hexc("#6e0f0f")
    s.splatter(BODY, blood_c, blood)
    s.splatter(ARM, blood_c, blood // 3 + 1)
    s.splatter(HEAD, blood_c, blood // 4, ("front", "left", "right"))
    s.splatter(LEG, blood_c, blood // 4)

    if extra:
        extra(s)
    s.save(name)
    return s


# --------------------------------------------------------------------------- Extras

def walker_extra(s):
    # Krawatte und Bisswunde am Hals
    x, y, w, h = BODY["front"]
    for yy in range(1, 7):
        s.px(x + 3, y + yy, hexc("#7a1c1c"))
        s.px(x + 4, y + yy, hexc("#8f2424"))
    s.px(x + 1, y, hexc("#4a0808"))
    s.px(x + 2, y, hexc("#4a0808"))


def runner_extra(s):
    # weiße Streifen am Trainingsanzug
    for part in (ARM, LEG):
        for name in ("left", "right"):
            x, y, w, h = part[name]
            for yy in range(h - 2):
                s.px(x + 1, y + yy, hexc("#e8e8e8"))
    x, y, w, h = BODY["front"]
    s.px(x + 5, y + 2, hexc("#e8e8e8"))
    s.px(x + 6, y + 2, hexc("#e8e8e8"))


def crawler_extra(s):
    # Rippen sichtbar, Beine zerrissen
    x, y, w, h = BODY["front"]
    for yy in (3, 5, 7):
        for xx in range(1, 7):
            if xx not in (3, 4):
                s.px(x + xx, y + yy, hexc("#d8d2c0"))
    for name in ("front", "back", "left", "right"):
        x, y, w, h = LEG[name]
        for yy in range(6, h):
            for xx in range(w):
                if s.r.random() < 0.5:
                    s.px(x + xx, y + yy, s.noisy(hexc("#5e1414"), 0.2))


def bloater_extra(s):
    # Eiterbeulen
    for part, n in ((BODY, 14), (HEAD, 5), (ARM, 5)):
        for _ in range(n):
            name = s.r.choice(("front", "back", "left", "right"))
            x, y, w, h = part[name]
            xx, yy = x + s.r.randrange(w), y + s.r.randrange(h)
            s.px(xx, yy, hexc("#d9e05a"))
            if xx + 1 < x + w:
                s.px(xx + 1, yy, hexc("#a8b03a"))


def spitter_extra(s):
    # Säure tropft aus dem Mund
    x, y, w, h = HEAD["front"]
    acid = hexc("#8cff3a")
    for xx, yy in ((3, 6), (4, 6), (3, 7), (4, 7), (2, 7)):
        s.px(x + xx, y + yy, acid)
        s.eyes.putpixel((x + xx, y + yy), shade(acid, 0.7))
    bx, by, bw, bh = BODY["front"]
    for yy in range(0, 5):
        s.px(bx + 4, by + yy, acid)
        s.eyes.putpixel((bx + 4, by + yy), shade(acid, 0.5))
    s.px(bx + 3, by + 1, acid)


def brute_extra(s):
    # Narben und Fesseln
    for name in ("front", "back", "left", "right"):
        x, y, w, h = ARM[name]
        for xx in range(w):
            s.px(x + xx, y + 9, hexc("#555555"))
            s.px(x + xx, y + 10, hexc("#777777"))
    x, y, w, h = HEAD["front"]
    for yy in range(0, 5):
        s.px(x + 6, y + yy, hexc("#3a1010"))


def screamer_extra(s):
    # weit aufgerissener Mund
    x, y, w, h = HEAD["front"]
    for yy in range(5, 8):
        for xx in range(2, 6):
            s.px(x + xx, y + yy, hexc("#050505"))
    s.px(x + 2, y + 5, hexc("#c8c0a8"))
    s.px(x + 5, y + 5, hexc("#c8c0a8"))


def soldier_extra(s):
    # Tarnmuster und Weste
    camo = [hexc("#4b5a2a"), hexc("#3a4520"), hexc("#6b6a3a"), hexc("#2e2a1c")]
    for part, rows in ((BODY, 12), (ARM, 6), (LEG, 10)):
        for name, (x, y, w, h) in part.items():
            for yy in range(min(rows, h)):
                for xx in range(w):
                    if s.r.random() < 0.35:
                        s.px(x + xx, y + yy, s.r.choice(camo))
    x, y, w, h = BODY["front"]
    for yy in range(1, 8):
        for xx in range(w):
            s.px(x + xx, y + yy, s.noisy(hexc("#3a3a30"), 0.08))
    for xx in (1, 2, 5, 6):
        s.px(x + xx, y + 4, hexc("#252520"))
    # Erkennungsmarke
    s.px(x + 3, y + 1, hexc("#c0c0c0"))


def leaper_extra(s):
    # Klauen
    for name in ("front", "back", "left", "right"):
        x, y, w, h = ARM[name]
        for xx in range(w):
            if xx % 2 == 0:
                s.px(x + xx, y + h - 1, hexc("#e8e2d0"))
    x, y, w, h = ARM["bottom"]
    for xx in range(w):
        for yy in range(h):
            s.px(x + xx, y + yy, hexc("#e8e2d0") if (xx + yy) % 2 == 0 else hexc("#4a1520"))


def toxic_extra(s):
    # Gasmaske + Strahlenwarnzeichen
    x, y, w, h = HEAD["front"]
    mask = hexc("#2a2d2a")
    for yy in range(3, 8):
        for xx in range(0, 8):
            s.px(x + xx, y + yy, s.noisy(mask, 0.1))
    glass = hexc("#9dff5a")
    for ex in (1, 5):
        s.px(x + ex, y + 4, glass)
        s.px(x + ex + 1, y + 4, glass)
        s.eyes.putpixel((x + ex, y + 4), glass)
        s.eyes.putpixel((x + ex + 1, y + 4), glass)
    for xx in (3, 4):
        s.px(x + xx, y + 6, hexc("#505550"))
        s.px(x + xx, y + 7, hexc("#707570"))
    bx, by, bw, bh = BODY["front"]
    sign = hexc("#1a1a1a")
    for xx, yy in ((3, 3), (4, 3), (2, 5), (5, 5), (3, 4), (4, 4), (3, 6), (4, 6)):
        s.px(bx + xx, by + yy, sign)
    # Leuchtende Flecken
    for _ in range(10):
        part = s.r.choice((BODY, ARM, LEG))
        name = s.r.choice(("front", "back", "left", "right"))
        px, py, pw, ph = part[name]
        xx, yy = px + s.r.randrange(pw), py + s.r.randrange(ph)
        s.px(xx, yy, glass)
        s.eyes.putpixel((xx, yy), shade(glass, 0.8))


def patient_zero_extra(s):
    # Freigelegtes Gehirn, Fixiergurte, OP-Nähte, Patientenarmband
    x, y, w, h = HEAD["top"]
    for yy in range(h):
        for xx in range(w):
            if s.r.random() < 0.75:
                s.px(x + xx, y + yy, s.noisy(hexc("#c86a7a"), 0.15))
    for name in ("left", "right", "back", "front"):
        fx, fy, fw, fh = HEAD[name]
        for xx in range(fw):
            if s.r.random() < 0.6:
                s.px(fx + xx, fy, s.noisy(hexc("#b85a6a"), 0.1))
    fx, fy, fw, fh = HEAD["front"]
    for yy in range(1, 7):
        s.px(fx + 1, fy + yy, hexc("#3a1a1a") if yy % 2 else hexc("#d8d0c4"))
    strap = hexc("#5a4a32")
    buckle = hexc("#c0c0c0")
    for name in ("front", "back", "left", "right"):
        bx, by, bw, bh = BODY[name]
        for xx in range(bw):
            s.px(bx + xx, by + 3, strap)
            s.px(bx + xx, by + 8, strap)
    bx, by, bw, bh = BODY["front"]
    s.px(bx + 4, by + 3, buckle)
    s.px(bx + 4, by + 8, buckle)
    for name in ("front", "back", "left", "right"):
        ax, ay, aw, ah = ARM[name]
        for xx in range(aw):
            s.px(ax + xx, ay + 9, strap)
        lx, ly, lw, lh = LEG[name]
        for xx in range(lw):
            s.px(lx + xx, ly + 4, strap)
    ax, ay, aw, ah = ARM["front"]
    s.px(ax + 1, ay + 11, hexc("#f0f0f0"))
    s.px(ax + 2, ay + 11, hexc("#f0f0f0"))


# --------------------------------------------------------------------------- Liste

ZOMBIES = [
    dict(name="walker", seed=1, skin="#6f8a4a", shirt="#3b5f8f", pants="#2b3a5c", shoes="#3a2a1a", eye_glow="#e8d27a",
         hair="#3a2a18", extra=walker_extra),
    dict(name="runner", seed=2, skin="#a9a98a", shirt="#9b1c1c", pants="#7a1515", shoes="#e0e0e0", eye_glow="#ff3030",
         sleeve_rows=12, extra=runner_extra),
    dict(name="crawler", seed=3, skin="#9a9a8c", shirt=None, pants="#4a4038", shoes=None, eye_glow="#f0f0f0", pants_rows=7,
         blood=18, hair="#2a2a2a", extra=crawler_extra),
    dict(name="bloater", seed=4, skin="#8e9a3c", shirt="#c9c0a0", pants="#5a4a30", shoes="#2a2018", eye_glow="#e6f040",
         shirt_rows=6, sleeve_rows=2, extra=bloater_extra),
    dict(name="spitter", seed=5, skin="#3f6a3a", shirt="#2a2a2a", pants="#1e1e1e", shoes="#111111", eye_glow="#8cff3a",
         mouth="#5aa020", extra=spitter_extra),
    dict(name="brute", seed=6, skin="#6a5a6e", shirt="#d0c8b8", pants="#3a3a44", shoes="#1a1a1a", eye_glow="#ff6a1a",
         shirt_rows=8, sleeve_rows=0, blood=20, extra=brute_extra),
    dict(name="screamer", seed=7, skin="#d8d4cc", shirt="#5a1a3a", pants="#2a2a2a", shoes="#1a1a1a", eye_glow="#b8f4ff",
         hair="#101010", eye_socket="#000000", extra=screamer_extra),
    dict(name="soldier", seed=8, skin="#7a8a5a", shirt="#4b5a2a", pants="#4b5a2a", shoes="#1a1a12", eye_glow="#ff2a2a",
         sleeve_rows=10, extra=soldier_extra),
    dict(name="leaper", seed=9, skin="#8a3a3a", shirt=None, pants="#2a2a2a", shoes=None, eye_glow="#ff3ad0", pants_rows=8,
         extra=leaper_extra),
    dict(name="patient_zero", seed=11, skin="#c8c0b4", shirt="#8fa8b8", pants="#8fa8b8", shoes=None, eye_glow="#ff1010",
         eye_socket="#200000", shirt_rows=12, sleeve_rows=4, pants_rows=8, blood=26, mouth="#3a0505", extra=patient_zero_extra),
    dict(name="toxic", seed=10, skin="#5a8a3a", shirt="#d6b822", pants="#c4a81e", shoes="#1a1a1a", eye_glow="#9dff5a",
         sleeve_rows=12, extra=toxic_extra),
]


def preview(skins):
    """Vorderansicht aller Zombies nebeneinander, 8x vergrößert."""
    sheet = Image.new("RGBA", (len(skins) * 20 * 8, 36 * 8), (40, 40, 48, 255))
    for i, s in enumerate(skins):
        img = s.img
        doll = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
        def cp(rect, dx, dy, flip=False):
            x, y, w, h = rect
            part = img.crop((x, y, x + w, y + h))
            if flip:
                part = part.transpose(Image.FLIP_LEFT_RIGHT)
            doll.paste(part, (dx, dy), part)
        cp(HEAD["front"], 4, 0)
        cp(BODY["front"], 4, 8)
        cp(ARM["front"], 0, 8)
        cp(ARM["front"], 12, 8, True)
        cp(LEG["front"], 4, 20)
        cp(LEG["front"], 8, 20, True)
        sheet.paste(doll.resize((16 * 8, 32 * 8), Image.NEAREST), (i * 160 + 16, 16))
    return sheet


if __name__ == "__main__":
    skins = [build(**z) for z in ZOMBIES]
    if PREVIEW:
        preview(skins).save(os.path.join(PREVIEW, "zombies_preview.png"))
    print("ok", len(skins))
