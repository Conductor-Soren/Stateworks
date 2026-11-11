# Stateworks Core

This is the platform-independent Stateworks engine and definition model. It is built as a standalone Java library and does not depend on Minecraft, NeoForge, Fabric, or any game loader.

The core currently depends only on Gson for JSON definition parsing. Platform projects provide `StateContext` implementations, resource loading, output targets, ticking, networking, and rendering.

Build this module independently with `gradle :core:build` (or `./gradlew :core:build` from the repository root).
