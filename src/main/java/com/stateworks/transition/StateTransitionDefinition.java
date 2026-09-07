package com.stateworks.transition;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;
import com.stateworks.block.*;

public record StateTransitionDefinition<T>(
        String from,
        String to,
        Condition condition,
        TransitionDuration duration,
        boolean onComplete
) {

    public boolean applies(
            String currentState,
            String nextState,
            StateContext context
    ) {
        return from.equals(currentState)
                && to.equals(nextState)
                && condition.evaluate(context);
    }

    public long resolveDuration(StateContext context) {
        return duration == null
                ? 0L
                : duration.resolve(context);
    }

    public boolean appliesOnComplete(
            String currentState,
            StateContext context
    ) {
        return onComplete
                && from.equals(currentState)
                && condition.evaluate(context);
    }
}