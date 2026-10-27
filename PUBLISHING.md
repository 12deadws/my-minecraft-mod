# Getting this onto Modrinth

## 1. Make it a proper release build
- Set mod_version and archives_base_name in gradle.properties (already included in this project).
- processResources in build.gradle fills "${version}" in fabric.mod.json.
- `./gradlew build` -> upload the file in build/libs/ that does NOT end in -sources or -dev
  (e.g. mymod-1.0.0.jar).
- Test it on a clean install (Fabric + Fabric API + only this jar) before uploading.

## 2. Change the placeholders
- Mod id "mymod" (fabric.mod.json, assets/mymod/, MyClient package) -> your unique id, lowercase.
  Search for "mymod" and "YourName" across the project.
- Replace icon.png (512x512) if you want your own.
- Pick the license you actually want; MIT is a placeholder. Match it in fabric.mod.json and LICENSE.

## 3. Modrinth project page (modrinth.com -> Create a project)
- Name / slug / summary: copy from fabric.mod.json "description"
- Description: paste modrinth/DESCRIPTION.md
- Categories: Utility (pick whatever fits)
- Environment: Client = Required, Server = Unsupported
- Links: source + issues (a public GitHub repo helps review)
- Version: 1.0.0, release channel, game version 1.21.11, loader Fabric,
  dependency: Fabric API (required), changelog from modrinth/CHANGELOG.md

## 4. Review
Modrinth moderators review new projects against their content rules before they go public, and cheat-style mods are an area where they can say no.
Read the content rules first (modrinth.com/legal/rules), keep the description honest about what the mod does, and
don't add anything that hides the mod from servers. Source code available publicly makes review easier.
Stay ready for a rejection; if it happens, CurseForge, GitHub Releases or just sharing the jar directly are the fallbacks.
