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

import java.util.List;

public final class PropertyInCondition implements Condition {

    private final String propertyName;
    private final List<String> expectedValues;

    public PropertyInCondition(
            String propertyName,
            List<String> expectedValues
    ) {
        this.propertyName = propertyName;
        this.expectedValues = expectedValues;
    }

    @Override
    public boolean evaluate(StateContext context) {
        BlockState state = context.getBlockState();

        Property<?> property =
                state.getBlock()
                        .getStateDefinition()
                        .getProperty(propertyName);

        if (property == null) {
            return false;
        }

        String actualValue =
                state.getValue(property).toString();

        return expectedValues.contains(actualValue);
    }

    @Override
    public String describe() {
        return "property_in="
                + propertyName
                + ", values="
                + expectedValues;
    }
}