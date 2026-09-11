package com.stateworks.output;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.network.*;
import com.stateworks.client.*;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Optional;

public final class MinecraftBlockOutputTarget
        implements StateOutputTarget {

    private final Level level;
    private final BlockPos position;
    private final Property<?> property;

    public MinecraftBlockOutputTarget(
            Level level,
            BlockPos position,
            Property<?> property
    ) {
        if (level == null) {
            throw new IllegalArgumentException(
                    "Stateworks output level cannot be null"
            );
        }

        if (position == null) {
            throw new IllegalArgumentException(
                    "Stateworks output position cannot be null"
            );
        }

        if (property == null) {
            throw new IllegalArgumentException(
                    "Stateworks output property cannot be null"
            );
        }

        this.level = level;
        this.position = position.immutable();
        this.property = property;
    }

    @Override
    public void apply(
            StateOutput output,
            long currentTime
    ) {
        if (!level.isLoaded(position)) {
            return;
        }

        BlockState state =
                level.getBlockState(position);

        if (!state.hasProperty(property)) {
            return;
        }

        Object outputValue =
                output.mappedValue(currentTime);

        if (outputValue == null) {
            return;
        }

        Optional<?> parsedValue =
                parseValue(outputValue);

        if (parsedValue.isEmpty()) {
            return;
        }

        applyValue(
                state,
                parsedValue.get()
        );
    }

    private Optional<?> parseValue(
            Object outputValue
    ) {
        String value;

        if (outputValue instanceof Number number) {
            double numericValue =
                    number.doubleValue();

            if (numericValue == Math.rint(numericValue)) {
                value =
                        Long.toString(
                                (long) numericValue
                        );
            } else {
                value =
                        Double.toString(
                                numericValue
                        );
            }
        } else {
            value =
                    String.valueOf(outputValue);
        }

        Optional<?> parsed =
                property.getValue(value);

        System.out.println(
                "Stateworks output conversion: "
                        + "property=" + property.getName()
                        + " raw=" + outputValue
                        + " text=" + value
                        + " parsed=" + parsed
        );

        return parsed;
    }

    private void applyValue(
            BlockState state,
            Object value
    ) {
        applyTypedValue(
                state,
                value,
                property
        );
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> void applyTypedValue(
            BlockState state,
            Object value,
            Property<T> property
    ) {
        T typedValue =
                (T) value;

        T current =
                state.getValue(property);

        if (java.util.Objects.equals(
                current,
                typedValue
        )) {
            return;
        }

        System.out.println(
                "Stateworks applying property: "
                        + property.getName()
                        + " current=" + current
                        + " new=" + typedValue
        );

        BlockState updated =
                state.setValue(
                        property,
                        typedValue
                );

        level.setBlock(
                position,
                updated,
                3
        );
    }

    public Level level() {
        return level;
    }

    public BlockPos position() {
        return position;
    }

    public Property<?> property() {
        return property;
    }
}