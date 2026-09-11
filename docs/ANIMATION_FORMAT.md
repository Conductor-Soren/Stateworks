# Animation Format

Stateworks animations are resource-pack-defined frame sequences.

## Variable frame counts

Animations are not limited to four frames. A transition configured for four ticks can contain four, eight, twelve, or more frames.

For example:

```json
{
  "1": "example:block/open_1",
  "2": "example:block/open_2",
  "3": "example:block/open_3",
  "4": "example:block/open_4",
  "5": "example:block/open_5",
  "6": "example:block/open_6",
  "7": "example:block/open_7",
  "8": "example:block/open_8"
}
```

The Java registration controls duration. The resource pack controls how many frames are available. Stateworks distributes those frames across the duration.

## Transition names

Names come from the registration. For a door, for example:

```text
open
closed
opening
closing
```

## Block-state context

Some blocks have additional properties that affect their geometry. Stateworks can use those properties when resolving visual variants while the registered transition property remains the trigger.

Examples include door hinge/half and trapdoor half.

## Rotation

Horizontal block orientation is handled by Stateworks so authors do not need four copies of an animation solely for north/south/east/west.

## Authoring model

Think of a transition as:

`vanilla state change → Stateworks transition → resource-pack frames → destination state`
