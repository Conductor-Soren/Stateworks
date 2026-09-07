package com.stateworks.output;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.network.*;
import com.stateworks.client.*;
import com.stateworks.block.*;

public final class StateOutput {

    private final StateMachine machine;

    private StateOutputTarget target;

    public StateOutput(StateMachine machine) {
        if (machine == null) {
            throw new IllegalArgumentException(
                    "Stateworks machine cannot be null"
            );
        }

        this.machine = machine;
    }

    public Object value(long currentTime) {
        VirtualState<?> state =
                machine.stableState(currentTime);

        if (state == null) {
            return null;
        }

        return state.value();
    }

    /**
     * Returns the configured output value for the current
     * Stateworks state.
     *
     * If the state has an explicit output mapping, that
     * mapping takes precedence over the state's normal value.
     */
    public Object mappedValue(long currentTime) {
        VirtualState<?> state =
                machine.stableState(currentTime);

        if (state == null) {
            return null;
        }

        OutputDefinition definition =
                machine.definitionOutput();

        if (definition != null) {

            Object mapped =
                    definition.values()
                            .get(state.name());

            if (mapped != null) {
                return mapped;
            }
        }

        return state.value();
    }

    /**
     * Retained for backwards compatibility with the
     * existing boolean output system.
     */
    public boolean isActive(long currentTime) {
        Object value =
                mappedValue(currentTime);

        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }

        return "active".equals(
                state(currentTime) != null
                        ? state(currentTime).name()
                        : null
        );
    }

    public VirtualState<?> state(long currentTime) {
        return machine.stableState(currentTime);
    }

    public StateMachine machine() {
        return machine;
    }

    public void setTarget(
            StateOutputTarget target
    ) {
        this.target = target;
    }

    public StateOutputTarget target() {
        return target;
    }

    public void apply(long currentTime) {

        if (target == null) {
return;
        }

        System.out.println(
                "Stateworks output: applying target "
                        + target.getClass().getSimpleName()
        );

        target.apply(
                this,
                currentTime
        );
    }
}