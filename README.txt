BUILD
1. Install JDK 21.
2. This folder is a complete Fabric Loom project. The only thing missing is the Gradle wrapper
   (gradlew + gradle/wrapper), which can't be bundled here. Get it one of two ways:
     a) install Gradle 8.14+ and run:  gradle wrapper
     b) copy gradlew, gradlew.bat and the gradle/ folder from a template made at https://fabricmc.net/develop
3. ./gradlew build   -> the jar to use is build/libs/mymod-1.0.0.jar (not -sources)
4. ./gradlew runClient to test in a dev client.
5. If dependencies don't resolve, open https://fabricmc.net/develop, pick 1.21.11 and copy the
   loader / loom / fabric API versions into gradle.properties.

INSTALL
Put the jar in .minecraft/mods together with Fabric API (Fabric Loader required).

KEYS (in game, no screen open)
R TriggerBot | G StunSlam (one-shot) | C AutoCrit | H Lunge | J Reach | F Fly | P AutoPot
[ and ] : TriggerBot hit chance -5% / +5%

CONFIG (config/mymod.properties, created on first launch)
triggerbot.hitChance      0-100, chance to attack per attempt
triggerbot.minDelayTicks  random reaction delay range before attacking (20 ticks = 1s)
triggerbot.maxDelayTicks
reach.range               3.0-6.0, used while Reach is on
autopot.hearts            heal-pot threshold in hearts
friends                   comma-separated player names never auto-attacked (e.g. friends=Steve,Alex)
Restart the game after hand-editing the file.

BUILD WITHOUT INSTALLING JAVA (GitHub)
1. Create a new GitHub repo and upload the CONTENTS of this folder (build.gradle at the repo root, not inside a subfolder).
2. Open the Actions tab -> "build" -> run it (or just push).
3. When it finishes, open the run and download the "mymod-jar" artifact. Unzip it: the .jar inside is the mod.
