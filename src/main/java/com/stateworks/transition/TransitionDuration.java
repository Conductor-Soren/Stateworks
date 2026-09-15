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

    /**
     * Creates a duration based on a numeric block property with an additional
     * fixed number of ticks. This is useful where the visual transition
     * endpoint is one tick later than the vanilla property timing.
     */
    public static TransitionDuration blockPropertyTicksPlus(
            String property,
            long scaleTicks,
            long extraTicks
    ) {
        return new TransitionDuration(
                Math.max(0L, extraTicks) * 50L,
                property,
                Math.max(0L, scaleTicks) * 50L
        );
    }

    /**
     * Repeater timing used by the built-in repeater presentation.
     *
     * Vanilla repeater delay values are 1..4. The first two values already
     * line up with Stateworks' presentation timing; for the longer delays,
     * use the repeater's full two-ticks-per-delay timing.
     */
    public static TransitionDuration repeaterTicks(String property) {
        return new TransitionDuration(
                0L,
                property + "|repeaterTicks",
                50L
        );
    }

    public long resolve(StateContext context) {
        if (property == null) {
            return Math.max(0L, fixedMillis);
        }

        if (context == null) {
            return 0L;
        }

        if (property.endsWith("|repeaterTicks")) {
            String propertyName =
                    property.substring(0, property.length() - "|repeaterTicks".length());

            Number value = context.getNumericProperty(propertyName);
            if (value == null) {
                return 0L;
            }

            int delay = Math.max(1, value.intValue());

            /*
             * Repeater delay values map directly to vanilla redstone ticks:
             *   delay 1 -> 2 ticks
             *   delay 2 -> 4 ticks
             *   delay 3 -> 6 ticks
             *   delay 4 -> 8 ticks
             */
            long ticks = delay * 2L;
            return ticks * 50L;
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
