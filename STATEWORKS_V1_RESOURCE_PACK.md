# Stateworks resource-pack visual definitions

Stateworks resource packs provide model frames for transitions that the mod already knows how to play. They do not define block-state logic, geometry, or texture inheritance.

## Required visual-definition format

Visual definitions live under a resource namespace at:

```text
assets/<namespace>/stateworks/visuals/<name>.json
```

Every visual definition must use the `animations` object. Each animation explicitly lists the Minecraft model identifiers that make up its frames. The older visual `states` format is not supported.

```json
{
  "block": "minecraft:furnace",
  "animations": {
    "heating": {
      "frames": [
        "example:block/furnace/heating_1",
        "example:block/furnace/heating_2",
        "example:block/furnace/heating_3",
        "example:block/furnace/heating_4"
      ],
      "durations": [1, 1, 1, 1]
    },
    "cooling": {
      "frames": [
        "example:block/furnace/cooling_1",
        "example:block/furnace/cooling_2",
        "example:block/furnace/cooling_3",
        "example:block/furnace/cooling_4"
      ],
      "durations": [1, 1, 1, 1]
    }
  }
}
```

Each frame identifier points to a normal Minecraft model JSON file. For example, `example:block/furnace/heating_1` resolves to:

```text
assets/example/models/block/furnace/heating_1.json
```

The model can use Minecraft's native `parent` and `textures` fields. Stateworks does not parse, copy, or override those texture definitions; Minecraft's model loader resolves them.

## Frame timing

`durations` is optional. When omitted, Stateworks uses the registered logical transition duration and advances through the frames uniformly. When supplied, it contains one duration per frame, measured in Minecraft ticks. Missing entries default to one tick; extra entries are ignored.

## Property variants

Use `variants` when an animation needs different frame sequences for different vanilla block properties. Each variant must explicitly list its own frame model identifiers:

```json
{
  "block": "minecraft:oak_door",
  "animations": {
    "opening": {
      "variants": [
        {
          "when": {
            "hinge": "left",
            "half": "lower"
          },
          "frames": [
            "example:block/oak_door/opening_left_1",
            "example:block/oak_door/opening_left_2",
            "example:block/oak_door/opening_left_3",
            "example:block/oak_door/opening_left_4"
          ]
        },
        {
          "when": {
            "hinge": "right",
            "half": "lower"
          },
          "frames": [
            "example:block/oak_door/opening_right_1",
            "example:block/oak_door/opening_right_2",
            "example:block/oak_door/opening_right_3",
            "example:block/oak_door/opening_right_4"
          ]
        }
      ]
    }
  }
}
```

Variant conditions are matched against the block's current vanilla `BlockState`. A variant with more matching conditions takes precedence over a less-specific variant. Each animation must define exactly one of `frames` or `variants`.

## Model ownership

Stateworks only selects and animates between model identifiers. Minecraft owns model loading, parent/child inheritance, geometry, texture variables, and texture resolution. Normal Minecraft blockstate JSON files remain unchanged.
