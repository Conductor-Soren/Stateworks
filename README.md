# Stateworks

Stateworks is a Minecraft mod framework for smooth visual transitions between block states. It animates transitions by selecting explicitly defined Minecraft model frames while leaving model geometry, texture inheritance, and model baking to Minecraft.

**Current release: 0.1.5**

## Features

- Visual transitions driven by Minecraft block-state changes
- Minecraft-tick-based transition timing
- Variable animation frame counts and per-frame durations
- Explicit per-block visual definitions using the `animations` format
- Reusable state and block-family registration helpers
- A shared, platform-neutral core with separate loader adapters
- Minecraft-native parent/child model inheritance

## Verified animated blocks

The 0.1.5 release has been tested in fresh Minecraft 26.3 installations with both supported loaders. Runtime checks covered:

- Furnace, blast furnace, and smoker
- Buttons (press and release)
- Repeaters (all four delay settings)
- Jukebox (insert and eject)

## Supported versions

| Component | Version |
|---|---|
| Stateworks | 0.1.5 |
| Minecraft | 26.3 |
| Java | 25 |
| NeoForge | 26.3.0.101 |
| Fabric Loader | 0.20.1 or newer |
| Fabric API | 0.166.2+26.3 |

Install the JAR matching your mod loader. Do not install both loader builds in the same instance.

## Project layout

- `core/` — platform-neutral state, transition, timing, and definition logic
- `src/` — root NeoForge project and Minecraft/NeoForge integration
- `fabric/` — Fabric loader adapter

NeoForge is built from the repository root. Fabric is a separate Gradle subproject. Both package the shared core into their installable mod JAR.

## Building

Use Java 25.

Build the NeoForge artifact:

```powershell
.\gradlew clean build --no-configuration-cache
```

Build the Fabric artifact:

```powershell
.\gradlew :fabric:clean :fabric:build --no-configuration-cache
```

Artifacts are written to:

- NeoForge: `build/libs/`
- Fabric: `fabric/build/libs/`

## Visual definition format

Stateworks visual definitions use the explicit `animations` format. Each animation lists the Minecraft model identifiers for its frames. Frame durations may be specified when the resource pack should control timing; when omitted, Stateworks' transition timing remains authoritative.

Example:

```json
{
  "block": "minecraft:furnace",
  "animations": {
    "heating": {
      "frames": [
        "stateworks_example:block/furnaces/furnace/heating_1",
        "stateworks_example:block/furnaces/furnace/heating_2"
      ],
      "durations": [1, 1]
    }
  }
}
```

Model parents, geometry, textures, texture variables, and baking remain Minecraft's responsibility. Stateworks selects the frame model during the transition.

See `ARCHITECTURE.md`, `LOADER_ADAPTERS.md`, `core/README.md`, and `fabric/README.md` for implementation and build details. See `CHANGELOG.md` for release history.
