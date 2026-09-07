package com.stateworks.context;

import com.stateworks.visual.*;

import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;
import com.stateworks.block.*;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class BlockStatePropertyResolver {

    private BlockStatePropertyResolver() {
    }

    public static Property<?> resolve(
            BlockState state,
            String propertyName
    ) {
        if (state == null) {
            throw new IllegalArgumentException(
                    "Stateworks target block state cannot be null"
            );
        }

        if (propertyName == null
                || propertyName.isBlank()) {
            throw new IllegalArgumentException(
                    "Stateworks property name cannot be null "
                            + "or blank"
            );
        }

        for (Property<?> property :
                state.getProperties()) {

            if (property.getName()
                    .equals(propertyName)) {

                return property;
            }
        }

        throw new IllegalArgumentException(
                "Block "
                        + state.getBlock()
                        + " does not contain a property named '"
                        + propertyName
                        + "'. Available properties: "
                        + state.getProperties()
                        .stream()
                        .map(Property::getName)
                        .sorted()
                        .toList()
        );
    }

    public static Property<?> tryResolve(
            BlockState state,
            String name
    ) {
        for (Property<?> property :
                state.getProperties()) {

            if (property.getName().equals(name)) {
                return property;
            }
        }

        return null;
    }
}