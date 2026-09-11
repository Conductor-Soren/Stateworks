# Stateworks Java Block API

Stateworks is intentionally focused on states and visual transitions. Neighbor/context behavior is outside the Stateworks API.

## Simple boolean blocks

Add blocks in Java with:

```java
registerBooleanBlock(
    "example:my_block",
    Blocks.MY_BLOCK,
    MyBlock.ACTIVE,
    "off",
    "active",
    "Activating",
    "Deactivating",
    4
);
```

The final argument is the fixed transition duration in Minecraft ticks.

## Repeater

The built-in repeater reads its vanilla `DELAY` property (1-4). Its transition duration follows that delay, and resource-pack visual names can be:

`Powering_1` ... `Powering_4`

`PoweringOff_1` ... `PoweringOff_4`

## Multi-property blocks

For blocks such as the copper bulb, define the four logical combinations explicitly with Java `Condition` lambdas and register them as a state set.
