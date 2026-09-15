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

    /** Creates a fixed duration in Minecraft ticks (1 tick = 50 ms). */
    public static TransitionDuration fixedTicks(long ticks) {
        return fixed(Math.max(0L, ticks) * 50L);
    }

    /** Creates a duration based on a numeric property measured in ticks. */
    public static TransitionDuration blockPropertyTicks(
            String property,
            long scaleTicks
    ) {
        return blockProperty(property, Math.max(0L, scaleTicks) * 50L);
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
