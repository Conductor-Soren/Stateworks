# State System

Stateworks associates Minecraft block-state changes with temporary visual transitions.

```text
Minecraft state A
      ↓
state change detected
      ↓
Stateworks transition
      ↓
animation frames
      ↓
Minecraft state B
```

## Boolean states

A boolean property gives two logical states with directional transition names.

```java
registerBooleanBlock(
    "example:lever",
    Blocks.LEVER,
    LeverBlock.POWERED,
    "off",
    "on",
    "switched_on",
    "switched_off",
    4L
);
```

## Property stages

Integer properties can expose stages such as `stage_0` through `stage_7`. The helper creates transitions between the appropriate values.

## Timing

Durations use Minecraft ticks. Frame count is independent of duration.

A `4L` transition with eight frames still lasts four ticks.

## Multi-property blocks

The transition property is not necessarily the only property that matters visually.

Bamboo has `AGE`, `LEAVES`, and `STAGE`. Doors and trapdoors also have contextual properties that affect their model. Those properties should remain part of visual-state resolution while only the intended transition property drives the animation.

## Special cases

Repeater and redstone dust have timing behavior that warrants dedicated implementations rather than forcing everything through the generic helpers.

## Design goal

Keep `register()` readable and data-oriented while reusable implementation code handles repetitive state and model work.
