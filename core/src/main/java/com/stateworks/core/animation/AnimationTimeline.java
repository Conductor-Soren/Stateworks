package com.stateworks.core.animation;

import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Loader- and game-independent transition timeline.
 *
 * The timeline owns only the previous/current values and timing. Callers are
 * responsible for evaluating game state, choosing durations, and applying
 * the resulting progress to their renderer or other presentation layer.
 */
public final class AnimationTimeline<T> {

    private T current;
    private TimedTransition<T> transition;

    /**
     * Establishes the first value as a silent baseline, or starts a transition
     * when the supplied value differs from the current value.
     */
    public void update(
            T next,
            long currentTime,
            long duration,
            BiPredicate<? super T, ? super T> sameValue
    ) {
        Objects.requireNonNull(sameValue, "sameValue");

        if (current == null) {
            current = next;
            transition = null;
            return;
        }

        if (sameValue.test(current, next)) {
            return;
        }

        transition = new TimedTransition<>(
                current,
                next,
                currentTime,
                Math.max(0L, duration)
        );
        current = next;
    }

    public T current() {
        return current;
    }

    public TimedTransition<T> transition() {
        return transition;
    }

    public boolean isTransitioning(long currentTime) {
        return transition != null && !transition.isComplete(currentTime);
    }

    public float progress(long currentTime) {
        return transition == null ? 1.0F : transition.progress(currentTime);
    }

    public boolean isComplete(long currentTime) {
        return transition == null || transition.isComplete(currentTime);
    }

    public void clearTransition() {
        transition = null;
    }

    public void reset() {
        current = null;
        transition = null;
    }
}
