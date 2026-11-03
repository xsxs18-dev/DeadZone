<img src="docs/images/banner.png" alt="DeadZone" width="100%">

# DeadZone

Zombie apocalypse mod for Minecraft 1.21.11 (Fabric): ruined cities, 10 zombie types, 20 guns, infection, secret bunkers and a boss. Download it from [Releases](https://github.com/xsxs18-dev/DeadZone/releases/latest).

<p align="center">
  <img src="docs/images/flythrough.gif" width="230" alt="Flying through a ruined mega city">
  <img src="docs/images/shot_horde.png" width="230" alt="Blood moon horde">
  <img src="docs/images/shot_fight.png" width="230" alt="Minigun first person">
  <img src="docs/images/shot_boss.png" width="230" alt="Patient Zero">
</p>

## Features

### Zombies

Zombies only spawn at night. During the day they take double damage, are slower and smoke, but they don't burn. Every type has its own texture and glowing eyes.

<p align="center"><img src="docs/images/zombies.png" width="90%" alt="All zombie types"></p>

| Zombie | Threat | Special ability |
|---|---|---|
| Walker | Low | Comes in large groups, breaks down doors |
| Runner | Low | Very fast, spots you from 48 blocks away |
| Crawler | Low | Half size, hard to hit, bites slow you down |
| Bloater | Medium | Explodes next to you and leaves a poison cloud |
| Spitter | Medium | Spits poisonous acid from up to 16 blocks |
| Brute | High (80 HP) | Huge, ground slam launches everything into the air |
| Screamer | Low | Screams to buff nearby zombies, blinds you and calls reinforcements |
| Soldier | High | Helmet and vest, often drops ammo |
| Leaper | Medium | Pounces from up to 12 blocks, no fall damage |
| Toxic | Medium | Radioactive aura that poisons everything nearby |
| Patient Zero (boss) | Extreme (300 HP) | Boss bar, ground slam, leap attack, summons runners, enrages below 50 % |

### Infection

- Every zombie hit gives you Weakness for 10 seconds.
- From the 18th hit on, every hit has a 12 % chance to infect you.
- After 20 minutes an infected player dies and turns into a zombie wearing their head. Milk doesn't help.
- The Green Antidote cures it. It has an 8 % chance to be in any loot chest.
- Patient Zero drops the Immune Serum, which makes you immune for good.

### Weapons and gear

20 guns. They're hitscan (no projectile entities), so even the minigun doesn't cost much performance. Right-click shoots, sneak + right-click reloads. Sneaking halves the spread, headshots do ×1.75 damage and bullets break glass.

<details>
<summary>Weapon stats</summary>

| Weapon | Ammo | Damage | Shots/s | Magazine | Notes |
|---|---|---|---|---|---|
| M9 Pistol | 9mm | 5 | 4 | 15 | |
| .357 Revolver | 9mm | 9 | 1.4 | 6 | |
| Desert Eagle | 9mm | 11 | 1.7 | 7 | |
| Uzi | 9mm | 3.5 | 10 | 25 | Automatic |
| MP5 | 9mm | 4 | 10 | 30 | Automatic |
| P90 | 9mm | 3.5 | 20 | 50 | Automatic |
| AK-47 | 5.56 | 7 | 6.7 | 30 | Automatic |
| M4A1 | 5.56 | 6 | 10 | 30 | Automatic |
| SCAR-H | 5.56 | 8 | 6.7 | 20 | Automatic |
| Marksman Rifle (DMR) | 5.56 | 12 | 2.5 | 10 | Pierces 2 |
| Pump Shotgun | Shells | 8 × 3.5 | 1.1 | 7 | |
| Double-Barrel Shotgun | Shells | 10 × 4 | 2.5 | 2 | |
| AA-12 Auto Shotgun | Shells | 7 × 2.5 | 4 | 20 | Automatic |
| Hunting Rifle | 5.56 | 16 | 0.8 | 5 | Pierces 2 |
| Sniper Rifle | .50 BMG | 28 | 0.6 | 5 | Pierces 3 |
| Barrett .50 | .50 BMG | 40 | 0.8 | 10 | Pierces 5 |
| M249 LMG | 5.56 | 6 | 10 | 100 | Automatic |
| Minigun | 5.56 | 5 | 20 | 200 | Automatic |
| Grenade Launcher | Explosive | - | 1 | 6 | Explosion, no block damage |
| RPG-7 | Explosive | - | 1 | 1 | Explosion, destroys blocks |

</details>

<p align="center"><img src="docs/images/items.png" width="70%" alt="Weapons and items"></p>

- Knives: Kitchen Knife, Combat Knife, Machete, Karambit
- Throwables: Frag Grenade, Molotov Cocktail
- Medical: Bandage, Medkit, Green Antidote, Immune Serum
- Gear: Combat Helmet, Body Armor Vest, Combat Pants, Combat Boots, Gas Mask (blocks poison, gas and radiation), Night Vision Goggles
- Ammo: 9mm, 5.56mm, shotgun shells, .50 BMG and explosive rounds, all craftable

### Structures

| Structure | Find it with | What's inside |
|---|---|---|
| Mega City | `/locate structure deadzone:metropolis` | About 210 × 210 blocks. Skyscrapers up to 90 blocks tall, intact, half torn off, collapsed or leaning over the street. Toppled towers, a collapsed highway, a bomb crater with a helicopter wreck, hospital, police station, gas station, supermarket, parking garage, a park with a survivor camp and a military checkpoint |
| Ruined Town | `/locate structure deadzone:city` | Streets with car wrecks, collapsed towers, apartments, shops |
| Secret Bunker | `/locate structure deadzone:secret_bunker` | Looks like an old shack. A hatch under the moss carpet leads 35 blocks down into a research facility with a command center, a lab with specimens in glass tubes, a weapons vault and Patient Zero |
| Military Base | `/locate structure deadzone:military_base` | Walls, watchtowers, barracks, armory, helipad, tents, a tank and a truck |
| Bunker | `/locate structure deadzone:bunker` | Underground storage, dorm, lab and armory |
| Abandoned House | `/locate structure deadzone:house` | Two-story houses with loot |
| Tower | `/locate structure deadzone:tower` | Watchtowers and radio towers |

<p align="center">
  <img src="docs/images/metropolis_top.png" width="49%" alt="Mega city from above">
  <img src="docs/images/metropolis_street.png" width="49%" alt="Mega city street level">
</p>
<p align="center">
  <img src="docs/images/bunker_command.png" width="49%" alt="Secret bunker command center">
  <img src="docs/images/title_screen.png" width="49%" alt="Custom title screen">
</p>

### Blood Moon

Every 7th night is a Blood Moon. You get a warning, then every 30 seconds a horde walks towards each survival player, at most 35 zombies per player.

### Title screen

DEADZONE logo, a blood moon panorama over a ruined skyline, red buttons and new splash texts.

### Performance

Made to run on low-end PCs: vanilla models, hitscan guns, abilities on cooldowns instead of per-tick checks, and structures generated in code instead of big NBT files. The jar is about 0.5 MB.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) 0.19+ for Minecraft 1.21.11.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api).
3. Download the latest `deadzone-x.y.z.jar` from [Releases](https://github.com/xsxs18-dev/DeadZone/releases/latest).
4. Put both jars into your `.minecraft/mods` folder.

DeadZone is needed on the client and the server. Structures only generate in new chunks, so start a new world or travel far.

## Building from source

Needs JDK 21+.

```bash
git clone https://github.com/xsxs18-dev/DeadZone.git
cd DeadZone
./gradlew build          # jar ends up in build/libs/
./gradlew runClient      # start a dev client
```

Textures, the title screen and the data files are generated by scripts:

```bash
python3 tools/gen_zombies.py   # zombie skins + glowing eyes
python3 tools/gen_items.py     # item textures, effect icon
python3 tools/gen_armor.py     # armor worn on the body
python3 tools/gen_title.py     # logo, panorama, buttons, splashes
python3 tools/gen_data.py      # models, languages, loot tables, recipes, structures
```

<details>
<summary>Project layout</summary>

```
src/main/java/com/kaan/deadzone/
├── entity/        zombies, boss, grenades & molotovs
├── item/          guns, healing items, gear effects
├── infection/     hit counter, infection timer, cure
├── event/         Blood Moon hordes
├── world/         procedural structures (cities, bunkers, bases)
└── registry/      items, entities, effects, components
src/client/java/   renderers, title screen, aiming pose
tools/             texture & data generators, shorts recorder
```

</details>

## License

[MIT](LICENSE). You can use it in modpacks.

Not affiliated with Mojang or Microsoft.
