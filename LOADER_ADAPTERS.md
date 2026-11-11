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

## Adding another loader later

A Fabric implementation should be added as a separate adapter source set/module with its own entrypoint, event bindings, config, networking, and client model integration. It should install its `VisualStateTransport` implementation through `MinecraftStateMachineService.installTransport(...)` and supply a client visual-duration resolver through `StateworksVisualStateManager.setVisualDurationResolver(...)`.

The Fabric adapter should not copy or fork the core state machine, transition definitions, parser, or resource-pack formats. Where Minecraft's API differs between target versions/loaders, implement a loader-specific adapter rather than leaking those APIs into `core`.

This guide describes the intended integration contract; it does not claim that a Fabric build or renderer exists yet.
