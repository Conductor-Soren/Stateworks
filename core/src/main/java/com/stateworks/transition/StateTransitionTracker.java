package com.stateworks.transition;

import com.stateworks.core.animation.AnimationTimeline;
import com.stateworks.state.VirtualState;

/**
 * Stateworks adapter around the game-independent animation timeline.
 *
 * This preserves the existing state-machine API while keeping the actual
 * time/progress bookkeeping in the pure Java core package.
 */
public final class StateTransitionTracker {

    private final AnimationTimeline<VirtualState<?>> timeline =
            new AnimationTimeline<>();

    /**
     * Updates the tracked state. The first state establishes a silent
     * baseline; subsequent changes start a timed transition.
     */
    public void update(
            VirtualState<?> nextState,
            long currentTime,
            long duration
    ) {
        timeline.update(
                nextState,
                currentTime,
                duration,
                StateTransitionTracker::sameState
        );
    }

    public VirtualState<?> currentState() {
        return timeline.current();
    }

    public StateTransition transition() {
        var transition = timeline.transition();
        if (transition == null) {
            return null;
        }

        return new StateTransition(
                transition.previous(),
                transition.next(),
                transition.startTime(),
                transition.duration()
        );
    }

    public boolean isTransitioning(long currentTime) {
        return timeline.isTransitioning(currentTime);
    }

    public float getProgress(long currentTime) {
        return timeline.progress(currentTime);
    }

    public boolean isComplete(long currentTime) {
        return timeline.isComplete(currentTime);
    }

    public void clearTransition() {
        timeline.clearTransition();
    }

    public void reset() {
        timeline.reset();
    }

    private static boolean sameState(
            VirtualState<?> first,
            VirtualState<?> second
    ) {
        return first.name().equals(second.name())
                && java.util.Objects.equals(first.value(), second.value());
    }
}
