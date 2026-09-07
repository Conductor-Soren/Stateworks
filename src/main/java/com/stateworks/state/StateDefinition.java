package com.stateworks.state;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;
import com.stateworks.block.*;

import java.util.function.Function;

public record StateDefinition<T>(
        String name,
        Function<StateContext, T> evaluator,
        Condition condition,
        String visualModel
) {

    public StateDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Stateworks state name cannot be null or blank"
            );
        }

        if (evaluator == null) {
            throw new IllegalArgumentException(
                    "Stateworks state evaluator cannot be null"
            );
        }

        if (condition == null) {
            throw new IllegalArgumentException(
                    "Stateworks state condition cannot be null"
            );
        }
    }

    public VirtualState<T> evaluate(
            StateContext context
    ) {
        return new VirtualState<>(
                name,
                evaluator.apply(context)
        );
    }

    public StateVisual visual() {
        if (visualModel == null
                || visualModel.isBlank()) {
            return null;
        }

        return new StateVisual(
                name,
                visualModel
        );
    }
}