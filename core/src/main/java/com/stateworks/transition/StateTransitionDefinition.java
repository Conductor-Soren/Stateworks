package com.stateworks.transition;

import com.stateworks.core.context.StateContext;
import com.stateworks.condition.Condition;

public record StateTransitionDefinition<T>(
        String from,
        String to,
        Condition condition,
        TransitionDuration duration,
        boolean onComplete,
        String visualName
) {

    /** Backwards-compatible constructor for transitions without a visual animation. */
    public StateTransitionDefinition(
            String from,
            String to,
            Condition condition,
            TransitionDuration duration,
            boolean onComplete
    ) {
        this(from, to, condition, duration, onComplete, null);
    }

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