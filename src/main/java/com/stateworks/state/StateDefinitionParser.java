package com.stateworks.state;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;

public final class StateDefinitionParser {

    private StateDefinitionParser() {
    }

    public static StateDefinition<?> parse(
            JsonObject json
    ) {
        String id = json.get("id").getAsString();
        String type = json.get("type").getAsString();

        if (!type.equals("boolean")) {
            throw new IllegalArgumentException("Unsupported Stateworks state type: " + type);
        }

        Condition condition = ConditionParser.parse(json.getAsJsonObject("condition"));
        String visualModel = null;

        if (json.has("visual")) {
            JsonElement visual = json.get("visual");
            visualModel = visual.isJsonPrimitive()
                    ? visual.getAsString()
                    : visual.getAsJsonObject().get("model").getAsString();
        }

        return new StateDefinition<>(id, condition::evaluate, condition, visualModel);
    }

    public static SignalDefinitionSet parseSignals(
            JsonObject json
    ) {
        String id = json.get("id").getAsString();
        JsonArray signalsJson = json.getAsJsonArray("signals");

        if (signalsJson == null || signalsJson.isEmpty()) {
            throw new IllegalArgumentException(
                    "Stateworks signal set must contain at least one signal"
            );
        }

        List<SignalDefinition> signals = new ArrayList<>();

        signalsJson.forEach(element -> {
            JsonObject signalJson = element.getAsJsonObject();
            String name = signalJson.get("name").getAsString();
            String type = signalJson.get("type").getAsString();

            if (!type.equals("double")) {
                throw new IllegalArgumentException(
                        "Unsupported Stateworks signal type: " + type
                );
            }

            String source = signalJson.get("source").getAsString();

            switch (source) {
                case "constant" -> {
                    double value = signalJson.get("value").getAsDouble();
                    signals.add(new SignalDefinition(name, context -> value));
                }
                case "property" -> {
                    String property = signalJson.get("property").getAsString();
                    signals.add(new SignalDefinition(name, context ->
                            readPropertySignal(context, property)
                    ));
                }
                default -> throw new IllegalArgumentException(
                        "Unsupported Stateworks signal source: " + source
                );
            }
        });

        return new SignalDefinitionSet(id, signals);
    }

    private static double readPropertySignal(
            StateContext context,
            String propertyName
    ) {
        var property = context.getBlockState()
                .getBlock()
                .getStateDefinition()
                .getProperty(propertyName);

        if (property == null) {
            throw new IllegalArgumentException(
                    "Unknown Stateworks signal property: " + propertyName
            );
        }

        Object value = context.getBlockState().getValue(property);

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        if (value instanceof Boolean bool) {
            return bool ? 1.0D : 0.0D;
        }

        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Stateworks signal property '" + propertyName
                            + "' is not numeric: " + value,
                    exception
            );
        }
    }

    public static StateDefinitionSet<?> parseSet(
            JsonObject json
    ) {
        JsonArray statesJson =
                json.getAsJsonArray("states");

        if (statesJson == null
                || statesJson.isEmpty()) {

            throw new IllegalArgumentException(
                    "Stateworks state set must contain "
                            + "at least one state"
            );
        }

        String firstType =
                statesJson.get(0)
                        .getAsJsonObject()
                        .get("type")
                        .getAsString();

        return switch (firstType) {
            case "boolean" ->
                    parseBooleanSet(json);

            case "double" ->
                    parseDoubleSet(json);

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported Stateworks state type: "
                                    + firstType
                    );
        };
    }

    private static StateDefinitionSet<Boolean>
    parseBooleanSet(
            JsonObject json
    ) {
        String id =
                json.get("id").getAsString();

        JsonArray statesJson =
                json.getAsJsonArray("states");

        List<StateDefinition<Boolean>> states =
                new ArrayList<>();

        statesJson.forEach(element -> {

            JsonObject stateJson =
                    element.getAsJsonObject();

            String name =
                    stateJson.get("name")
                            .getAsString();

            String type =
                    stateJson.get("type")
                            .getAsString();

            if (!type.equals("boolean")) {
                throw new IllegalArgumentException(
                        "Mixed state types are not allowed "
                                + "in Stateworks state set: "
                                + id
                );
            }

            Condition condition =
                    ConditionParser.parse(
                            stateJson.getAsJsonObject(
                                    "condition"
                            )
                    );

            String visualModel = null;
            if (stateJson.has("visual")) {
                JsonElement visual = stateJson.get("visual");
                visualModel = visual.isJsonPrimitive()
                        ? visual.getAsString()
                        : visual.getAsJsonObject().get("model").getAsString();
            }

            states.add(
                    new StateDefinition<>(
                            name,
                            condition::evaluate,
                            condition,
                            visualModel
                    )
            );
        });

        StateDefinition<Boolean> defaultState =
                findDefaultState(
                        json,
                        states
                );

        List<StateTransitionDefinition<Boolean>>
                transitions =
                parseTransitions(json);

        OutputDefinition output =
                parseOutput(json);

        return new StateDefinitionSet<>(
                id,
                states,
                defaultState,
                transitions,
                output
        );
    }

    private static StateDefinitionSet<Double>
    parseDoubleSet(
            JsonObject json
    ) {
        String id =
                json.get("id").getAsString();

        JsonArray statesJson =
                json.getAsJsonArray("states");

        List<StateDefinition<Double>> states =
                new ArrayList<>();

        statesJson.forEach(element -> {

            JsonObject stateJson =
                    element.getAsJsonObject();

            String name =
                    stateJson.get("name")
                            .getAsString();

            String type =
                    stateJson.get("type")
                            .getAsString();

            if (!type.equals("double")) {
                throw new IllegalArgumentException(
                        "Mixed state types are not allowed "
                                + "in Stateworks state set: "
                                + id
                );
            }

            if (!stateJson.has("value")) {
                throw new IllegalArgumentException(
                        "Double state requires 'value': "
                                + name
                );
            }

            double value =
                    stateJson.get("value")
                            .getAsDouble();

            Condition condition =
                    ConditionParser.parse(
                            stateJson.getAsJsonObject(
                                    "condition"
                            )
                    );

            String visualModel = null;
            if (stateJson.has("visual")) {
                JsonElement visual = stateJson.get("visual");
                visualModel = visual.isJsonPrimitive()
                        ? visual.getAsString()
                        : visual.getAsJsonObject().get("model").getAsString();
            }

            states.add(
                    new StateDefinition<>(
                            name,
                            context -> value,
                            condition,
                            visualModel
                    )
            );
        });

        StateDefinition<Double> defaultState =
                findDefaultState(
                        json,
                        states
                );

        List<StateTransitionDefinition<Double>>
                transitions =
                parseTransitions(json);

        OutputDefinition output =
                parseOutput(json);

        return new StateDefinitionSet<>(
                id,
                states,
                defaultState,
                transitions,
                output
        );
    }

    /**
     * Parses the optional output definition.
     *
     * Example:
     *
     * "output": {
     *     "type": "block_property"
     * }
     *
     * If no output is specified, this returns null.
     */
    private static OutputDefinition parseOutput(
            JsonObject json
    ) {
        if (!json.has("output")
                || json.get("output").isJsonNull()) {
            return null;
        }

        JsonObject outputJson =
                json.getAsJsonObject("output");

        if (!outputJson.has("type")) {
            throw new IllegalArgumentException(
                    "Stateworks output requires 'type'"
            );
        }

        String type =
                outputJson.get("type")
                        .getAsString();

        if (type.isBlank()) {
            throw new IllegalArgumentException(
                    "Stateworks output type cannot be blank"
            );
        }

        String property =
                outputJson.has("property")
                        ? outputJson.get("property")
                        .getAsString()
                        : "lit";

        List<Integer> offset =
                List.of(2, 0, 0);

        boolean relativeToFacing = true;

        if (outputJson.has("target")) {

            JsonObject targetJson =
                    outputJson.getAsJsonObject(
                            "target"
                    );

            if (targetJson.has("offset")) {

                JsonArray offsetJson =
                        targetJson.getAsJsonArray(
                                "offset"
                        );

                if (offsetJson.size() != 3) {
                    throw new IllegalArgumentException(
                            "Stateworks output target offset "
                                    + "must contain exactly three values"
                    );
                }

                offset =
                        List.of(
                                offsetJson.get(0).getAsInt(),
                                offsetJson.get(1).getAsInt(),
                                offsetJson.get(2).getAsInt()
                        );
            }

            if (targetJson.has(
                    "relative_to_facing"
            )) {
                relativeToFacing =
                        targetJson.get(
                                "relative_to_facing"
                        ).getAsBoolean();
            }
        }

        Map<String, Object> values =
                new java.util.HashMap<>();

        if (outputJson.has("values")) {

            JsonObject valuesJson =
                    outputJson.getAsJsonObject(
                            "values"
                    );

            valuesJson.entrySet()
                    .forEach(entry ->
                            values.put(
                                    entry.getKey(),
                                    parseOutputValue(
                                            entry.getValue()
                                    )
                            )
                    );
        }

        return new OutputDefinition(
                type,
                offset,
                relativeToFacing,
                property,
                values
        );
    }

    private static Object parseOutputValue(
            com.google.gson.JsonElement element
    ) {
        if (element.isJsonPrimitive()) {

            if (element.getAsJsonPrimitive()
                    .isBoolean()) {
                return element.getAsBoolean();
            }

            if (element.getAsJsonPrimitive()
                    .isNumber()) {
                return element.getAsDouble();
            }

            if (element.getAsJsonPrimitive()
                    .isString()) {
                return element.getAsString();
            }
        }

        throw new IllegalArgumentException(
                "Unsupported Stateworks output value: "
                        + element
        );
    }

    private static <T>
    StateDefinition<T> findDefaultState(
            JsonObject json,
            List<StateDefinition<T>> states
    ) {
        if (!json.has("default")) {
            return null;
        }

        String defaultName =
                json.get("default")
                        .getAsString();

        for (
                StateDefinition<T> state :
                states
        ) {
            if (state.name()
                    .equals(defaultName)) {

                return state;
            }
        }

        throw new IllegalArgumentException(
                "Unknown default Stateworks state: "
                        + defaultName
        );
    }

    private static TransitionDuration parseTransitionDuration(
            JsonElement element
    ) {
        if (element == null || element.isJsonNull()) {
            return TransitionDuration.fixed(0L);
        }

        if (element.isJsonPrimitive()) {
            return TransitionDuration.fixed(
                    element.getAsLong()
            );
        }

        if (!element.isJsonObject()) {
            throw new IllegalArgumentException(
                    "Transition duration must be a number or object"
            );
        }

        JsonObject duration = element.getAsJsonObject();
        String type = duration.has("type")
                ? duration.get("type").getAsString()
                : "fixed";

        if (type.equals("fixed")) {
            return TransitionDuration.fixed(
                    duration.get("milliseconds").getAsLong()
            );
        }

        if (type.equals("block_property")) {
            return TransitionDuration.blockProperty(
                    duration.get("property").getAsString(),
                    duration.get("scale_ms").getAsLong()
            );
        }

        throw new IllegalArgumentException(
                "Unsupported transition duration type: " + type
        );
    }

    private static <T>
    List<StateTransitionDefinition<T>>
    parseTransitions(
            JsonObject json
    ) {
        List<StateTransitionDefinition<T>>
                transitions =
                new ArrayList<>();

        if (!json.has("transitions")) {
            return transitions;
        }

        JsonArray transitionsJson =
                json.getAsJsonArray(
                        "transitions"
                );

        transitionsJson.forEach(element -> {

            JsonObject transitionJson =
                    element.getAsJsonObject();

            String from =
                    transitionJson.get("from")
                            .getAsString();

            String to =
                    transitionJson.get("to")
                            .getAsString();

            TransitionDuration duration =
                    parseTransitionDuration(
                            transitionJson.get("duration")
                    );

            Condition condition =
                    context -> true;

            if (transitionJson.has(
                    "condition"
            )) {
                condition =
                        ConditionParser.parse(
                                transitionJson
                                        .getAsJsonObject(
                                                "condition"
                                        )
                        );
            }

            boolean onComplete =
                    transitionJson.has(
                            "on_complete"
                    )
                            && transitionJson
                            .get("on_complete")
                            .getAsBoolean();

            transitions.add(
                    new StateTransitionDefinition<>(
                            from,
                            to,
                            condition,
                            duration,
                            onComplete
                    )
            );
        });

        return transitions;
    }
}