package com.stateworks.condition;

import com.google.gson.JsonArray;

import java.util.ArrayList;
import java.util.List;

public final class BuiltinConditions {

    private BuiltinConditions() {
    }

    public static void register() {

        ConditionParser.register("property_equals", json -> {

            String property =
                    json.get("property").getAsString();

            String value =
                    json.get("value").getAsString();

            return new PropertyEqualsCondition(
                    property,
                    value
            );
        });

        ConditionParser.register("property_in", json -> {

            String property =
                    json.get("property").getAsString();

            List<String> values =
                    new ArrayList<>();

            for (var element :
                    json.getAsJsonArray("values")) {

                values.add(element.getAsString());
            }

            return new PropertyInCondition(
                    property,
                    values
            );
        });

        ConditionParser.register("block_tag", json -> {

            return new BlockTagCondition(
                    json.get("tag").getAsString()
            );
        });


        ConditionParser.register("block", json -> {

            return new BlockCondition(
                    json.get("block").getAsString()
            );
        });


        ConditionParser.register("all", json -> {

            JsonArray conditionsJson =
                    json.getAsJsonArray("conditions");

            List<Condition> conditions = new ArrayList<>();

            for (var element : conditionsJson) {
                conditions.add(
                        ConditionParser.parse(
                                element.getAsJsonObject()
                        )
                );
            }

            return new CompositeCondition(
                    CompositeCondition.Mode.ALL,
                    conditions
            );
        });

        ConditionParser.register("any", json -> {

            JsonArray conditionsJson =
                    json.getAsJsonArray("conditions");

            List<Condition> conditions = new ArrayList<>();

            for (var element : conditionsJson) {
                conditions.add(
                        ConditionParser.parse(
                                element.getAsJsonObject()
                        )
                );
            }

            return new CompositeCondition(
                    CompositeCondition.Mode.ANY,
                    conditions
            );
        });

        ConditionParser.register("not", json -> {

            Condition condition =
                    ConditionParser.parse(
                            json.getAsJsonObject("condition")
                    );

            return new NotCondition(condition);
        });
    }
}