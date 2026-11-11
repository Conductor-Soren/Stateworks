## Unreleased

### Visual resource format
- Standardized visual definitions on the explicit `animations` format.
- Removed support for the old visual `states` format and automatic numbered-frame discovery.
- Require every animation frame and property variant to list its Minecraft model identifiers.
- Delegate model parents, geometry, and texture inheritance to Minecraft.

# Changelog

## Alpha 1

Initial development milestone.

### Core
- State-based visual transitions
- Configurable durations in Minecraft ticks
- Variable animation frame counts
- Reusable block-family registration helpers
- Property-stage transitions

### Systems
- Repeater
- Furnace heating/cooling
- Redstone Dust timing
- Crops
- Doors
- Trapdoors
- Buttons
- Saplings
- Lever
- Redstone Torch
- Bamboo multi-property state handling

### Documentation
- Block registration guide
- Animation format guide
- State system overview
- Example pack documentation

## Architecture: platform-neutral StateContext extraction

- Added `com.stateworks.core.context.StateContext`, a platform-neutral read-only interface for block identity, property values, numeric properties, and tag membership.
- Replaced the Minecraft-bound `StateContext` implementation with `MinecraftStateContext` under `com.stateworks.minecraft.context`.
- Updated state definitions, conditions, signal evaluation, transition timing, and the state machine to consume the core context interface.
- Converted block, property, and block-tag conditions to compare portable identifiers and values rather than Minecraft object types.
- Added the optional `StateOutputContext` capability so the core state machine can request output targets without importing Minecraft classes.
- Moved Minecraft output-target creation and its Level/BlockPos factory into `com.stateworks.minecraft.output`.
- Kept world-dependent built-in behavior, including repeater input detection, inside the Minecraft-backed adapter path.
- Pure Java compilation passed for the core context, animation timeline, and portable condition classes. A full NeoForge build remains unverified because the supplied project has no `gradle-wrapper.jar` and this environment has Java 21 rather than the project's required Java 25.

## Architecture: first game-independent core extraction

- Added pure Java `AnimationTimeline<T>` and `TimedTransition<T>` primitives under `com.stateworks.core.animation`.
- Updated `StateTransitionTracker` to adapt Stateworks virtual states to the independent timeline while preserving its existing public API.
- Added `ARCHITECTURE.md` documenting the current boundaries and staged path toward game/loader adapters.
- No block registration, resource-pack schema, model loading, networking, or rendering behavior was intentionally changed.

## Architecture: resource-definition separation

- Moved `StateDefinitionParser` into `com.stateworks.core.definition`; it parses JSON text and returns immutable `ParsedStateDefinitions` without accessing Minecraft resources.
- Moved `StateRegistry` into `com.stateworks.core.registry` and separated built-in registrations from data-driven definitions.
- Added atomic data-driven registry replacement so datapack reloads preserve built-in Java registrations.
- Added `MinecraftStateDefinitionLoader` under `com.stateworks.minecraft.resource` to discover Minecraft resources, read UTF-8 JSON, and delegate parsing to the core parser.
- Registered the Minecraft resource adapter through NeoForge's `AddReloadListenerEvent`.
- Removed the old visual `states` fallback and implicit numbered-frame discovery from `StateworksVisualOverlay`; explicit `animations` with model identifiers is now the only supported visual format.
- Kept Minecraft parent/child model inheritance, texture resolution, and model baking in Minecraft's rendering pipeline.


## Architecture: standalone core module (Phase 4)

- Added a standalone Gradle Java-library subproject at `:core`.
- Moved portable conditions, state definitions, state machines, transitions, signals, output abstractions, visuals, context contracts, registry, parser, and animation timeline into the core module while preserving their Java package names.
- Declared Gson as the core module's only external dependency for portable JSON parsing.
- Made the NeoForge project depend on the core module and include core classes in the final mod JAR, preserving a single-JAR installation.
- Moved built-in Minecraft block registrations, world machine management, and Minecraft block output implementation into `com.stateworks.minecraft.*` adapter packages.
- Added `core/README.md` documenting the standalone module boundary and build command.


## Architecture: NeoForge event and network adapter separation (Phase 4, step 2)

- Extracted NeoForge server lifecycle, tick, datapack reload, block-placement, and neighbor-notification handlers into `StateworksServerEvents`.
- Added `MinecraftStateMachineService` to own the Minecraft-backed manager and provide the update, tick, clear, and count operations used by adapters.
- Centralized visual-state packet delivery in `StateworksNetwork`; `StateMachineManager` now delegates transport instead of directly using `PacketDistributor`.
- Preserved existing tracking behavior, including repeater input-side tracking and client inspection access to the machine manager.


## Phase 4 — Step 3: Client-side isolation

- Removed client-only Minecraft imports and inspection logic from the common `Stateworks` entrypoint.
- Added the client-only `StateworksBlockInspector` adapter and routed the inspection keybind through it.
- Moved visual-state client payload handling into the client-only `StateworksClient` entrypoint.
- Reduced `StateworksNetwork` to common payload registration and server-to-client sending.
- Removed unused client-package wildcard imports from common classes.

## Phase 4 — Step 4: Loader adapter preparation

- Moved NeoForge entrypoints, configuration, server events, networking transport, keybind registration, and model integration into the explicit `com.stateworks.neoforge` package namespace.
- Moved the Minecraft custom payload type into `com.stateworks.minecraft.network` and centralized the mod identifier in the portable core constants class.
- Added the Minecraft-facing `VisualStateTransport` interface. The state machine now depends on this interface instead of NeoForge's packet distributor; the NeoForge entrypoint installs its transport implementation.
- Removed the NeoForge visual-overlay dependency from the shared client visual-state manager by injecting a visual-duration resolver from the client loader adapter.
- Added `LOADER_ADAPTERS.md` documenting the layer boundaries and future Fabric integration points. No Fabric implementation or dependency was added.
