package com.stateworks.minecraft.context;



import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;

public final class MinecraftStateContext implements com.stateworks.core.context.StateOutputContext {

    private final Level level;
    private final BlockPos pos;
    private final BlockState state;

    public MinecraftStateContext(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
        this.state = level.getBlockState(pos);
    }

    public Level getLevel() {
        return level;
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getBlockState() {
        return state;
    }


    @Override
    public String blockId() {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(state.getBlock()).toString();
    }

    @Override
    public String propertyValue(String propertyName) {
        if (propertyName == null || propertyName.isBlank()) {
            return null;
        }

        var property = state.getBlock().getStateDefinition()
                .getProperty(propertyName);
        return property == null ? null : state.getValue(property).toString();
    }

    @Override
    public java.util.Set<String> propertyNames() {
        return state.getProperties().stream()
                .map(net.minecraft.world.level.block.state.properties.Property::getName)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean isInBlockTag(String tagId) {
        if (tagId == null || tagId.isBlank()) {
            return false;
        }
        return state.is(TagKey.create(Registries.BLOCK, Identifier.parse(tagId)));
    }


    @Override
    public com.stateworks.output.StateOutputTarget createOutputTarget(
            com.stateworks.output.OutputDefinition definition
    ) {
        return com.stateworks.minecraft.output.MinecraftStateOutputTargets
                .createFactory()
                .create(definition, level, pos);
    }

    public static MinecraftStateContext require(
            com.stateworks.core.context.StateContext context
    ) {
        if (context instanceof MinecraftStateContext minecraftContext) {
            return minecraftContext;
        }
        throw new IllegalArgumentException(
                "This Stateworks operation requires a Minecraft-backed StateContext"
        );
    }

    /**
     * Returns a numeric block property value from the current block, when
     * the property exists and its value is numeric.
     */
    @Override
    public Number getNumericProperty(String propertyName) {
        if (propertyName == null || propertyName.isBlank()) {
            return null;
        }

        for (var property : state.getProperties()) {
            if (!property.getName().equals(propertyName)) {
                continue;
            }

            Object value = state.getValue(property);
            if (value instanceof Number number) {
                return number;
            }

            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }
}