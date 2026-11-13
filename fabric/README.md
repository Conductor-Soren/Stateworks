# Stateworks Fabric adapter

The Fabric adapter provides a Fabric build of Stateworks for Minecraft 26.3. It reuses the standalone `core` module and shared Minecraft-facing code while keeping Fabric entrypoints, lifecycle integration, networking, mixins, and model rendering in this subproject.

## Target versions

- Minecraft: 26.3
- Java: 25
- Fabric Loader: 0.20.1 or newer
- Fabric API: 0.166.2+26.3
- Fabric Loom: version configured by the project Gradle build

## Implemented

- Fabric common and client entrypoints.
- Shared Stateworks definitions and state-machine service initialization.
- Fabric server tick and lifecycle integration.
- Queued server-side block-state update processing.
- Fabric custom-payload registration, server delivery, and client receipt.
- Client inspection key mapping and visual-state cache updates.
- Shared core classes bundled into the Fabric mod JAR.
- Model-reload integration that discovers explicit Stateworks visual definitions.
- Explicit `animations` frame and property-variant parsing.
- Registration and baking of referenced Minecraft block models through Fabric extra-model keys.
- Animated block-state model wrapper with directional rotations and button attachment orientations.
- Shared visual-duration resolution, preserving Java transition timing when JSON frame durations are omitted.

## Runtime verification

The Fabric 0.1.5 build was tested in a fresh Minecraft 26.3 instance.

Runtime checks covered:

- Furnace, blast furnace, and smoker
- Button press and release
- Repeater animations at all four delay settings
- Jukebox insertion and ejection

The Fabric API dependency was updated to `0.166.2+26.3` and the Fabric build was confirmed working.

## Not included

The NeoForge config screen and NeoForge-specific config UI have not been ported to Fabric.

## Build

From the repository root, using Java 25:

```powershell
.\gradlew :fabric:clean :fabric:build --no-configuration-cache
```

The NeoForge root project is built separately:

```powershell
.\gradlew clean build --no-configuration-cache
```

The Fabric artifact is written to `fabric/build/libs/`.

## Model and animation behavior

Stateworks visual definitions use the explicit `animations` format. Each animation lists its Minecraft model frame identifiers. Minecraft remains responsible for model parents, geometry, texture inheritance, and baking; Stateworks selects the active frame during a transition.

When a visual definition omits an explicit `durations` array, the shared transition timing remains authoritative. Explicitly authored frame durations can provide resource-pack timing.
