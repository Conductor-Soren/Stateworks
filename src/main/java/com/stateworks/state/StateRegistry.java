package com.stateworks.state;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;

import java.util.HashMap;
import java.util.Map;

public final class StateRegistry {

    public static final StateRegistry INSTANCE =
            new StateRegistry();

    private volatile Map<String, StateDefinition<?>> definitions =
            new HashMap<>();

    private volatile Map<String, StateDefinitionSet<?>> sets =
            new HashMap<>();

    private volatile Map<String, SignalDefinitionSet> signalSets =
            new HashMap<>();

    private StateRegistry() {
    }

    public <T> void register(
            StateDefinition<T> definition
    ) {
        definitions.put(
                definition.name(),
                definition
        );
    }

    public <T> void registerSet(
            StateDefinitionSet<T> set
    ) {
        sets.put(
                set.id(),
                set
        );
    }



    public StateDefinition<?> get(
            String name
    ) {
        return definitions.get(name);
    }

    public StateDefinitionSet<?> getSet(
            String name
    ) {
        return sets.get(name);
    }

    public boolean applies(
            String stateSetId,
            StateContext context
    ) {
        StateDefinitionSet<?> set = sets.get(stateSetId);
        return set != null && set.applies(context);
    }

    /**
     * Finds the first registered Stateworks state set that applies to the
     * supplied block context. Built-in and data-driven sets can therefore be
     * selected from the block itself instead of callers hard-coding a set ID.
     */
    public String findApplicableSet(
            StateContext context
    ) {
        return sets.entrySet().stream()
                .filter(entry -> entry.getValue().applies(context))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public void registerSignalSet(
            SignalDefinitionSet set
    ) {
        signalSets.put(
                set.id(),
                set
        );
    }

    public SignalDefinitionSet getSignalSet(
            String name
    ) {
        return signalSets.get(name);
    }

    public boolean containsSignalSet(String name) {
        return signalSets.containsKey(name);
    }

    public boolean contains(
            String name
    ) {
        return definitions.containsKey(name)
                || sets.containsKey(name)
                || signalSets.containsKey(name);
    }

    public boolean containsDefinition(
            String name
    ) {
        return definitions.containsKey(name);
    }

    public boolean containsSet(
            String name
    ) {
        return sets.containsKey(name);
    }

    /**
     * Atomically replaces all data-driven definitions produced by a resource reload.
     * Existing machines therefore see either the old complete registry or the new
     * complete registry, never a partially loaded one.
     */
    public synchronized void replaceAll(
            Map<String, StateDefinition<?>> definitions,
            Map<String, StateDefinitionSet<?>> sets,
            Map<String, SignalDefinitionSet> signalSets
    ) {
        this.definitions = new HashMap<>(definitions);
        this.sets = new HashMap<>(sets);
        this.signalSets = new HashMap<>(signalSets);
    }

    public synchronized void clear() {
        definitions = new HashMap<>();
        sets = new HashMap<>();
        signalSets = new HashMap<>();
    }
}