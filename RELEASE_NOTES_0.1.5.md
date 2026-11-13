# Stateworks 0.1.5

Stateworks 0.1.5 provides matching NeoForge and Fabric builds for Minecraft 26.3.

## Highlights

- Shared platform-neutral core packaged in both loader builds.
- Separate NeoForge and Fabric adapters.
- Updated Stateworks square mod icon and synchronized metadata.
- Fabric API dependency updated to `0.166.2+26.3`.
- Removed the development-only transition test class from release artifacts.

## Tested

Both loader builds were installed and tested in fresh Minecraft 26.3 instances.

Runtime checks covered furnace, blast furnace, smoker, button press/release, all four repeater delay settings, and jukebox insertion/ejection.

## Downloads

Choose the JAR matching your loader:

- `stateworks-0.1.5.jar` — NeoForge 26.3.0.101
- `stateworks-fabric-0.1.5.jar` — Fabric Loader 0.20.1+, Fabric API 0.166.2+26.3

Java 25 is required. Do not install both loader builds in the same instance.
