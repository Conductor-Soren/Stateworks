# Stateworks 1.0 — Virtual Visual States

Stateworks exposes a small set of built-in virtual transition states to resource packs.

## Repeater

The first built-in transition is:

`off -> Powering -> active`

and:

`active -> PoweringOff -> off`

Transitions are always 4 Minecraft ticks (200 ms).

Resource packs do not define states or timing. They only provide visuals for states Stateworks already knows.

Example:

```json
{
  "block": "minecraft:repeater",
  "states": {
    "Powering": {
      "1": "minecraft:block/repeater/powering_1",
      "2": "minecraft:block/repeater/powering_2",
      "3": "minecraft:block/repeater/powering_3",
      "4": "minecraft:block/repeater/powering_4"
    }
  }
}
```

The normal `assets/minecraft/blockstates/repeater.json` remains unchanged.

Stateworks is a client-side visual enhancement. Without Stateworks, the resource-pack overlay is ignored and the normal blockstate rendering is used.
