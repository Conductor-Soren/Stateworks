package com.stateworks.condition;

import com.stateworks.core.context.StateContext;

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
        return expectedValue.equals(context.propertyValue(property));
    }

    @Override
    public String describe() {
        return "property="
                + property
                + ", value="
                + expectedValue;
    }
}