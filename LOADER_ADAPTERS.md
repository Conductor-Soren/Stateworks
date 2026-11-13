# Stateworks Loader Adapter Guide

Stateworks is structured in three practical layers:

| Layer | Location | Responsibility |
|---|---|---|
| Portable engine | `core/src/main/java/com/stateworks` | State definitions, conditions, transitions, registry, parsing, timing, and output contracts |
| Minecraft game integration | `src/main/java/com/stateworks/minecraft` and `com/stateworks/client` | Minecraft block/world/resource adapters and client visual-state data |
| Loader integration | `src/main/java/com/stateworks/neoforge` | NeoForge entrypoints, event hooks, networking, configuration, and model integration |

## Loader boundary rules

- Do not add `net.neoforged.*` imports to `core` or `com.stateworks.minecraft`.
- Keep `net.neoforged.*` imports inside `com.stateworks.neoforge`.
- Keep `net.minecraft.*` imports out of `core`.
- Loader event handlers should translate events into calls to Minecraft services; they should not implement transition logic.
- Packet delivery must go through `VisualStateTransport`, not directly from the state machine.
- Keep resource-pack definitions in the existing explicit JSON formats. Loader adapters should pass resource text to the existing core parsers rather than introduce a second format.
- Client model integration is loader-specific. Do not move NeoForge model interfaces into shared Minecraft or core packages.

## Current NeoForge implementation points

| Capability | NeoForge implementation |
|---|---|
| Common mod entrypoint | `com.stateworks.neoforge.StateworksNeoForge` |
| Server events and reload registration | `com.stateworks.neoforge.event.StateworksNeoForgeServerEvents` |
| Network payload registration and send | `com.stateworks.neoforge.network.StateworksNetwork` |
| Shared Minecraft payload type | `com.stateworks.minecraft.network.VisualStatePayload` |
| Transport contract | `com.stateworks.minecraft.network.VisualStateTransport` |
| Client entrypoint and event wiring | `com.stateworks.neoforge.client.StateworksNeoForgeClient` |
| Client keybind registration | `com.stateworks.neoforge.client.StateworksNeoForgeKeybinds` |
| NeoForge model integration | `com.stateworks.neoforge.client.StateworksVisualOverlay` and `model/VirtualStateBlockModel` |
| NeoForge config | `com.stateworks.neoforge.config.NeoForgeConfig` |

## Fabric implementation

Fabric is implemented as a separate `fabric` Gradle subproject. It supplies its own entrypoints, event bindings, networking, client key mapping, and client model integration. It installs its `VisualStateTransport` implementation through `MinecraftStateMachineService.installTransport(...)` and supplies its visual-duration resolver through `StateworksVisualStateManager.setVisualDurationResolver(...)`.

The Fabric client model adapter uses `PreparableModelLoadingPlugin` to discover visual JSON resources during model reload, registers each referenced Minecraft model as an extra model, and wraps each relevant baked block-state model. The wrapper selects the matching pre-baked frame from the shared client visual-state cache and delegates geometry emission to Fabric's `FabricBlockStateModel` API. Minecraft continues to own model parent resolution, geometry, and texture baking.

The Fabric adapter does not fork the core state machine, transition definitions, or resource-pack formats. Loader-specific model APIs remain inside `fabric/src/client`.


## Phase 5 — Fabric adapter (Step 1)

The first Fabric adapter module targets Minecraft 26.3 and reuses the standalone `core` module plus the shared Minecraft context, state-machine, resource-definition, and client-state classes. The Fabric module supplies its own entrypoints, lifecycle bridge, payload registration/delivery, client key mapping, and a queued block-state-write hook.

The Fabric visual model integration is intentionally not included in Step 1. NeoForge's standalone-model and dynamic block-model APIs cannot be reused directly; Fabric model loading will be implemented as a separate client adapter in the next step.


## Phase 5 — Fabric visual model adapter (Step 2)

- Added a preparable Fabric model-loading plugin that scans the same `stateworks/visuals/` resource path and reads the explicit `animations` JSON format.
- Registered every referenced frame model as a Fabric extra model, retaining Minecraft's normal parent/child model inheritance and texture resolution.
- Added a Fabric unbaked-root wrapper and baked block-state model wrapper. During geometry emission, the wrapper selects the active transition, the most-specific matching property variant, the block orientation, and the current timed frame.
- Preserved the NeoForge rotation conventions for furnaces, other directional blocks, and buttons.
- Connected the Fabric visual duration resolver to the shared client visual-state manager.
- The implementation has not yet been built or exercised in-game by the user; verify the Fabric build and resource-pack animations before accepting this step.
