"""Erzeugt alle JSON-Dateien: Item-Modelle, Sprachen, Loot-Tabellen, Rezepte, Schadensarten, Strukturen.

Aufruf: python3 tools/gen_data.py  (aus dem deadzone-Ordner)
"""
import json
import os

RES = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
NS = "deadzone"

GUNS = ["pistol_m9", "revolver", "desert_eagle", "uzi", "mp5", "p90", "ak47", "m4a1", "scar", "dmr",
        "pump_shotgun", "double_barrel", "auto_shotgun", "hunting_rifle", "sniper_rifle", "barrett",
        "m249", "minigun", "grenade_launcher", "rpg"]
KNIVES = ["kitchen_knife", "combat_knife", "machete", "karambit"]
AMMO = ["ammo_9mm", "ammo_556", "shotgun_shell", "ammo_50cal", "explosive_round"]
ZOMBIES = ["walker", "runner", "crawler", "bloater", "spitter", "brute", "screamer", "soldier", "leaper", "toxic", "patient_zero"]
EXTRA = ["grenade", "molotov", "bandage", "medkit", "immune_serum"]
ARMOR = ["military_helmet", "military_vest", "military_pants", "military_boots", "gas_mask", "nv_goggles"]


def write(path, data):
    full = os.path.join(RES, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


# --------------------------------------------------------------------------- Modelle

def models():
    # Wie eine geladene Armbrust gehalten, aber um 45° gedreht, weil die Waffen-Texturen waagerecht statt diagonal sind.
    write(f"assets/{NS}/models/item/gun.json", {
        "parent": "minecraft:item/generated",
        "display": {
            "thirdperson_righthand": {"rotation": [-90, 0, -15], "translation": [2, 0.1, -3], "scale": [0.9, 0.9, 0.9]},
            "thirdperson_lefthand": {"rotation": [-90, 0, 75], "translation": [2, 0.1, -3], "scale": [0.9, 0.9, 0.9]},
            "firstperson_righthand": {"rotation": [-90, 0, 80], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
            "firstperson_lefthand": {"rotation": [-90, 0, 170], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
        },
    })
    for name in GUNS:
        item(name, f"{NS}:item/gun")
    for name in KNIVES:
        item(name, "minecraft:item/handheld")
    for name in AMMO + EXTRA + ARMOR + ["antidote"] + [z + "_spawn_egg" for z in ZOMBIES]:
        item(name, "minecraft:item/generated")


def item(name, parent):
    write(f"assets/{NS}/models/item/{name}.json", {"parent": parent, "textures": {"layer0": f"{NS}:item/{name}"}})
    write(f"assets/{NS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}})


# --------------------------------------------------------------------------- Sprachen

DE_NAMES = {
    "pistol_m9": "M9 Pistole", "revolver": "Revolver .357", "desert_eagle": "Desert Eagle", "uzi": "Uzi", "mp5": "MP5",
    "p90": "P90", "ak47": "AK-47", "m4a1": "M4A1", "scar": "SCAR-H", "dmr": "Präzisionsgewehr (DMR)",
    "pump_shotgun": "Pump-Schrotflinte", "double_barrel": "Doppelläufige Flinte", "auto_shotgun": "AA-12 Auto-Schrotflinte",
    "hunting_rifle": "Jagdgewehr", "sniper_rifle": "Scharfschützengewehr", "barrett": "Barrett .50", "m249": "M249 MG",
    "minigun": "Minigun", "grenade_launcher": "Granatwerfer", "rpg": "RPG-7",
    "kitchen_knife": "Küchenmesser", "combat_knife": "Kampfmesser", "machete": "Machete", "karambit": "Karambit",
    "ammo_9mm": "9mm-Munition", "ammo_556": "5.56mm-Munition", "shotgun_shell": "Schrotpatrone",
    "ammo_50cal": ".50 BMG-Munition", "explosive_round": "Sprenggeschoss", "antidote": "Grünes Gegenmittel",
    "grenade": "Splittergranate", "molotov": "Molotow-Cocktail", "bandage": "Verband", "medkit": "Medikit",
    "immune_serum": "Immunserum", "military_helmet": "Gefechtshelm", "military_vest": "Schutzweste",
    "military_pants": "Kampfhose", "military_boots": "Kampfstiefel", "gas_mask": "Gasmaske", "nv_goggles": "Nachtsichtbrille",
}
EN_NAMES = {
    "pistol_m9": "M9 Pistol", "revolver": ".357 Revolver", "desert_eagle": "Desert Eagle", "uzi": "Uzi", "mp5": "MP5",
    "p90": "P90", "ak47": "AK-47", "m4a1": "M4A1", "scar": "SCAR-H", "dmr": "Marksman Rifle (DMR)",
    "pump_shotgun": "Pump Shotgun", "double_barrel": "Double-Barrel Shotgun", "auto_shotgun": "AA-12 Auto Shotgun",
    "hunting_rifle": "Hunting Rifle", "sniper_rifle": "Sniper Rifle", "barrett": "Barrett .50", "m249": "M249 LMG",
    "minigun": "Minigun", "grenade_launcher": "Grenade Launcher", "rpg": "RPG-7",
    "kitchen_knife": "Kitchen Knife", "combat_knife": "Combat Knife", "machete": "Machete", "karambit": "Karambit",
    "ammo_9mm": "9mm Ammo", "ammo_556": "5.56mm Ammo", "shotgun_shell": "Shotgun Shell",
    "ammo_50cal": ".50 BMG Ammo", "explosive_round": "Explosive Round", "antidote": "Green Antidote",
    "grenade": "Frag Grenade", "molotov": "Molotov Cocktail", "bandage": "Bandage", "medkit": "Medkit",
    "immune_serum": "Immune Serum", "military_helmet": "Combat Helmet", "military_vest": "Body Armor Vest",
    "military_pants": "Combat Pants", "military_boots": "Combat Boots", "gas_mask": "Gas Mask", "nv_goggles": "Night Vision Goggles",
}
DE_ZOMBIES = {
    "walker": "Streuner", "runner": "Sprinter", "crawler": "Kriecher", "bloater": "Platzer", "spitter": "Spucker",
    "brute": "Koloss", "screamer": "Schreier", "soldier": "Soldaten-Zombie", "leaper": "Springer", "toxic": "Strahlen-Zombie",
    "patient_zero": "Subjekt Null", "grenade": "Granate", "molotov": "Molotow-Cocktail",
}
EN_ZOMBIES = {
    "walker": "Walker", "runner": "Runner", "crawler": "Crawler", "bloater": "Bloater", "spitter": "Spitter",
    "brute": "Brute", "screamer": "Screamer", "soldier": "Soldier Zombie", "leaper": "Leaper", "toxic": "Toxic Zombie",
    "patient_zero": "Patient Zero", "grenade": "Grenade", "molotov": "Molotov Cocktail",
}

DE_TEXT = {
    "itemGroup.deadzone": "DeadZone",
    "effect.deadzone.infected": "Infiziert",
    "tooltip.deadzone.magazine": "Magazin: %s / %s",
    "tooltip.deadzone.damage": "Schaden: %s",
    "tooltip.deadzone.rate": "Feuerrate: %s Schuss/s",
    "tooltip.deadzone.ammo": "Munition: %s",
    "tooltip.deadzone.automatic": "Vollautomatisch",
    "tooltip.deadzone.pierce": "Durchschlägt bis zu %s Gegner",
    "tooltip.deadzone.explosive": "Explosiv",
    "tooltip.deadzone.controls": "Rechtsklick: schießen · Schleichen + Rechtsklick: nachladen",
    "tooltip.deadzone.antidote": "Heilt die Zombie-Infektion",
    "hud.deadzone.no_ammo": "Keine Munition: %s",
    "hud.deadzone.reloading": "Lädt nach…",
    "hud.deadzone.headshot": "KOPFSCHUSS!",
    "hud.deadzone.hits": "Zombie-Treffer: %s / %s",
    "hud.deadzone.risk": "⚠ Infektionsgefahr! %s%% pro Treffer",
    "hud.deadzone.cured": "Infektion geheilt!",
    "hud.deadzone.cured_hits": "Gegenmittel getrunken – Treffer-Zähler zurückgesetzt",
    "title.deadzone.infected": "☣ INFIZIERT ☣",
    "title.deadzone.infected.sub": "Finde in 20 Minuten das grüne Gegenmittel!",
    "chat.deadzone.infection_warning": "☣ Noch %s Minuten bis zur Verwandlung. Trink ein grünes Gegenmittel!",
    "death.attack.deadzone.bullet": "%1$s wurde erschossen",
    "death.attack.deadzone.bullet.player": "%1$s wurde von %2$s erschossen",
    "death.attack.deadzone.bullet.item": "%1$s wurde von %2$s mit %3$s erschossen",
    "death.attack.deadzone.infection": "%1$s hat sich in einen Zombie verwandelt",
    "death.attack.deadzone.infection.player": "%1$s hat sich auf der Flucht vor %2$s in einen Zombie verwandelt",
    "menu.deadzone.tagline": "DeadZone 1.1 – Überlebe die Nacht",
    "tooltip.deadzone.bandage": "Heilt 3 Herzen und stoppt Gift",
    "tooltip.deadzone.medkit": "Heilt komplett, Regeneration",
    "tooltip.deadzone.serum": "Macht dich für immer immun gegen die Infektion",
    "title.deadzone.immune": "IMMUN",
    "title.deadzone.immune.sub": "Die Infektion kann dir nichts mehr anhaben",
    "title.deadzone.blood_moon": "☾ BLUTMOND ☽",
    "title.deadzone.blood_moon.sub": "Die Horde kommt. Verbarrikadier dich!",
}
EN_TEXT = {
    "itemGroup.deadzone": "DeadZone",
    "effect.deadzone.infected": "Infected",
    "tooltip.deadzone.magazine": "Magazine: %s / %s",
    "tooltip.deadzone.damage": "Damage: %s",
    "tooltip.deadzone.rate": "Fire rate: %s shots/s",
    "tooltip.deadzone.ammo": "Ammo: %s",
    "tooltip.deadzone.automatic": "Fully automatic",
    "tooltip.deadzone.pierce": "Pierces up to %s targets",
    "tooltip.deadzone.explosive": "Explosive",
    "tooltip.deadzone.controls": "Right-click: shoot · Sneak + right-click: reload",
    "tooltip.deadzone.antidote": "Cures the zombie infection",
    "hud.deadzone.no_ammo": "Out of ammo: %s",
    "hud.deadzone.reloading": "Reloading…",
    "hud.deadzone.headshot": "HEADSHOT!",
    "hud.deadzone.hits": "Zombie hits: %s / %s",
    "hud.deadzone.risk": "⚠ Infection risk! %s%% per hit",
    "hud.deadzone.cured": "Infection cured!",
    "hud.deadzone.cured_hits": "Antidote taken – hit counter reset",
    "title.deadzone.infected": "☣ INFECTED ☣",
    "title.deadzone.infected.sub": "Find the green antidote within 20 minutes!",
    "chat.deadzone.infection_warning": "☣ %s minutes until you turn. Drink a green antidote!",
    "death.attack.deadzone.bullet": "%1$s was shot",
    "death.attack.deadzone.bullet.player": "%1$s was shot by %2$s",
    "death.attack.deadzone.bullet.item": "%1$s was shot by %2$s using %3$s",
    "death.attack.deadzone.infection": "%1$s turned into a zombie",
    "death.attack.deadzone.infection.player": "%1$s turned into a zombie while fleeing from %2$s",
    "menu.deadzone.tagline": "DeadZone 1.1 – Survive the night",
    "tooltip.deadzone.bandage": "Heals 3 hearts and stops poison",
    "tooltip.deadzone.medkit": "Fully heals, grants regeneration",
    "tooltip.deadzone.serum": "Makes you permanently immune to the infection",
    "title.deadzone.immune": "IMMUNE",
    "title.deadzone.immune.sub": "The infection can no longer harm you",
    "title.deadzone.blood_moon": "☾ BLOOD MOON ☽",
    "title.deadzone.blood_moon.sub": "The horde is coming. Barricade yourself!",
}


def lang():
    for code, names, znames, text, egg in (
            ("de_de", DE_NAMES, DE_ZOMBIES, DE_TEXT, "%s-Spawn-Ei"),
            ("en_us", EN_NAMES, EN_ZOMBIES, EN_TEXT, "%s Spawn Egg")):
        out = dict(text)
        for k, v in names.items():
            out[f"item.{NS}.{k}"] = v
        for k, v in znames.items():
            out[f"entity.{NS}.{k}"] = v
            if k in ZOMBIES:
                out[f"item.{NS}.{k}_spawn_egg"] = egg % v
        write(f"assets/{NS}/lang/{code}.json", out)


# --------------------------------------------------------------------------- Ausrüstung

def equipment():
    write(f"assets/{NS}/equipment/military.json", {"layers": {
        "humanoid": [{"texture": f"{NS}:military"}], "humanoid_leggings": [{"texture": f"{NS}:military"}]}})
    for name in ("gas_mask", "nv_goggles"):
        write(f"assets/{NS}/equipment/{name}.json", {"layers": {"humanoid": [{"texture": f"{NS}:{name}"}]}})
    write(f"data/{NS}/tags/item/repairs_military.json", {"values": ["minecraft:iron_ingot"]})
    zombies = [f"{NS}:{z}" for z in ZOMBIES]
    for tag in ("zombies", "undead", "sensitive_to_smite"):
        write(f"data/minecraft/tags/entity_type/{tag}.json", {"replace": False, "values": zombies})


# --------------------------------------------------------------------------- Schaden

def damage():
    write(f"data/{NS}/damage_type/bullet.json", {"message_id": f"{NS}.bullet", "exhaustion": 0.1, "scaling": "when_caused_by_living_non_player"})
    write(f"data/{NS}/damage_type/infection.json", {"message_id": f"{NS}.infection", "exhaustion": 0.0, "scaling": "never"})
    for tag in ("bypasses_armor", "bypasses_invulnerability", "bypasses_effects", "bypasses_resistance", "bypasses_enchantments",
                "bypasses_shield", "no_knockback"):
        write(f"data/minecraft/tags/damage_type/{tag}.json", {"replace": False, "values": [f"{NS}:infection"]})
    write("data/minecraft/tags/damage_type/is_projectile.json", {"replace": False, "values": [f"{NS}:bullet"]})


# --------------------------------------------------------------------------- Loot

def entry(name, weight, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    if hi > 1:
        e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


def chest_table(rolls, items, antidote=0.08):
    pools = [{"rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]},
              "entries": [entry(*i) for i in items]}]
    pools.append({"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": antidote}],
                  "entries": [entry(f"{NS}:antidote", 1)]})
    return {"type": "minecraft:chest", "pools": pools}


def d(n):
    return f"{NS}:{n}"


def loot():
    food = [("minecraft:bread", 6, 1, 3), ("minecraft:apple", 5, 1, 3), ("minecraft:cooked_beef", 3, 1, 2),
            ("minecraft:carrot", 4, 1, 4), ("minecraft:baked_potato", 4, 1, 3), ("minecraft:rotten_flesh", 4, 1, 4)]
    tables = {
        "house": chest_table((3, 6), food + [
            ("minecraft:torch", 5, 2, 6), ("minecraft:paper", 3, 1, 4), ("minecraft:string", 3, 1, 3), ("minecraft:iron_nugget", 3, 2, 6),
            (d("ammo_9mm"), 10, 4, 12), (d("shotgun_shell"), 6, 2, 6), (d("kitchen_knife"), 4), (d("combat_knife"), 2),
            (d("pistol_m9"), 3), (d("revolver"), 2), (d("pump_shotgun"), 2), (d("double_barrel"), 2), (d("uzi"), 1),
            (d("bandage"), 5, 1, 3), (d("molotov"), 2, 1, 2)]),
        "city": chest_table((3, 7), food + [
            ("minecraft:iron_ingot", 3, 1, 3), ("minecraft:gunpowder", 3, 1, 3), ("minecraft:compass", 1),
            (d("ammo_9mm"), 10, 6, 16), (d("ammo_556"), 6, 4, 12), (d("shotgun_shell"), 6, 3, 8),
            (d("machete"), 2), (d("combat_knife"), 3), (d("pistol_m9"), 3), (d("desert_eagle"), 2), (d("uzi"), 2),
            (d("mp5"), 2), (d("ak47"), 1), (d("hunting_rifle"), 1), (d("pump_shotgun"), 2), (d("p90"), 1),
            (d("bandage"), 4, 1, 3), (d("molotov"), 3, 1, 2), (d("grenade"), 1, 1, 2)]),
        "bunker": chest_table((4, 8), [
            ("minecraft:cooked_beef", 5, 2, 5), ("minecraft:bread", 5, 2, 5), ("minecraft:golden_apple", 1),
            ("minecraft:iron_ingot", 4, 2, 5), ("minecraft:redstone", 3, 2, 6), ("minecraft:gunpowder", 4, 2, 5),
            (d("ammo_9mm"), 8, 8, 24), (d("ammo_556"), 8, 8, 20), (d("shotgun_shell"), 5, 4, 10), (d("ammo_50cal"), 3, 2, 6),
            (d("explosive_round"), 1, 1, 2), (d("karambit"), 2), (d("combat_knife"), 3),
            (d("mp5"), 3), (d("m4a1"), 2), (d("scar"), 1), (d("dmr"), 1), (d("sniper_rifle"), 1), (d("auto_shotgun"), 1),
            (d("medkit"), 2), (d("bandage"), 4, 1, 4), (d("gas_mask"), 1), (d("nv_goggles"), 1), (d("grenade"), 2, 1, 3)]),
        "military": chest_table((4, 8), [
            ("minecraft:iron_ingot", 4, 2, 6), ("minecraft:iron_helmet", 2), ("minecraft:iron_chestplate", 1), ("minecraft:tnt", 2, 1, 3),
            ("minecraft:cooked_beef", 4, 2, 4), ("minecraft:spyglass", 1),
            (d("ammo_556"), 10, 10, 30), (d("ammo_9mm"), 6, 8, 20), (d("ammo_50cal"), 4, 3, 8), (d("explosive_round"), 2, 1, 4),
            (d("shotgun_shell"), 4, 4, 12), (d("combat_knife"), 3), (d("karambit"), 1),
            (d("m4a1"), 3), (d("ak47"), 3), (d("scar"), 2), (d("dmr"), 2), (d("sniper_rifle"), 2), (d("barrett"), 1),
            (d("m249"), 1), (d("minigun"), 1), (d("grenade_launcher"), 1), (d("rpg"), 1), (d("auto_shotgun"), 1),
            (d("grenade"), 4, 1, 4), (d("military_helmet"), 2), (d("military_vest"), 2), (d("military_pants"), 2),
            (d("military_boots"), 2), (d("nv_goggles"), 1), (d("gas_mask"), 1), (d("medkit"), 2), (d("bandage"), 3, 1, 3)]),
        "tower": chest_table((3, 6), [
            ("minecraft:bread", 4, 1, 3), ("minecraft:spyglass", 2), ("minecraft:arrow", 3, 4, 12), ("minecraft:torch", 3, 2, 6),
            (d("ammo_9mm"), 6, 4, 12), (d("ammo_556"), 6, 4, 12), (d("ammo_50cal"), 3, 2, 5),
            (d("hunting_rifle"), 3), (d("sniper_rifle"), 1), (d("dmr"), 1), (d("revolver"), 2), (d("machete"), 2)]),
    }
    tables.update({
        "hospital": chest_table((3, 6), [
            (d("bandage"), 10, 1, 4), (d("medkit"), 5), (d("gas_mask"), 2), ("minecraft:paper", 3, 1, 4),
            ("minecraft:glass_bottle", 3, 1, 3), ("minecraft:golden_apple", 1), ("minecraft:honey_bottle", 3, 1, 2),
            (d("kitchen_knife"), 2)]),
        "police": chest_table((3, 7), [
            (d("ammo_9mm"), 10, 8, 20), (d("shotgun_shell"), 8, 4, 10), (d("ammo_556"), 4, 4, 12),
            (d("pistol_m9"), 4), (d("revolver"), 3), (d("desert_eagle"), 2), (d("pump_shotgun"), 3), (d("mp5"), 2), (d("m4a1"), 1),
            (d("military_helmet"), 2), (d("military_vest"), 2), (d("combat_knife"), 3), (d("grenade"), 2, 1, 2), (d("bandage"), 3, 1, 2)]),
        "supermarket": chest_table((3, 6), [
            ("minecraft:bread", 8, 2, 6), ("minecraft:apple", 6, 2, 5), ("minecraft:cooked_beef", 4, 1, 4), ("minecraft:cookie", 4, 2, 8),
            ("minecraft:carrot", 5, 2, 6), ("minecraft:baked_potato", 5, 2, 5), ("minecraft:melon_slice", 4, 2, 6),
            ("minecraft:honey_bottle", 2, 1, 2), ("minecraft:glass_bottle", 3, 1, 4), (d("kitchen_knife"), 2), (d("bandage"), 2, 1, 2)]),
        "gas_station": chest_table((3, 6), [
            (d("molotov"), 6, 1, 3), ("minecraft:coal", 5, 2, 6), ("minecraft:bread", 4, 1, 3), ("minecraft:glass_bottle", 3, 1, 4),
            (d("ammo_9mm"), 4, 4, 10), (d("pistol_m9"), 2), (d("machete"), 2), ("minecraft:flint_and_steel", 2)]),
        "secret_vault": chest_table((5, 9), [
            (d("ammo_556"), 8, 16, 40), (d("ammo_50cal"), 6, 6, 16), (d("explosive_round"), 5, 2, 6), (d("grenade"), 5, 2, 5),
            (d("barrett"), 2), (d("minigun"), 2), (d("rpg"), 2), (d("grenade_launcher"), 2), (d("auto_shotgun"), 2), (d("scar"), 3),
            (d("military_helmet"), 3), (d("military_vest"), 3), (d("military_pants"), 3), (d("military_boots"), 3),
            (d("nv_goggles"), 2), (d("gas_mask"), 2), (d("medkit"), 4, 1, 2), ("minecraft:golden_apple", 2, 1, 2),
            ("minecraft:enchanted_golden_apple", 1), ("minecraft:diamond", 2, 1, 3)]),
        "secret_lab": chest_table((3, 6), [
            (d("medkit"), 6, 1, 2), (d("bandage"), 6, 2, 5), (d("gas_mask"), 3), ("minecraft:glass_bottle", 4, 2, 5),
            ("minecraft:fermented_spider_eye", 3, 1, 3), ("minecraft:golden_carrot", 3, 1, 4), ("minecraft:experience_bottle", 4, 2, 6)],
            antidote=0.5),
    })
    for name, table in tables.items():
        write(f"data/{NS}/loot_table/chests/{name}.json", table)

    looting = {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
               "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}

    def ent(extra_pools):
        pools = [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:rotten_flesh", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 2}}, looting]}]}]
        for name, chance, lo, hi in extra_pools:
            pools.append({"rolls": 1, "conditions": [{"condition": "minecraft:killed_by_player"},
                                                     {"condition": "minecraft:random_chance", "chance": chance}],
                          "entries": [entry(name, 1, lo, hi)]})
        return {"type": "minecraft:entity", "pools": pools}

    drops = {
        "walker": [(d("ammo_9mm"), 0.08, 2, 6)],
        "runner": [(d("ammo_9mm"), 0.06, 2, 5)],
        "crawler": [(d("shotgun_shell"), 0.06, 1, 3)],
        "bloater": [("minecraft:gunpowder", 0.5, 1, 3)],
        "spitter": [("minecraft:slime_ball", 0.3, 1, 2)],
        "brute": [("minecraft:iron_ingot", 0.6, 1, 3), (d("shotgun_shell"), 0.4, 3, 8), (d("antidote"), 0.05, 1, 1)],
        "screamer": [(d("ammo_9mm"), 0.15, 3, 8)],
        "soldier": [(d("ammo_556"), 0.5, 3, 10), (d("ammo_9mm"), 0.3, 3, 8), (d("combat_knife"), 0.03, 1, 1), (d("pistol_m9"), 0.03, 1, 1)],
        "leaper": [("minecraft:bone", 0.4, 1, 2)],
        "toxic": [("minecraft:glowstone_dust", 0.4, 1, 3), (d("ammo_556"), 0.1, 2, 6), (d("gas_mask"), 0.02, 1, 1)],
        "patient_zero": [(d("immune_serum"), 1.0, 1, 1), (d("antidote"), 1.0, 2, 3), (d("explosive_round"), 1.0, 4, 8),
                         (d("barrett"), 0.5, 1, 1), (d("minigun"), 0.5, 1, 1), ("minecraft:diamond", 1.0, 3, 6)],
    }
    for name, extra in drops.items():
        write(f"data/{NS}/loot_table/entities/{name}.json", ent(extra))


# --------------------------------------------------------------------------- Rezepte

def recipes():
    def shapeless(name, ingredients, result, count):
        write(f"data/{NS}/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                                "ingredients": ingredients, "result": {"id": result, "count": count}})
    shapeless("ammo_9mm", ["minecraft:iron_ingot", "minecraft:gunpowder"], d("ammo_9mm"), 16)
    shapeless("ammo_556", ["minecraft:iron_ingot", "minecraft:gunpowder", "minecraft:copper_ingot"], d("ammo_556"), 12)
    shapeless("shotgun_shell", ["minecraft:iron_nugget", "minecraft:gunpowder", "minecraft:paper"], d("shotgun_shell"), 8)
    shapeless("ammo_50cal", ["minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:gunpowder", "minecraft:gunpowder", "minecraft:copper_ingot"], d("ammo_50cal"), 4)
    shapeless("explosive_round", ["minecraft:tnt", "minecraft:iron_ingot", "minecraft:gunpowder"], d("explosive_round"), 2)
    shapeless("grenade", ["minecraft:iron_ingot", "minecraft:gunpowder", "minecraft:gunpowder", "minecraft:iron_nugget"], d("grenade"), 2)
    shapeless("molotov", ["minecraft:glass_bottle", "minecraft:coal", "minecraft:string", "minecraft:paper"], d("molotov"), 1)
    shapeless("bandage", ["minecraft:paper", "minecraft:paper", "minecraft:string"], d("bandage"), 2)
    shapeless("medkit", [d("bandage"), d("bandage"), "minecraft:golden_apple", "minecraft:honey_bottle"], d("medkit"), 1)


# --------------------------------------------------------------------------- Strukturen

LAND = ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:savanna", "minecraft:desert", "minecraft:snowy_plains",
        "minecraft:forest", "minecraft:birch_forest", "minecraft:taiga", "minecraft:meadow"]
MORE = ["minecraft:flower_forest", "minecraft:dark_forest", "minecraft:old_growth_birch_forest", "minecraft:snowy_taiga",
        "minecraft:savanna_plateau", "minecraft:sparse_jungle", "minecraft:badlands", "minecraft:wooded_badlands",
        "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:cherry_grove"]


def structures():
    biome_tags = {
        "has_city": LAND,
        "has_house": LAND + MORE,
        "has_tower": LAND + MORE + ["minecraft:windswept_hills", "minecraft:windswept_forest", "minecraft:jungle"],
        "has_military": ["minecraft:plains", "minecraft:savanna", "minecraft:desert", "minecraft:badlands",
                         "minecraft:snowy_plains", "minecraft:taiga", "minecraft:sunflower_plains"],
        "has_bunker": LAND + MORE + ["minecraft:windswept_hills", "minecraft:windswept_forest", "minecraft:jungle",
                                     "minecraft:swamp", "minecraft:grove", "minecraft:snowy_slopes"],
    }
    biome_tags["has_metropolis"] = ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:savanna", "minecraft:desert",
                                    "minecraft:snowy_plains", "minecraft:meadow", "minecraft:forest", "minecraft:birch_forest", "minecraft:taiga", "minecraft:flower_forest"]
    biome_tags["has_secret_bunker"] = LAND + MORE
    for name, biomes in biome_tags.items():
        write(f"data/{NS}/tags/worldgen/biome/{name}.json", {"replace": False, "values": biomes})

    def spawns(weights):
        return {"monster": {"bounding_box": "full", "spawns": [
            {"type": d(z), "weight": w, "minCount": lo, "maxCount": hi} for z, w, lo, hi in weights]}}

    city_spawns = spawns([("walker", 60, 2, 4), ("runner", 30, 2, 3), ("crawler", 20, 1, 3), ("spitter", 15, 1, 2),
                          ("bloater", 10, 1, 1), ("screamer", 10, 1, 1), ("leaper", 15, 1, 2), ("toxic", 8, 1, 1),
                          ("brute", 5, 1, 1), ("soldier", 6, 1, 2)])
    mil_spawns = spawns([("soldier", 60, 2, 4), ("walker", 20, 2, 3), ("brute", 6, 1, 1), ("leaper", 10, 1, 2)])

    defs = {
        "city": dict(layout="city", biomes="#deadzone:has_city", adapt="beard_thin", step="surface_structures", spawn=city_spawns,
                     spacing=24, separation=10, salt=581630241, exclude=("metropolis", 10)),
        "house": dict(layout="house", biomes="#deadzone:has_house", adapt="beard_thin", step="surface_structures", spawn={},
                      spacing=9, separation=4, salt=730115823, exclude=("city", 5)),
        "tower": dict(layout="tower", biomes="#deadzone:has_tower", adapt="beard_thin", step="surface_structures", spawn={},
                      spacing=18, separation=7, salt=291046637, exclude=("city", 5)),
        "military_base": dict(layout="military", biomes="#deadzone:has_military", adapt="beard_thin", step="surface_structures",
                              spawn=mil_spawns, spacing=28, separation=12, salt=118204417, exclude=("city", 8)),
        "metropolis": dict(layout="metropolis", biomes="#deadzone:has_metropolis", adapt="beard_thin", step="surface_structures",
                           spawn=city_spawns, spacing=36, separation=16, salt=902211357, exclude=None),
        "secret_bunker": dict(layout="secret_bunker", biomes="#deadzone:has_secret_bunker", adapt="none", step="underground_structures",
                              spawn={}, spacing=36, separation=14, salt=667301249, exclude=("metropolis", 8)),
        "bunker": dict(layout="bunker", biomes="#deadzone:has_bunker", adapt="none", step="underground_structures", spawn={},
                       spacing=20, separation=8, salt=455019271, exclude=("city", 5)),
    }
    for name, s in defs.items():
        write(f"data/{NS}/worldgen/structure/{name}.json", {
            "type": d("ruin"), "layout": s["layout"], "biomes": s["biomes"], "step": s["step"],
            "terrain_adaptation": s["adapt"], "spawn_overrides": s["spawn"]})
        placement = {"type": "minecraft:random_spread", "spacing": s["spacing"], "separation": s["separation"], "salt": s["salt"]}
        if s["exclude"]:
            placement["exclusion_zone"] = {"other_set": d(s["exclude"][0]), "chunk_count": s["exclude"][1]}
        write(f"data/{NS}/worldgen/structure_set/{name}.json", {
            "structures": [{"structure": d(name), "weight": 1}], "placement": placement})


if __name__ == "__main__":
    models()
    lang()
    equipment()
    damage()
    loot()
    recipes()
    structures()
    print("ok")
