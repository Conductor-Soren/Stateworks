package com.stateworks.signal;

import com.stateworks.core.context.StateContext;

import java.util.function.Function;

public record SignalDefinition(
        String name,
        Function<StateContext, Double> evaluator
) {

    public SignalDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Stateworks signal name cannot be null or blank"
            );
        }

        if (evaluator == null) {
            throw new IllegalArgumentException(
                    "Stateworks signal evaluator cannot be null"
            );
        }
    }

    public double evaluate(StateContext context) {
        return evaluator.apply(context);
    }
}
