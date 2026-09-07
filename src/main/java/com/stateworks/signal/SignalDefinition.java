package com.stateworks.signal;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;
import com.stateworks.block.*;

import java.util.function.Function;

/**
 * A Stateworks signal is derived data exposed for presentation.
 * It does not modify Minecraft's authoritative BlockState.
 */
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
