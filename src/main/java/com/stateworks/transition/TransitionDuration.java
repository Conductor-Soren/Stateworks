package com.stateworks.transition;

import com.stateworks.context.StateContext;

import java.util.Objects;

/**
 * Describes how long a Stateworks transition lasts.
 *
 * A duration may be fixed or derived from a numeric block property in the
 * current StateContext. This keeps timing data declarative while allowing
 * visual transitions to follow vanilla block timing.
 */
public record TransitionDuration(
        long fixedMillis,
        String property,
        long propertyScaleMillis
) {

    public static TransitionDuration fixed(long millis) {
        return new TransitionDuration(millis, null, 0L);
    }

    public static TransitionDuration blockProperty(
            String property,
            long scaleMillis
    ) {
        if (property == null || property.isBlank()) {
            throw new IllegalArgumentException(
                    "Transition duration property cannot be blank"
            );
        }

        if (scaleMillis < 0L) {
            throw new IllegalArgumentException(
                    "Transition duration scale cannot be negative"
            );
        }

        return new TransitionDuration(0L, property, scaleMillis);
    }

    public long resolve(StateContext context) {
        if (property == null) {
            return Math.max(0L, fixedMillis);
        }

        if (context == null) {
            return 0L;
        }

        Number value = context.getNumericProperty(property);
        if (value == null) {
            return 0L;
        }

        return Math.max(
                0L,
                Math.round(value.doubleValue() * propertyScaleMillis)
        );
    }

    public boolean isFixed() {
        return property == null;
    }
}
