
## Redstone dust

```java
registerRedstoneDust(
    "example:redstone_power",
    Blocks.REDSTONE_WIRE
);
```

This exposes POWER 0-15 as `stage_0` through `stage_15`. Adjacent changes use
a 1-tick visual transition in both directions. Minecraft remains responsible
for the actual redstone signal.
