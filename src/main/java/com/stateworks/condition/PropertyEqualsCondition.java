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
import net.minecraft.world.level.block.state.properties.Property;

public final class PropertyEqualsCondition implements Condition {

    private final String propertyName;
    private final String expectedValue;

    public PropertyEqualsCondition(
            String propertyName,
            String expectedValue
    ) {
        this.propertyName = propertyName;
        this.expectedValue = expectedValue;
    }

    @Override
    public boolean evaluate(StateContext context) {
        BlockState state = context.getBlockState();

        Property<?> property =
                state.getBlock().getStateDefinition().getProperty(propertyName);

        if (property == null) {
            return false;
        }

        return state.getValue(property).toString().equals(expectedValue);
    }

    @Override
    public String describe() {
        return "property_equals="
                + propertyName
                + ", value="
                + expectedValue;
    }
}