# CyberNpc local build (no GitHub Actions)

Development branch: `development-no-actions`.
The existing GitHub workflow runs on pushes to `main`, pull requests targeting
`main`, or manual dispatch. This development branch does not match those push
triggers. **Do not merge it into main or open a PR targeting main** while
Actions usage is unavailable.

Requirements: JDK 17, Gradle **8.8** installed locally, and internet access
for the first Forge dependency download.

## Windows

```bat
git clone --branch development-no-actions https://github.com/Cyberspectraa/CyberNpc.git
cd CyberNpc
build-local.bat
```

The launcher checks that Java and Gradle are on PATH, runs
`gradle clean build --no-daemon --stacktrace`, then copies the compiled
mod to `CyberNpc.jar` in the repository root **only if the build passes**.
The original versioned file is also in `build/libs/`.

If you downloaded the branch ZIP rather than using Git, extract it and
double-click `build-local.bat` within the extracted project folder.

## Linux / macOS

```bash
gradle clean build --no-daemon --stacktrace
```

Builds here are **local**, not GitHub Actions builds. No compiled JAR can be
promised until one of these commands succeeds. Keep your existing stable pack
JARs in place until a development build is tested.

## Next planned gameplay work

- Remove obsolete Ice and Fire integration/items where Cyber mods actually use them.
- Coordinate CyberRaces character creation, removed flying abilities, preserved
  Goblin/Dwarf wild NPC diversity and a safe approach for existing player saves.
- Add an admin-only Island Rift with per-player discovery progression.
- Allow the main seven named town NPCs to follow eligible friends to the Rift.
- Store world-level departure records safely for a future project, with
  admin recovery and export support.
