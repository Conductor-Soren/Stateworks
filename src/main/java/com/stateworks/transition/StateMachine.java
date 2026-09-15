package com.stateworks.transition;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;

public final class StateMachine {

    private final StateRegistry registry;
    private final String stateSetId;

    private final StateTransitionTracker tracker =
            new StateTransitionTracker();

    private final StateOutput output =
            new StateOutput(this);

    private VirtualState<?> desiredState;
    // True while an externally-triggered delayed-block transition is waiting
    // for the vanilla block state to catch up.
    private boolean forcedTransitionPending;
    private StateContext context;

    public StateMachine(String stateSetId) {
        this(
                StateRegistry.INSTANCE,
                stateSetId
        );
    }

    public StateMachine(
            StateRegistry registry,
            String stateSetId
    ) {
        if (registry == null) {
            throw new IllegalArgumentException(
                    "Stateworks registry cannot be null"
            );
        }

        if (stateSetId == null || stateSetId.isBlank()) {
            throw new IllegalArgumentException(
                    "Stateworks state set ID cannot be null or blank"
            );
        }

        StateDefinitionSet<?> definitionSet = registry.getSet(stateSetId);

        if (definitionSet == null) {
            throw new IllegalArgumentException(
                    "Unknown Stateworks state set: " + stateSetId
            );
        }

        this.registry = registry;
        this.stateSetId = stateSetId;
    }

    public void update(
            StateContext context,
            long currentTime
    ) {
        if (!definitionSet().applies(context)) {
            reset();
            return;
        }

        this.context = context;

        /*
         * An externally-triggered transition (for example a delayed repeater
         * input or an explicitly observed block edge) owns the machine until
         * its visual transition finishes. Do not immediately replace it with
         * the ordinary block-state evaluation.
         */
        if (forcedTransitionPending) {
            if (tracker.isTransitioning(currentTime)) {
                return;
            }
            forcedTransitionPending = false;
        }

        /*
         * Establish a silent baseline when this machine is first observed.
         * If the first observation happens after a vanilla state change, the
         * default state is still available as the previous endpoint, so the
         * first real trigger can animate instead of being treated as an
         * already-completed state.
         */
        if (tracker.currentState() == null) {
            VirtualState<?> baseline =
                    definitionSet().defaultState().evaluate(context);
            tracker.update(baseline, currentTime, 0L);
        }

        OutputDefinition outputDefinition =
                definitionSet().output();

        if (outputDefinition != null) {

            StateOutputTarget target =
                    StateOutputTargets
                            .createFactory()
                            .create(
                                    outputDefinition,
                                    context.getLevel(),
                                    context.getPos()
                            );

            output.setTarget(target);

        } else {

            output.setTarget(null);
        }

        VirtualState<?> evaluatedState =
                definitionSet().evaluate(context);

        VirtualState<?> currentState =
                tracker.currentState();

        /*
         * A state reached by an on_complete transition owns
         * the machine while its context remains valid.
         *
         * Example:
         *
         * powered=true
         *
         * ACTIVATING -> ACTIVE
         *
         * The context still resolves to ACTIVATING, but ACTIVE
         * must remain stable until the context actually requests
         * a different explicit transition.
         */
        if (currentState != null
                && definitionSet().isCompletionState(currentState.name())
                && !currentState.name().equals(
                evaluatedState.name()
        )
                && !definitionSet().hasTransition(
                currentState.name(),
                evaluatedState.name(),
                context
        )) {

            desiredState = currentState;
            return;
        }

        desiredState = evaluatedState;

        long duration =
                definitionSet().transitionDuration(
                        tracker.currentState(),
                        evaluatedState,
                        context
                );

        boolean hadTransition =
                tracker.transition() != null;

        tracker.update(
                evaluatedState,
                currentTime,
                duration
        );

        boolean transitionCompleted =
                hadTransition
                        && tracker.isComplete(
                        currentTime
                );

        if (transitionCompleted) {
            tracker.clearTransition();

            advanceCompletedTransition(
                    currentTime
            );
        } else if (tracker.isComplete(
                currentTime
        )) {
            tracker.clearTransition();
        }
    }

    private void advanceCompletedTransition(
            long currentTime
    ) {
        final int maxSteps = 32;

        for (int step = 0;
             step < maxSteps;
             step++) {

            VirtualState<?> current =
                    tracker.currentState();

            if (current == null) {
                return;
            }

            StateTransitionDefinition<?> completion =
                    definitionSet().findCompletionTransitionFor(
                            current,
                            context
                    );

            if (completion == null) {
                return;
            }

            VirtualState<?> next =
                    definitionSet().evaluateNamedState(completion.to(), context);

            desiredState = next;

            tracker.update(
                    next,
                    currentTime,
                    completion.resolveDuration(context)
            );

            if (!tracker.isComplete(
                    currentTime
            )) {
                return;
            }

            tracker.clearTransition();
        }

        throw new IllegalStateException(
                "Too many chained Stateworks completion "
                        + "transitions in state set: "
                        + stateSetId
        );
    }

    /**
     * Evaluates all data-driven signals for the machine's current context.
     * Signals are presentation data; they never modify Minecraft state.
     */
    public java.util.Map<String, Double> signalValues() {
        if (context == null) {
            return java.util.Map.of();
        }

        SignalDefinitionSet signalSet =
                registry.getSignalSet(stateSetId);

        return signalSet == null
                ? java.util.Map.of()
                : signalSet.evaluate(context);
    }

    /**
     * Starts a transition immediately for an externally detected event.
     *
     * This is used for delayed vanilla blocks such as repeaters: the input
     * signal changes now, while the block's POWERED property changes later.
     * The normal evaluator must not wait for that delayed property.
     */
    public void forceTransition(
            String stateName,
            StateContext context,
            long currentTime
    ) {
        if (context == null || stateName == null) {
            return;
        }

        StateDefinitionSet<?> definitions = definitionSet();

        if (!definitions.applies(context)) {
            return;
        }

        VirtualState<?> next = definitions.evaluateNamedState(
                stateName,
                context
        );

        VirtualState<?> current = tracker.currentState();

        this.context = context;

        /*
         * An event can arrive before this block has ever been tracked.
         * Establish the normal evaluated state silently as the transition
         * baseline. Do not emit a visual packet for that baseline.
         */
        if (current == null) {
            current = definitions.defaultState().evaluate(context);
            tracker.update(current, currentTime, 0L);
        }

        if (current.name().equals(next.name())) {
            return;
        }

        long duration = definitions.transitionDuration(
                current,
                next,
                context
        );

        this.desiredState = next;
        this.forcedTransitionPending = true;

        tracker.update(
                next,
                currentTime,
                duration
        );
    }

    public void tick(long currentTime) {
        if (context == null) {
            return;
        }

        update(
                context,
                currentTime
        );
    }

    public Object interpolatedValue(
            long currentTime
    ) {
        StateTransition transition =
                tracker.transition();

        if (transition == null) {
            VirtualState<?> current =
                    tracker.currentState();

            if (current != null) {
                return current.value();
            }

            if (desiredState != null) {
                return desiredState.value();
            }

            return null;
        }

        VirtualState<?> previous =
                transition.previous();

        VirtualState<?> next =
                transition.next();

        if (previous == null || next == null) {
            return next != null
                    ? next.value()
                    : previous != null
                    ? previous.value()
                    : null;
        }

        Object previousValue =
                previous.value();

        Object nextValue =
                next.value();

        if (previousValue instanceof Number previousNumber
                && nextValue instanceof Number nextNumber) {

            float progress =
                    transition.getProgress(
                            currentTime
                    );

            return StateInterpolator.linear(
                    previousNumber.doubleValue(),
                    nextNumber.doubleValue(),
                    progress
            );
        }

        if (transition.isComplete(
                currentTime
        )) {
            return nextValue;
        }

        return previousValue;
    }

    public void setContext(
            StateContext context
    ) {
        this.context = context;
    }

    public StateContext context() {
        return context;
    }

    private StateDefinitionSet<?> definitionSet() {
        StateDefinitionSet<?> set = registry.getSet(stateSetId);

        if (set == null) {
            throw new IllegalStateException(
                    "Stateworks state set was removed during runtime: " + stateSetId
            );
        }

        return set;
    }

    public String stateSetId() {
        return stateSetId;
    }

    public VirtualState<?> currentState() {
        return tracker.currentState();
    }

    public VirtualState<?> desiredState() {
        return desiredState;
    }

    public VirtualState<?> stableState(
            long currentTime
    ) {
        if (tracker.isTransitioning(
                currentTime
        )) {
            return tracker.currentState();
        }

        return desiredState;
    }

    public boolean hasCompleted(
            long currentTime
    ) {
        return desiredState != null
                && tracker.isComplete(
                currentTime
        );
    }

    public StateTransition transition() {
        return tracker.transition();
    }

    public boolean isTransitioning(
            long currentTime
    ) {
        return tracker.isTransitioning(
                currentTime
        );
    }

    public float getProgress(
            long currentTime
    ) {
        return tracker.getProgress(
                currentTime
        );
    }

    public boolean isComplete(
            long currentTime
    ) {
        return tracker.isComplete(
                currentTime
        );
    }

    public OutputDefinition definitionOutput() {
        return definitionSet().output();
    }

    public StateOutput output() {
        return output;
    }

    public void clearTransition() {
        tracker.clearTransition();
    }

    public void reset() {
        desiredState = null;
        context = null;
        tracker.clearTransition();
        output.setTarget(null);
    }
}