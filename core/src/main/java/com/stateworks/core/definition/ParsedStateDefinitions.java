package com.stateworks.core.definition;

import com.stateworks.signal.SignalDefinitionSet;
import com.stateworks.state.StateDefinition;
import com.stateworks.state.StateDefinitionSet;

import java.util.Map;

/** Immutable result of parsing a single Stateworks definition resource. */
public record ParsedStateDefinitions(
        Map<String, StateDefinition<?>> definitions,
        Map<String, StateDefinitionSet<?>> stateSets,
        Map<String, SignalDefinitionSet> signalSets
) {
    public ParsedStateDefinitions {
        definitions = Map.copyOf(definitions);
        stateSets = Map.copyOf(stateSets);
        signalSets = Map.copyOf(signalSets);
    }
}
