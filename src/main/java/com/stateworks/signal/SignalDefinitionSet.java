package com.stateworks.signal;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;
import com.stateworks.block.*;

import java.util.List;

public record SignalDefinitionSet(
        String id,
        List<SignalDefinition> signals
) {

    public SignalDefinitionSet {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "Stateworks signal set ID cannot be null or blank"
            );
        }

        if (signals == null || signals.isEmpty()) {
            throw new IllegalArgumentException(
                    "Stateworks signal set must contain at least one signal"
            );
        }

        signals = List.copyOf(signals);
    }

    public java.util.Map<String, Double> evaluate(
            StateContext context
    ) {
        java.util.Map<String, Double> values = new java.util.HashMap<>();

        for (SignalDefinition signal : signals) {
            values.put(signal.name(), signal.evaluate(context));
        }

        return java.util.Map.copyOf(values);
    }

    public SignalDefinition findSignal(String name) {
        if (name == null) {
            return null;
        }

        return signals.stream()
                .filter(signal -> signal.name().equals(name))
                .findFirst()
                .orElse(null);
    }
}
