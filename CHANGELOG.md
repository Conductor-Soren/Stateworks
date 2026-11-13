# Changelog

## 0.1.5 — November 2026

### Release
- Published matching NeoForge and Fabric builds for Minecraft 26.3.
- Updated the mod icon to the Stateworks square logo in both loader builds.
- Synchronized the mod version and description across both loaders.
- Confirmed the shared core is packaged in both JARs and that loader-specific implementation classes remain separated.
- Removed the development-only `TransitionTest` class from release artifacts.

### Fabric
- Updated the Fabric API development dependency to `0.166.2+26.3`.
- Verified the Fabric build and tested it in a fresh Minecraft 26.3 instance.

### Verification
- Inspected both release JARs for metadata, icon, shared classes, and loader-specific contents.
- Tested both NeoForge and Fabric builds in fresh Minecraft 26.3 installations.
- Runtime checks covered furnace, blast furnace, smoker, button press/release, all four repeater delay settings, and jukebox insertion/ejection.

## Earlier development notes

### Visual resource format
- Standardized visual definitions on the explicit `animations` format.
- Removed support for the old visual `states` format and automatic numbered-frame discovery.
- Require every animation frame and property variant to list its Minecraft model identifiers.
- Delegate model parents, geometry, and texture inheritance to Minecraft.

### Architecture: platform-neutral core
- Extracted portable animation timelines, state context, state definitions, conditions, state machines, transitions, signals, output abstractions, visuals, registry, and parser into the standalone `core` module.
- Kept Minecraft-specific world access, resource loading, output creation, networking, and rendering in Minecraft and loader adapters.
- NeoForge remains the root Gradle project; Fabric is a separate `:fabric` subproject.
- Both loader builds include the shared core in their installable JARs.

### NeoForge and Fabric adapters
- Separated loader entrypoints, lifecycle/event handling, networking transport, client initialization, and model integration from the shared core.
- Added Fabric server lifecycle and tick integration, payload transport, client state receipt, inspection key mapping, model loading, and animated block-state model rendering.
- Preserved the explicit visual JSON format across both loaders.
- Fixed Fabric duration resolution so generated default frame durations do not override Java-defined transition timing when the JSON omits an explicit `durations` array.
