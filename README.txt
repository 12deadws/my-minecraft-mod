BUILD
1. Install JDK 21.
2. This folder is a complete Fabric Loom project. The only thing missing is the Gradle wrapper
   (gradlew + gradle/wrapper), which can't be bundled here. Get it one of two ways:
     a) install Gradle 8.14+ and run:  gradle wrapper
     b) copy gradlew, gradlew.bat and the gradle/ folder from a template made at https://fabricmc.net/develop
3. ./gradlew build   -> the jar to use is build/libs/caelestial-1.0.0.jar (not -sources)
4. ./gradlew runClient to test in a dev client.
5. If dependencies don't resolve, open https://fabricmc.net/develop, pick 1.21.11 and copy the
   loader / loom / fabric API versions into gradle.properties.

INSTALL
Put the jar in .minecraft/mods together with Fabric API (Fabric Loader required).

KEYS
Right Shift opens/closes the menu. Every module starts with Keybind: NONE; use the menu toggles,
or open a module's dropdown, click "Keybind", press a key (Esc clears, Right Shift cancels).
Binding a key that another module uses unbinds the other one. Binds are saved to the config file.

CONFIG (config/caelestial.properties, created on first launch)
triggerbot.hitChance      0-100, chance to attack per attempt
triggerbot.minDelayTicks  random reaction delay range before attacking (20 ticks = 1s)
triggerbot.maxDelayTicks
reach.range               3.0-6.0, used while Reach is on
autopot.hearts            heal-pot threshold in hearts
esp.chests / esp.shulkers / esp.spawners   true/false
esp.range                 16-128 blocks
key.<Module>              saved keybinds (-1 = none)
friends                   comma-separated player names never auto-attacked (e.g. friends=Steve,Alex)
Restart the game after hand-editing the file.

BUILD WITHOUT INSTALLING JAVA (GitHub)
1. Create a new GitHub repo and upload the CONTENTS of this folder (build.gradle at the repo root, not inside a subfolder).
2. Open the Actions tab -> "build" -> run it (or just push).
3. When it finishes, open the run and download the "caelestial-jar" artifact. Unzip it: the .jar inside is the mod.

MENU
Right Shift opens the Caelestial Client menu (Right Shift or Esc closes it).
Click a module to open its dropdown, click ON/OFF to toggle it (RUN fires macro modules like StunSlam).
Settings in the dropdowns are saved to config/caelestial.properties straight away.
