# Stateworks

Stateworks is a Minecraft mod framework for smooth visual transitions between block states.

**Status: Alpha 1** — an early testing release. The core systems are being actively tested and APIs may change.

## Alpha 1 systems

1. Repeater
2. Furnace
3. Redstone Dust
4. Crops
5. Doors
6. Trapdoors
7. Buttons
8. Saplings
9. Lever
10. Redstone Torch

These are transition systems; block variants within a family do not each count as a separate system.

## Highlights

- Boolean and property-stage transitions
- Minecraft-tick-based transition timing
- Variable animation frame counts
- Reusable multi-block registration helpers
- Special handling for timing-sensitive systems
- Context-aware visual states for blocks with additional properties

## Documentation

- [Adding Blocks](docs/ADDING_BLOCKS.md)
- [Animation Format](docs/ANIMATION_FORMAT.md)
- [State System](docs/STATE_SYSTEM.md)
- [Example Pack](example-pack/README.md)

## Building

```powershell
.\gradlew clean build
```

Development client:

```powershell
.\gradlew runclient
```

## Alpha 1 goal

The first Alpha is focused on validating the transition engine: state detection, timing, frame playback, block-family registration, and preservation of relevant vanilla properties.

See `CHANGELOG.md` for the Alpha 1 scope.
