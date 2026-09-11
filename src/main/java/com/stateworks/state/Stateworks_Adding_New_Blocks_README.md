# Adding New Blocks to Stateworks

This guide explains how to add new block transition systems to Stateworks.

Stateworks separates block registrations into two categories:

- **Generic systems** — use the automatic helpers for ordinary block properties.
- **Special systems** — use custom code when a block has unique behavior that cannot be represented by the generic helpers.

The main file for built-in registrations is:

`src/main/java/com/stateworks/state/BuiltinStateSets.java`

---

## 1. Find `register()`

Open `BuiltinStateSets.java` and find:

```java
public static void register() {
    // registrations
}
```

This is where block transition systems are registered.

For most new blocks, you only need to add a registration call here.

---

# Generic Systems

## 2. Blocks with an Integer Property

Use `registerPropertyStages(...)` when a block has an integer property whose values represent stages or levels.

The helper automatically creates a Stateworks visual state for every value and creates transitions between adjacent values in both directions.

### Format

```java
registerPropertyStages(
    "example:block_name",
    Blocks.BLOCK_NAME,
    BlockProperty.PROPERTY,
    MIN_VALUE,
    MAX_VALUE,
    DURATION_IN_TICKS
);
```

### Example: Sapling

Saplings use Minecraft's `STAGE` property:

```java
registerPropertyStages(
    "example:oak_sapling",
    Blocks.OAK_SAPLING,
    SaplingBlock.STAGE,
    0,
    1,
    4L
);
```

This creates:

```text
stage_0 → stage_1
stage_1 → stage_0
```

### Example: Crop

Crops use an `AGE` property:

```java
registerPropertyStages(
    "example:wheat_growth",
    Blocks.WHEAT,
    CropBlock.AGE,
    0,
    7,
    4L
);
```

This creates:

```text
stage_0 → stage_1 → stage_2 → ... → stage_7
```

and the reverse transitions as well.

### Important

Stateworks does **not** control the actual Minecraft growth.

For example, with a crop:

```text
Minecraft changes AGE
        ↓
Stateworks detects the change
        ↓
Stateworks plays the visual transition
        ↓
The block remains a normal Minecraft crop
```

Do not create a transition from a sapling to a tree. Register the sapling's existing `STAGE` property instead.

---

# Special Systems

Some blocks need custom code because their behavior cannot be handled by the generic property system.

Special systems keep their own constants and helper methods.

## 3. Repeater

Repeaters have custom Stateworks logic because the transition duration depends on the repeater's vanilla `DELAY` property.

Register the repeater with:

```java
registerRepeater(Blocks.REPEATER);
```

The repeater's visual transition states are:

```text
off → Powering → active
active → PoweringOff → off
```

The delay determines the visual duration and variant:

```text
Powering_1
Powering_2
Powering_3
Powering_4

PoweringOff_1
PoweringOff_2
PoweringOff_3
PoweringOff_4
```

Do not replace this with `registerPropertyStages(...)`.

---

## 4. Bamboo

Bamboo is a special system because it exposes three properties that affect its visual state:

- `AGE` — 0–1
- `LEAVES` — `NONE`, `SMALL`, or `LARGE`
- `STAGE` — 0–1

Register bamboo with:

```java
registerBamboo(Blocks.BAMBOO);
```

Stateworks creates a state for each combination of those properties. State names use this pattern:

```text
age_<age>_leaves_<leaves>_stage_<stage>
```

For example:

```text
age_0_leaves_none_stage_0
age_0_leaves_small_stage_0
age_1_leaves_large_stage_1
```

Bamboo is intentionally handled by its own helper rather than `registerPropertyStages(...)`, because its visual state depends on multiple properties.

---

## 4. Redstone Dust

Redstone dust is also a special system because its `POWER` property ranges from 0–15 and needs fast visual updates.

Register it with:

```java
registerRedstoneDust(
    "example:redstone_power",
    Blocks.REDSTONE_WIRE
);
```

The visual stages are:

```text
stage_0
stage_1
stage_2
...
stage_15
```

A change between adjacent power levels uses a one-tick visual transition.

Do not use the generic property helper for redstone dust.

---

## 5. Boolean Block Properties

Some blocks use a boolean property rather than an integer property.

Use:

```java
registerBooleanBlock(
    "example:block_behavior",
    Blocks.BLOCK_NAME,
    BlockClass.PROPERTY,
    "false_state",
    "true_state",
    "ForwardTransition",
    "ReverseTransition",
    DURATION_IN_TICKS
);
```

Example:

```java
registerBooleanBlock(
    "example:furnace_heat",
    Blocks.FURNACE,
    FurnaceBlock.LIT,
    "cold",
    "hot",
    "Heating",
    "Cooling",
    4L
);
```

This produces a transition between the two boolean states.

Use this only when the block's behavior fits the boolean helper. If the block requires additional logic, treat it as a special system.

---

# Choosing the Correct System

Use this quick guide:

| Block behavior | Use |
|---|---|
| Integer property representing stages/levels | `registerPropertyStages(...)` |
| Boolean property | `registerBooleanBlock(...)` |
| Repeater | `registerRepeater(...)` |
| Redstone dust | `registerRedstoneDust(...)` |
| Anything requiring unique behavior | Custom helper |

---

# 6. Choosing an ID

Each Stateworks state set needs its own ID.

For example:

```java
"example:wheat_growth"
```

The ID is used internally by Stateworks and by resource-pack definitions.

Use a clear name describing what the state set represents.

Examples:

```text
example:oak_sapling
example:wheat_growth
example:furnace_heat
example:redstone_power
```

The ID does not have to be the exact Minecraft block ID.

---

# 7. Duration

Durations passed to the helper methods are measured in **Minecraft ticks**.

```java
1L  = 1 tick
4L  = 4 ticks
```

Minecraft runs at 20 ticks per second, so:

```text
1 tick = 50 ms
4 ticks = 200 ms
```

Choose a duration that matches the visual change you want.

For fast-changing properties such as redstone power, use a short duration.

---

# 8. Resource Pack Side

Registering a block in Java creates the Stateworks states and transitions.

The resource pack supplies the visual models.

For example, a staged registration may expose:

```text
stage_0
stage_1
stage_2
...
```

The resource pack can then provide matching models such as:

```text
assets/<namespace>/models/block/<path>/stage_0.json
assets/<namespace>/models/block/<path>/stage_1.json
assets/<namespace>/models/block/<path>/stage_2.json
```

For transition-specific visuals, use the Stateworks transition format documented by the project.

The normal Minecraft `blockstates/*.json` files do not need to be replaced.

---

# 9. Automatic Rotation

Stateworks automatically handles horizontal block rotation.

Author your models facing **north**.

Stateworks rotates them automatically to match the block's horizontal facing.

Do not add a `"rotation"` field to the Stateworks state JSON.

Vertical orientation is handled by custom models when necessary.

---

# 10. Adding a New Generic Block

For a normal property-based block, the workflow is:

### Step 1

Find the appropriate Minecraft block property.

For example:

```java
CropBlock.AGE
```

### Step 2

Find the property's minimum and maximum values.

For wheat:

```text
0–7
```

### Step 3

Add the registration to `register()`:

```java
registerPropertyStages(
    "example:wheat_growth",
    Blocks.WHEAT,
    CropBlock.AGE,
    0,
    7,
    4L
);
```

### Step 4

Add the matching visual resources to your resource pack.

### Step 5

Build and test Stateworks locally.

---

# 11. When a Block Needs Custom Code

If a block cannot be represented cleanly by one of the existing helpers, do not force it into the generic system.

Instead:

1. Create a dedicated helper method in `BuiltinStateSets.java`.
2. Add any required constant near the other special-system constants.
3. Call the helper from `register()`.
4. Keep the special behavior contained in that helper.

This is how systems such as the repeater and redstone dust are handled.

The goal is to keep `register()` easy to read while allowing unusual blocks to have the custom logic they need.

---

# Quick Reference

### Generic integer property

```java
registerPropertyStages(
    "example:id",
    Blocks.BLOCK,
    SomeBlock.PROPERTY,
    0,
    7,
    4L
);
```

### Boolean property

```java
registerBooleanBlock(
    "example:id",
    Blocks.BLOCK,
    SomeBlock.PROPERTY,
    "off",
    "on",
    "TurningOn",
    "TurningOff",
    4L
);
```

### Repeater

```java
registerRepeater(Blocks.REPEATER);
```

### Redstone dust

```java
registerRedstoneDust(
    "example:redstone_power",
    Blocks.REDSTONE_WIRE
);
```

---

## The Rule of Thumb

**If the block has a normal integer or boolean property, use the generic helper.**

**If the block has special behavior, give it a dedicated helper.**

Keep the registration list in `register()` simple. The helper methods contain the machinery; the registration list describes what Stateworks should support.


## Property-aware visual variants

A transition can optionally select different frame sets based on the block's
other vanilla properties. This is useful when the property being animated is
not enough to describe the model, such as doors and trapdoors.

The normal/simple format still works:

```json
{
  "block": "minecraft:oak_door",
  "states": {
    "opening": {
      "1": "example:oak_door/opening_1",
      "2": "example:oak_door/opening_2",
      "3": "example:oak_door/opening_3",
      "4": "example:oak_door/opening_4"
    }
  }
}
```

For property-specific models, use `variants`:

```json
{
  "block": "minecraft:oak_door",
  "states": {
    "opening": {
      "variants": [
        {
          "when": {
            "hinge": "left",
            "half": "lower"
          },
          "frames": {
            "1": "example:oak_door/opening_left_1",
            "2": "example:oak_door/opening_left_2",
            "3": "example:oak_door/opening_left_3",
            "4": "example:oak_door/opening_left_4"
          }
        },
        {
          "when": {
            "hinge": "right",
            "half": "lower"
          },
          "frames": {
            "1": "example:oak_door/opening_right_1",
            "2": "example:oak_door/opening_right_2",
            "3": "example:oak_door/opening_right_3",
            "4": "example:oak_door/opening_right_4"
          }
        }
      ]
    }
  }
}
```

`when` keys are vanilla BlockState property names and values are their normal
serialized names. Variants can specify only the properties that matter.
Stateworks chooses the matching variant with the greatest number of matching
properties, so a specific variant overrides a default variant.

This means doors can keep `HINGE` and `HALF` differences while `OPEN` remains
the transition property. Trapdoors can similarly distinguish `HALF` (`top` or
`bottom`) while animating `OPEN`.

`FACING` does not normally need to be written as a variant: Stateworks already
provides the four horizontal rotations automatically. Variant conditions are
for cases where the geometry itself differs, such as hinge side or top/bottom
placement.

All existing frame-count behavior remains unchanged. A transition can contain
any number of frames, and the Java duration controls the total transition
time.
