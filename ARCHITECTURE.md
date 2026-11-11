# Stateworks Architecture

## Current direction

Stateworks is being organized around a game-independent animation core and thin Minecraft/loader integration layers. Resource-pack model JSON remains Minecraft-native: Minecraft resolves model inheritance, geometry, and textures; Stateworks selects model identifiers and controls animation timing.

## First extraction: animation timeline

The package `com.stateworks.core.animation` contains pure Java timing primitives:

- `AnimationTimeline<T>` tracks a current value and an optional timed transition.
- `TimedTransition<T>` stores transition endpoints and calculates normalized progress.

These classes do not import Minecraft, NeoForge, resource managers, block positions, or rendering classes. They can be compiled and tested independently of a Minecraft installation.

`com.stateworks.transition.StateTransitionTracker` is currently the Stateworks-facing adapter. It maps `VirtualState<?>` values to the generic timeline and preserves the API consumed by `StateMachine` and the existing visual/network code.

## Second extraction: platform-neutral state context

The package `com.stateworks.core.context` now defines the platform-neutral `StateContext` contract. The animation/state engine reads only namespaced block IDs, property values, numeric properties, and tag membership through this interface.

`com.stateworks.minecraft.context.MinecraftStateContext` is the Minecraft adapter. It wraps `Level`, `BlockPos`, and `BlockState`, and implements the portable context contract. Minecraft-only built-in registrations explicitly request this adapter when they need world access (for example, repeater input-signal detection).

Optional output creation is represented by `StateOutputContext`, a core capability interface. The Minecraft context implements that capability and delegates target creation to `com.stateworks.minecraft.output.MinecraftStateOutputTargets`. The state machine itself no longer imports Minecraft classes to create output targets.

Portable block/property/tag conditions now compare values exposed by `StateContext`; they no longer import Minecraft block, property, or tag classes.

## Current integration boundary

This is an incremental extraction, not yet a fully loader-independent mod. The following areas still depend on Minecraft or NeoForge and are candidates for later adapter work:

- built-in block registrations and specialized world-dependent conditions
- state-definition loading from Minecraft's resource manager
- machine lifecycle, world ticking, dimensions, and networking
- model discovery/loading and client rendering

## Intended layering

1. **Core** — generic state values, transition timing, interpolation, and animation decisions. No Minecraft or loader imports.
2. **Game adapter** — translates Minecraft block state, world context, resources, and lifecycle events into core inputs.
3. **Loader adapter** — NeoForge/Fabric event registration, networking, and loader-specific services.
4. **Client renderer adapter** — asks Minecraft to load native models and displays the selected/interpolated Stateworks visual.

The first two seams now cover transition timing and platform-neutral state access. Later extractions should move one responsibility at a time and keep the tested furnace/button/repeater/jukebox behavior unchanged.

## Compatibility policy

The explicit `animations` resource-pack format is the sole supported Stateworks format. Legacy visual `states` parsing and implicit numbered-frame discovery are not part of the architecture.

## Third extraction: definition parsing, registry, and resource adapter

State-definition JSON parsing now lives in `com.stateworks.core.definition`. `StateDefinitionParser` accepts JSON text and produces immutable `ParsedStateDefinitions`; it does not open files, inspect Minecraft resource locations, or access a `ResourceManager`.

The registry now lives in `com.stateworks.core.registry.StateRegistry`. It contains no Minecraft or NeoForge imports. Built-in Java registrations and data-driven resource definitions are stored separately and exposed through immutable merged snapshots. A resource reload replaces only the data-driven layer, so built-in furnace, repeater, and other registrations are not accidentally erased. Resource definitions override built-ins only when they intentionally reuse the same ID.

`com.stateworks.minecraft.resource.MinecraftStateDefinitionLoader` is the platform adapter. It discovers resources under the `states/` data path, reads them as UTF-8 text, delegates parsing to the core parser, and publishes the parsed maps to the core registry. NeoForge's `AddServerReloadListenersEvent` registers this adapter for server datapack reloads.

The client visual overlay remains a separate Minecraft rendering adapter. It now accepts only the explicit `animations` visual format; old visual `states` parsing and implicit numbered-frame discovery have been removed. The overlay still passes model identifiers to Minecraft's standalone block-model loader, leaving parent/child model inheritance and texture resolution to Minecraft.

The core packages `com.stateworks.core.animation`, `com.stateworks.core.context`, `com.stateworks.core.definition`, and `com.stateworks.core.registry` contain no direct `net.minecraft` or `net.neoforged` imports. Gson remains the JSON library dependency of the definition parser.


## Phase 4: standalone Java core module

The portable engine and domain model now live in the independent Gradle `:core` Java-library module. The module preserves the existing `com.stateworks.*` package names while moving portable condition, state-definition, transition, signal, output, visual, registry, context, parser, and timeline classes out of the NeoForge source set. Its only external library dependency is Gson, used for JSON definition parsing.

The root NeoForge project depends on `:core` for compilation and includes the core module's compiled output in the distributable mod JAR. This keeps the current installation experience as one JAR while making `:core:build` independently runnable without Minecraft or NeoForge dependencies.

Minecraft-specific built-in block registrations, the world-aware machine manager, block output target, resource loading, networking, and rendering remain in the NeoForge project. The built-in registrations and machine manager were moved into `com.stateworks.minecraft.*` packages to avoid splitting core packages across modules.

The independent module boundary is checked by ensuring `core/src/main/java` contains no `net.minecraft`, `net.neoforged`, or platform adapter imports. The module is configured for Java 25, matching the current Minecraft 26.3 toolchain.


### Phase 4, step 2: NeoForge event and network adapters

- Moved server lifecycle, tick, block-placement, neighbor-notification, and datapack reload event handlers into `com.stateworks.minecraft.event.StateworksServerEvents`.
- Added `MinecraftStateMachineService` as the Minecraft-facing owner/facade for the state-machine manager, so event handlers and inspection code no longer own the manager directly.
- Kept payload registration, client payload handling, and packet transport in `com.stateworks.network.StateworksNetwork`. The Minecraft state-machine manager now delegates packet delivery to that adapter rather than calling NeoForge's `PacketDistributor` itself.
- Preserved the existing server-side tracking rules, repeater neighbor tracking, tick cadence, and per-level player synchronization behavior.


## Phase 4, Step 3 — Client-side isolation

Client-only functionality is isolated from common initialization. The common `Stateworks` entrypoint no longer imports or references `net.minecraft.client` classes or classes in `com.stateworks.client`. The block inspection/debug workflow now lives in `StateworksBlockInspector`, and the keybind calls that client-only adapter directly.

Client visual model registration, visual ticking, keybind handling, and client payload handling remain in the client package and are initialized by the `@Mod(dist = Dist.CLIENT)` entrypoint. `StateworksNetwork` now contains only common payload registration and server-to-client sending; it no longer imports client networking events, the visual state manager, or `Minecraft`.

The `VisualStatePayload` remains a common network data type because both physical sides need its codec. The shared `core` module remains platform-independent.

## Phase 4, step 4 — loader-adapter boundary

The NeoForge implementation now has an explicit package boundary under `com.stateworks.neoforge`:

- `com.stateworks.neoforge.StateworksNeoForge` is the NeoForge mod entrypoint and owns NeoForge registration/configuration.
- `com.stateworks.neoforge.event` contains NeoForge lifecycle, server tick, block event, and reload-event bindings.
- `com.stateworks.neoforge.network` contains NeoForge payload registration and packet delivery.
- `com.stateworks.neoforge.client` contains NeoForge client entrypoint, event/keybind registration, standalone model registration, and NeoForge's dynamic model integration.
- `com.stateworks.neoforge.config` contains NeoForge config API usage.

Minecraft-facing code remains under `com.stateworks.minecraft` and `com.stateworks.client`. The common Minecraft payload type is `com.stateworks.minecraft.network.VisualStatePayload`; it uses Minecraft's payload/codec API but does not import NeoForge. `VisualStateTransport` is the game-facing transport contract. `StateMachineManager` depends on that contract rather than on `PacketDistributor` or the NeoForge network package. The NeoForge entrypoint installs `StateworksNetwork::sendToLevel` as its implementation before server events begin.

The client visual-state manager likewise no longer imports the NeoForge visual overlay. It accepts a duration resolver through `setVisualDurationResolver`; the NeoForge client adapter supplies the overlay's explicit JSON-duration lookup during client initialization.

`com.stateworks.core.StateworksConstants` owns the shared mod identifier so the Minecraft payload type does not depend on the NeoForge entrypoint. The standalone `:core` module remains free of Minecraft and loader imports.

### Future Fabric adapter mapping

A future Fabric integration can reuse `:core`, `com.stateworks.minecraft` game adapters where the Minecraft version/API is shared, and the resource/visual definition formats. It will provide loader-specific implementations for:

1. Mod initialization, registry registration, and configuration.
2. Server lifecycle, ticking, block-change observation, and datapack/resource reload registration.
3. `VisualStateTransport` using Fabric's networking API.
4. Client initialization, keybind/event registration, model loading, and dynamic model integration.

The rendering hook is intentionally still loader-specific: NeoForge's `DynamicBlockStateModel` and standalone model loader are not treated as portable APIs. A Fabric renderer will need its own implementation against Fabric's supported model-loading hooks rather than trying to reuse NeoForge classes.

No Fabric dependency or Fabric source set has been added in this step. The purpose is to make the existing NeoForge boundary explicit and provide a narrow, testable transport seam while retaining NeoForge as the working reference implementation.
