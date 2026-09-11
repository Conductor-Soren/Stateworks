package com.stateworks.transition;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;

public final class StateTransitionTracker {

    private VirtualState<?> currentState;
    private StateTransition transition;

    /**
     * Updates the tracked state.
     *
     * The first state establishes the baseline and does not create
     * a transition.
     *
     * When the state changes, a new StateTransition is created.
     */
    public void update(
            VirtualState<?> nextState,
            long currentTime,
            long duration
    ) {
        if (currentState == null) {
            currentState = nextState;
            transition = null;
            return;
        }

        if (sameState(currentState, nextState)) {
            return;
        }

        transition = new StateTransition(
                currentState,
                nextState,
                currentTime,
                duration
        );

        currentState = nextState;
    }

    public VirtualState<?> currentState() {
        return currentState;
    }

    public StateTransition transition() {
        return transition;
    }

    public boolean isTransitioning(long currentTime) {
        return transition != null
                && !transition.isComplete(currentTime);
    }

    public float getProgress(long currentTime) {
        if (transition == null) {
            return 1.0f;
        }

        return transition.getProgress(currentTime);
    }

    public boolean isComplete(long currentTime) {
        return transition == null
                || transition.isComplete(currentTime);
    }

    public void clearTransition() {
        transition = null;
    }

    private boolean sameState(
            VirtualState<?> first,
            VirtualState<?> second
    ) {
        return first.name().equals(second.name())
                && java.util.Objects.equals(
                first.value(),
                second.value()
        );
    }
}