package com.stateworks.condition;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class ConditionParser {

    private static final Map<String, Function<JsonObject, Condition>> CONDITIONS =
            new HashMap<>();

    private ConditionParser() {
    }

    public static Condition parse(JsonObject json) {

        if (json == null) {
            throw new IllegalArgumentException(
                    "Stateworks condition cannot be null"
            );
        }

        /*
         * Preferred format:
         *
         * {
         *   "type": "property_equals",
         *   "property": "delay",
         *   "value": 1
         * }
         */
        if (json.has("type")) {

            String type =
                    json.get("type").getAsString();

            Function<JsonObject, Condition> factory =
                    CONDITIONS.get(type);

            if (factory == null) {
                throw new IllegalArgumentException(
                        "Unknown Stateworks condition type: " + type
                );
            }

            return factory.apply(json);
        }

        /*
         * Legacy/nested format:
         *
         * {
         *   "property_equals": {
         *     "property": "delay",
         *     "value": 1
         *   }
         * }
         *
         * Normalize it into the preferred format.
         */
        if (json.entrySet().size() == 1) {

            Map.Entry<String, com.google.gson.JsonElement> entry =
                    json.entrySet().iterator().next();

            String type =
                    entry.getKey();

            if (!entry.getValue().isJsonObject()) {
                throw new IllegalArgumentException(
                        "Stateworks condition '" + type
                                + "' must contain an object"
                );
            }

            Function<JsonObject, Condition> factory =
                    CONDITIONS.get(type);

            if (factory == null) {
                throw new IllegalArgumentException(
                        "Unknown Stateworks condition type: " + type
                );
            }

            JsonObject normalized =
                    entry.getValue().getAsJsonObject().deepCopy();

            normalized.addProperty("type", type);

            return factory.apply(normalized);
        }

        throw new IllegalArgumentException(
                "Stateworks condition must contain a 'type' "
                        + "or use the nested condition format"
        );
    }

    public static void register(
            String type,
            Function<JsonObject, Condition> factory
    ) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException(
                    "Stateworks condition type cannot be null or blank"
            );
        }

        if (factory == null) {
            throw new IllegalArgumentException(
                    "Stateworks condition factory cannot be null"
            );
        }

        CONDITIONS.put(type, factory);
    }
}