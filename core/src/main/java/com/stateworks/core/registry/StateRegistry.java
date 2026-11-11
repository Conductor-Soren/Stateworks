package com.stateworks.core.registry;

import com.stateworks.core.context.StateContext;
import com.stateworks.signal.SignalDefinitionSet;
import com.stateworks.state.StateDefinition;
import com.stateworks.state.StateDefinitionSet;

import java.util.HashMap;
import java.util.Map;

public final class StateRegistry {

    public static final StateRegistry INSTANCE =
            new StateRegistry();

    private final Map<String, StateDefinition<?>> builtInDefinitions = new HashMap<>();
    private final Map<String, StateDefinitionSet<?>> builtInSets = new HashMap<>();
    private final Map<String, SignalDefinitionSet> builtInSignalSets = new HashMap<>();

    private volatile Map<String, StateDefinition<?>> definitions = new HashMap<>();
    private volatile Map<String, StateDefinitionSet<?>> sets = new HashMap<>();
    private volatile Map<String, SignalDefinitionSet> signalSets = new HashMap<>();

    private Map<String, StateDefinition<?>> dataDefinitions = new HashMap<>();
    private Map<String, StateDefinitionSet<?>> dataSets = new HashMap<>();
    private Map<String, SignalDefinitionSet> dataSignalSets = new HashMap<>();

    private StateRegistry() {
    }

    public synchronized <T> void register(StateDefinition<T> definition) {
        builtInDefinitions.put(definition.name(), definition);
        rebuildMergedMaps();
    }

    public synchronized <T> void registerSet(StateDefinitionSet<T> set) {
        builtInSets.put(set.id(), set);
        rebuildMergedMaps();
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

    public synchronized void registerSignalSet(SignalDefinitionSet set) {
        builtInSignalSets.put(set.id(), set);
        rebuildMergedMaps();
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
     * Atomically replaces resource-driven definitions while preserving built-in
     * Java registrations. Resource definitions take precedence on duplicate IDs.
     */
    public synchronized void replaceDataDriven(
            Map<String, StateDefinition<?>> definitions,
            Map<String, StateDefinitionSet<?>> sets,
            Map<String, SignalDefinitionSet> signalSets
    ) {
        this.dataDefinitions = new HashMap<>(definitions);
        this.dataSets = new HashMap<>(sets);
        this.dataSignalSets = new HashMap<>(signalSets);
        rebuildMergedMaps();
    }

    /** Replaces every registry entry, including built-in registrations. */
    public synchronized void replaceAll(
            Map<String, StateDefinition<?>> definitions,
            Map<String, StateDefinitionSet<?>> sets,
            Map<String, SignalDefinitionSet> signalSets
    ) {
        builtInDefinitions.clear();
        builtInSets.clear();
        builtInSignalSets.clear();
        dataDefinitions = new HashMap<>(definitions);
        dataSets = new HashMap<>(sets);
        dataSignalSets = new HashMap<>(signalSets);
        rebuildMergedMaps();
    }

    public synchronized void clear() {
        builtInDefinitions.clear();
        builtInSets.clear();
        builtInSignalSets.clear();
        dataDefinitions.clear();
        dataSets.clear();
        dataSignalSets.clear();
        rebuildMergedMaps();
    }

    private void rebuildMergedMaps() {
        Map<String, StateDefinition<?>> mergedDefinitions = new HashMap<>(builtInDefinitions);
        mergedDefinitions.putAll(dataDefinitions);

        Map<String, StateDefinitionSet<?>> mergedSets = new HashMap<>(builtInSets);
        mergedSets.putAll(dataSets);

        Map<String, SignalDefinitionSet> mergedSignalSets = new HashMap<>(builtInSignalSets);
        mergedSignalSets.putAll(dataSignalSets);

        definitions = Map.copyOf(mergedDefinitions);
        sets = Map.copyOf(mergedSets);
        signalSets = Map.copyOf(mergedSignalSets);
    }
}