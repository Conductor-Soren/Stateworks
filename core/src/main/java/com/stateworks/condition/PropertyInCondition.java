package com.stateworks.condition;

import com.stateworks.visual.*;

import com.stateworks.core.context.StateContext;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
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
        String actualValue = context.propertyValue(propertyName);
        return actualValue != null && expectedValues.contains(actualValue);
    }

    @Override
    public String describe() {
        return "property_in="
                + propertyName
                + ", values="
                + expectedValues;
    }
}