package com.stateworks.context;

import com.stateworks.visual.*;

import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class StateContext {

    private final Level level;
    private final BlockPos pos;
    private final BlockState state;

    public StateContext(Level level, BlockPos pos) {
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


    /**
     * Returns a numeric block property value from the current block, when
     * the property exists and its value is numeric.
     */
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