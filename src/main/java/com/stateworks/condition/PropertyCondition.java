package com.stateworks.condition;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;
import com.stateworks.block.*;

import net.minecraft.world.level.block.state.BlockState;

public final class PropertyCondition implements Condition {

    private final String property;
    private final String expectedValue;

    public PropertyCondition(
            String property,
            String expectedValue
    ) {
        this.property = property;
        this.expectedValue = expectedValue;
    }

    @Override
    public boolean evaluate(StateContext context) {
        BlockState state = context.getBlockState();

        var blockProperty = state.getProperties().stream()
                .filter(p -> p.getName().equals(property))
                .findFirst()
                .orElse(null);

        if (blockProperty == null) {
            return false;
        }

        return state.getValue(blockProperty)
                .toString()
                .equals(expectedValue);
    }

    @Override
    public String describe() {
        return "property="
                + property
                + ", value="
                + expectedValue;
    }
}