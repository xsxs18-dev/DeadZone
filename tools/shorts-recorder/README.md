# Shorts recorder (not part of the mod)

Dev-only tooling that recorded the DeadZone YouTube Short directly from the game:
1080×1920 at 60 FPS, frame by frame. The game's tick rate is slowed down so every rendered frame
is exactly 1/60 s of game time, which keeps the video perfectly smooth on any GPU. Every sound the
game plays is logged and mixed into a matching audio track afterwards.

## Usage

1. Copy `ShortsDirector.java` and `Script.java` to `src/client/java/com/kaan/deadzone/client/shorts/`
   and the three mixins to `src/client/java/com/kaan/deadzone/mixin/client/shorts/`.
2. Register the mixins in `deadzone.client.mixins.json` (`shorts.WindowMixin`, `shorts.CameraMixin`,
   `shorts.MinecraftClientMixin`) and call `ShortsDirector.register();` in `DeadZoneClient`.
3. Add a run config to `build.gradle`:
   ```groovy
   loom {
       runs {
           shorts {
               inherit client
               vmArg "-Ddeadzone.shorts=<world name>"
               vmArg "-Ddeadzone.shorts.cityX=<metropolis center x>"
               vmArg "-Ddeadzone.shorts.cityZ=<metropolis center z>"
               vmArg "-Ddeadzone.shorts.bunkerX=<secret bunker x>"
               vmArg "-Ddeadzone.shorts.bunkerZ=<secret bunker z>"
           }
       }
   }
   ```
4. `./gradlew runShorts` → `run/shorts/raw.mp4`, `sounds.jsonl`, `shots.json`
5. `python tools/shorts_post.py run/shorts` (needs numpy and ffmpeg; `SHORTS_LANG=en` for English captions)
