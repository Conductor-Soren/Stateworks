package com.stateworks.state;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;
import com.stateworks.block.*;

import java.util.List;

public record StateDefinitionSet<T>(
        String id,
        List<StateDefinition<T>> states,
        StateDefinition<T> defaultState,
        List<StateTransitionDefinition<T>> transitions,
        OutputDefinition output
) {

    public StateDefinition<T> resolve(
            StateContext context
    ) {
        return states.stream()
                .filter(state ->
                        state.condition().evaluate(context)
                )
                .findFirst()
                .orElseGet(() -> {
                    if (defaultState != null) {
                        return defaultState;
                    }

                    throw new IllegalStateException(
                            "No matching state for: " + id
                    );
                });
    }

    /**
     * Returns true when this state set has a state whose condition
     * matches the supplied context.
     */
    public boolean applies(
            StateContext context
    ) {
        return states.stream()
                .anyMatch(state ->
                        state.condition().evaluate(context)
                );
    }

    /**
     * Finds a state by its name.
     */
    public StateDefinition<T> findState(
            String stateName
    ) {
        if (stateName == null) {
            return null;
        }

        return states.stream()
                .filter(state ->
                        state.name().equals(stateName)
                )
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds an on_complete transition originating from the
     * supplied state and whose condition is satisfied by the
     * supplied context.
     */
    public StateTransitionDefinition<T>
    findCompletionTransition(
            String stateName,
            StateContext context
    ) {
        if (stateName == null) {
            return null;
        }

        return transitions.stream()
                .filter(transition ->
                        transition.from().equals(stateName)
                )
                .filter(
                        StateTransitionDefinition::onComplete
                )
                .filter(transition ->
                        transition.condition()
                                .evaluate(context)
                )
                .findFirst()
                .orElse(null);
    }

    public VirtualState<T> evaluate(
            StateContext context
    ) {
        return resolve(context).evaluate(context);
    }

    public long transitionDuration(
            VirtualState<?> previous,
            VirtualState<?> next,
            StateContext context
    ) {
        if (previous == null || next == null) {
            return 0L;
        }

        return transitions.stream()
                .filter(transition ->
                        transition.applies(
                                previous.name(),
                                next.name(),
                                context
                        )
                )
                .mapToLong(transition -> transition.resolveDuration(context))
                .findFirst()
                .orElse(0L);
    }

    public StateTransitionDefinition<?> findCompletionTransitionFor(
            VirtualState<?> current,
            StateContext context
    ) {
        if (current == null) {
            return null;
        }

        return findCompletionTransition(
                current.name(),
                context
        );
    }

    public boolean isCompletionState(
            String stateName
    ) {
        if (stateName == null) {
            return false;
        }

        return transitions.stream()
                .anyMatch(transition ->
                        transition.onComplete()
                                && transition.to().equals(stateName)
                );
    }

    public boolean hasTransition(
            String from,
            String to,
            StateContext context
    ) {
        return transitions.stream()
                .anyMatch(transition ->
                        transition.applies(
                                from,
                                to,
                                context
                        )
                );
    }

    public VirtualState<T> evaluateNamedState(
            String stateName,
            StateContext context
    ) {
        StateDefinition<T> definition = findState(stateName);

        if (definition == null) {
            throw new IllegalArgumentException(
                    "Unknown Stateworks state '"
                            + stateName
                            + "' in state set '"
                            + id
                            + "'"
            );
        }

        return definition.evaluate(context);
    }

    public StateVisual visual(
            String stateName
    ) {
        StateDefinition<T> definition = findState(stateName);
        return definition != null
                ? definition.visual()
                : null;
    }

    /**
     * Returns true when this state set has an output definition.
     */
    public boolean hasOutput() {
        return output != null;
    }
}