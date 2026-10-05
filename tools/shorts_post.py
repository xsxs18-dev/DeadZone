"""Nachbearbeitung des Shorts: mischt die Tonspur aus dem Sound-Protokoll und legt Texte, Farbkorrektur und Logo darüber.

Aufruf (mit numpy): python tools/shorts_post.py run/shorts
Ergebnis: run/shorts/DeadZone_Short.mp4
"""
import json
import math
import os
import subprocess
import sys
import random

import numpy as np

FPS = 60
LANG = os.environ.get("SHORTS_LANG", "de")
SR = 48000
ASSETS = os.path.expanduser("~/.gradle/caches/fabric-loom/assets")
INDEX = json.load(open(os.path.join(ASSETS, "indexes", "1.21.11-29.json")))["objects"]
FONT = "/usr/share/fonts/google-noto/NotoSans-CondensedBlack.ttf"
_cache = {}


def asset_path(key):
    h = INDEX[key]["hash"]
    return os.path.join(ASSETS, "objects", h[:2], h)


def decode(key):
    if key not in _cache:
        raw = subprocess.run(["ffmpeg", "-v", "error", "-i", asset_path(key), "-f", "f32le", "-ac", "1", "-ar", str(SR), "-"],
                             check=True, capture_output=True).stdout
        _cache[key] = np.frombuffer(raw, dtype=np.float32)
    return _cache[key]


def place(mix, samples, t, gl, gr, pitch=1.0):
    if pitch != 1.0:
        n = max(1, int(len(samples) / pitch))
        samples = np.interp(np.arange(n) * pitch, np.arange(len(samples)), samples).astype(np.float32)
    start = int(t * SR)
    if start >= mix.shape[1]:
        return
    end = min(mix.shape[1], start + len(samples))
    mix[0, start:end] += samples[:end - start] * gl
    mix[1, start:end] += samples[:end - start] * gr


def build_audio(folder, total_frames, shots):
    total = total_frames / FPS + 0.5
    mix = np.zeros((2, int(total * SR)), dtype=np.float32)
    rnd = random.Random(1)

    # Spielsounds an exakt der Stelle, an der sie im Bild passieren
    count = 0
    with open(os.path.join(folder, "sounds.jsonl")) as f:
        for line in f:
            e = json.loads(line)
            loc = e["loc"]
            ns, path = loc.split(":", 1)
            key = f"{ns}/{path}"
            if key not in INDEX:
                continue
            vol = min(1.0, e["vol"])
            gl = gr = vol
            if not e["rel"] and e["range"] > 0:
                dx, dy, dz = e["x"] - e["cx"], e["y"] - e["cy"], e["z"] - e["cz"]
                dist = math.sqrt(dx * dx + dy * dy + dz * dz)
                att = max(0.0, 1.0 - dist / e["range"])
                if att <= 0:
                    continue
                yaw = math.radians(e["yaw"])
                right = (-math.cos(yaw), -math.sin(yaw))
                hd = math.sqrt(dx * dx + dz * dz)
                pan = 0.0 if hd < 0.01 else (dx * right[0] + dz * right[1]) / hd
                pan *= 0.7
                gl = vol * att * math.sqrt((1 - pan) / 2) * 1.41
                gr = vol * att * math.sqrt((1 + pan) / 2) * 1.41
            place(mix, decode(key), e["f"] / FPS, gl * 0.8, gr * 0.8, max(0.5, min(2.0, e["pitch"])))
            count += 1
    print("sounds placed:", count)

    # Atmosphäre: dunkles Höhlen-Rauschen leise unter allem
    t = 0.0
    while t < total:
        key = f"minecraft/sounds/ambient/cave/cave{rnd.randint(1, 19)}.ogg"
        s = decode(key)
        place(mix, s, t, 0.12, 0.12, 0.8)
        t += len(s) / SR / 0.8 * 0.7
    # Bass-Schlag zum Start und dumpfe Schläge bei harten Schnitten
    boom = decode("minecraft/sounds/random/explode2.ogg")
    place(mix, boom, 0.0, 0.55, 0.55, 0.5)
    for s in shots:
        if s["name"] in ("horde", "shed", "boss", "outro"):
            place(mix, boom, s["start"] / FPS, 0.35, 0.35, 0.55)
    thunder = decode("minecraft/sounds/ambient/weather/thunder2.ogg")
    for s in shots:
        if s["name"] == "outro":
            place(mix, thunder, s["start"] / FPS + 0.1, 0.4, 0.4, 0.9)

    # weicher Limiter
    peak = np.max(np.abs(mix)) or 1.0
    mix = np.tanh(mix / peak * 1.6) * 0.95
    wav = os.path.join(folder, "audio.wav")
    pcm = (mix.T * 32767).astype(np.int16).tobytes()
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "s16le", "-ar", str(SR), "-ac", "2", "-i", "-", wav], input=pcm, check=True)
    return wav


def esc(text):
    return text.replace("\\", "\\\\").replace(":", "\\:").replace("'", "’").replace("%", "\\%")


def caption(text, start, end, y="h*0.15", size=104, color="white", border=12):
    a = f"if(lt(t,{start:.3f}+0.12),(t-{start:.3f})/0.12,if(gt(t,{end:.3f}-0.12),({end:.3f}-t)/0.12,1))"
    return (f"drawtext=fontfile={FONT}:text='{esc(text)}':fontsize={size}:fontcolor={color}:borderw={border}:bordercolor=black"
            f":shadowcolor=black@0.6:shadowx=6:shadowy=6:x=(w-text_w)/2:y={y}:alpha='{a}':enable='between(t,{start:.3f},{end:.3f})'")


def build_video(folder, shots, wav, logo):
    by_name = {s["name"]: s for s in shots}

    def span(name, a=0.0, b=None):
        s = by_name[name]
        start = s["start"] / FPS + a
        end = (s["start"] + s["frames"]) / FPS if b is None else s["start"] / FPS + b
        return start, end

    en = LANG == "en"
    T = (lambda de, eng: eng if en else de)
    caps = []
    st, end = span("hook", 0.15)
    caps.append(caption(T("ICH HABE DIE KRASSESTE", "I BUILT THE CRAZIEST"), st, end, y="h*0.10", size=88))
    caps.append(caption("ZOMBIE-APOKALYPSE" if not en else "ZOMBIE APOCALYPSE", st + 0.35, end, y="h*0.10+112", size=108, color="0xFF3030"))
    caps.append(caption(T("MOD GEBAUT", "MOD"), st + 0.7, end, y="h*0.10+240", size=108))
    caps.append(caption(T("RIESIGE ZERSTÖRTE", "HUGE DESTROYED"), *span("street", 0.1), y="h*0.13"))
    caps.append(caption(T("MEGA-STÄDTE", "MEGA CITIES"), *span("street", 0.3), y="h*0.13+125", color="0xFFD23A"))
    caps.append(caption(T("BOMBENKRATER", "BOMB CRATERS"), *span("crater", 0.1)))
    caps.append(caption(T("EINGESTÜRZTE", "COLLAPSED"), *span("highway", 0.1), y="h*0.13"))
    caps.append(caption("HIGHWAYS", *span("highway", 0.25), y="h*0.13+125", color="0xFFD23A"))
    caps.append(caption(T("BLUTMOND", "BLOOD MOON"), *span("horde", 0.1, 3.0), size=150, color="0xE01010"))
    caps.append(caption(T("10 ZOMBIE-ARTEN", "10 ZOMBIE TYPES"), *span("horde", 3.0), color="0x9DFF5A"))
    caps.append(caption(T("20 WAFFEN", "20 GUNS"), *span("fight", 0.1, 4.1), y="h*0.11"))
    caps.append(caption("RPG!", *span("fight", 4.4, 5.2), y="h*0.11", size=150, color="0xFF8A1E"))
    caps.append(caption(T("MOLOTOWS", "MOLOTOVS"), *span("fight", 5.3), y="h*0.11", color="0xFF8A1E"))
    caps.append(caption(T("UNTER DIESER HÜTTE…", "UNDER THIS SHACK…"), *span("shed", 0.05)))
    caps.append(caption(T("…EIN GEHEIMBUNKER", "…A SECRET BUNKER"), *span("shaft", 0.1)))
    caps.append(caption("BOSS:", *span("boss", 1.7), y="h*0.11", size=96))
    caps.append(caption(T("SUBJEKT NULL", "PATIENT ZERO"), *span("boss", 1.85), y="h*0.11+115", size=130, color="0xE01010"))
    ost, oen = span("outro", 0.2)
    caps.append(caption("LINK IN BIO", ost + 0.6, oen, y="h*0.62", size=88, color="0xFFD23A"))
    caps.append(caption(T("FOLGEN FÜR TEIL 2", "FOLLOW FOR PART 2"), ost + 0.9, oen, y="h*0.62+110", size=72))

    logo_start = ost
    grade = "eq=contrast=1.06:saturation=1.2:gamma=1.12:brightness=0.02,vignette=angle=PI/6"
    filt = (f"[0:v]{grade},{','.join(caps)}[v1];"
            f"[2:v]scale=960:-1,format=rgba,fade=t=in:st=0:d=0.3:alpha=1[lg];"
            f"[v1][lg]overlay=(W-w)/2:H*0.38:enable='gte(t,{logo_start:.3f})':shortest=0[v2];"
            f"[v2]fade=t=out:st={oen - 0.35:.3f}:d=0.35[v]")
    out = os.path.join(folder, "DeadZone_Short.mp4" if LANG == "de" else "DeadZone_Short_EN.mp4")
    cmd = ["ffmpeg", "-v", "error", "-y", "-i", os.path.join(folder, "raw.mp4"), "-i", wav,
           "-loop", "1", "-t", f"{oen - logo_start + 1:.3f}", "-itsoffset", f"{logo_start:.3f}", "-i", logo,
           "-filter_complex", filt, "-map", "[v]", "-map", "1:a",
           "-c:v", "libx264", "-preset", "slow", "-crf", "17", "-pix_fmt", "yuv420p", "-r", str(FPS),
           "-c:a", "aac", "-b:a", "192k", "-af", "loudnorm=I=-14:TP=-1.0:LRA=11",
           "-shortest", "-movflags", "+faststart", out]
    subprocess.run(cmd, check=True)
    return out


if __name__ == "__main__":
    folder = sys.argv[1]
    shots = json.load(open(os.path.join(folder, "shots.json")))
    total = shots[-1]["start"] + shots[-1]["frames"]
    wav = build_audio(folder, total, shots)
    logo = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "minecraft", "textures", "gui", "title", "minecraft.png")
    print(build_video(folder, shots, wav, logo))
