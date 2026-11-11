package com.stateworks.condition;

import com.stateworks.visual.*;

import com.stateworks.core.context.StateContext;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
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
        return expectedValue.equals(context.propertyValue(propertyName));
    }

    @Override
    public String describe() {
        return "property_equals="
                + propertyName
                + ", value="
                + expectedValue;
    }
}