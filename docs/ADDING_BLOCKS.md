# Adding Blocks

Built-in registrations live in `src/main/java/com/stateworks/state/BuiltinStateSets.java`.

## Boolean block

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

`4L` is four Minecraft ticks.

## Block families

If multiple blocks share the same behavior, use the family helper instead of repeating registrations:

```java
registerBooleanBlocks(
        "",
        ButtonBlock.POWERED,
        "off", "on", "pressed", "unpressed", 4L,
        Blocks.OAK_BUTTON,
        Blocks.SPRUCE_BUTTON
        // ...
);
```

The helper derives each Stateworks ID from the registered Minecraft block.

## Property stages

For an integer property with a known range:

```java
registerPropertyStages(
        "example:cocoa",
        Blocks.COCOA,
        CocoaBlock.AGE,
        0,
        2,
        4L
);
```

For a family:

```java
registerPropertyStagesBlocks(
        "",
        CropBlock.AGE,
        0,
        7,
        4L,
        Blocks.WHEAT,
        Blocks.CARROTS,
        Blocks.POTATOES
);
```

Use the property's actual Minecraft property constant when possible. Matching numeric ranges do not necessarily mean two blocks use the same property object.

## Bamboo

Bamboo has multiple relevant properties and uses its dedicated helper:

```java
registerBamboo(Blocks.BAMBOO);
```

## Special systems

Some blocks need dedicated logic because their behavior depends on more than a simple state change. Examples include the repeater and redstone dust systems.

## Other visual properties

A transition property can drive the animation while other block-state properties determine the visual context. Doors and trapdoors are examples: hinge/half and other properties must remain available to model selection.

## Naming

Prefer lowercase resource IDs:

```text
example:oak_button_pressed
```

## Testing

After changing registrations:

```powershell
.\gradlew clean build
```

Then:

```powershell
.\gradlew runclient
```

Test both directions, orientations, relevant variants, and rapid state changes.
