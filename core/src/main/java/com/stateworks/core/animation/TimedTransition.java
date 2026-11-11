package com.stateworks.core.animation;

/**
 * A time-based transition between two engine values.
 *
 * This class deliberately has no Minecraft or loader dependencies. It only
 * describes the transition endpoints and its timing.
 */
public record TimedTransition<T>(
        T previous,
        T next,
        long startTime,
        long duration
) {

    /** Returns normalized transition progress in the inclusive range [0, 1]. */
    public float progress(long currentTime) {
        if (duration <= 0L) {
            return 1.0F;
        }

        long elapsed = currentTime - startTime;
        if (elapsed <= 0L) {
            return 0.0F;
        }
        if (elapsed >= duration) {
            return 1.0F;
        }

        return (float) elapsed / (float) duration;
    }

    public boolean isComplete(long currentTime) {
        return progress(currentTime) >= 1.0F;
    }
}
