package com.stateworks.condition;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;
import com.stateworks.block.*;

import com.google.gson.JsonArray;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;

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

            Identifier tagId = Identifier.parse(
                    json.get("tag").getAsString()
            );

            TagKey<Block> tag = TagKey.create(
                    Registries.BLOCK,
                    tagId
            );

            return new BlockTagCondition(tag);
        });

        ConditionParser.register("neighbor_tag", json -> {

            Direction direction = Direction.byName(
                    json.get("direction").getAsString()
            );

            if (direction == null) {
                throw new IllegalArgumentException(
                        "Invalid direction in neighbor_tag condition"
                );
            }

            Identifier tagId = Identifier.parse(
                    json.get("tag").getAsString()
            );

            TagKey<Block> tag = TagKey.create(
                    Registries.BLOCK,
                    tagId
            );

            return new NeighborTagCondition(
                    direction,
                    tag
            );
        });

        ConditionParser.register("neighbor_solid", json -> {

            Direction direction = Direction.byName(
                    json.get("direction").getAsString()
            );

            if (direction == null) {
                throw new IllegalArgumentException(
                        "Invalid direction in neighbor_solid condition"
                );
            }

            return new NeighborSolidCondition(direction);
        });

        ConditionParser.register("block", json -> {

            Identifier blockId = Identifier.parse(
                    json.get("block").getAsString()
            );

            Block block = BuiltInRegistries.BLOCK.get(blockId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unknown block in block condition: " + blockId
                    ))
                    .value();

            return new BlockCondition(block);
        });

        ConditionParser.register("neighbor_block", json -> {

            Direction direction = Direction.byName(
                    json.get("direction").getAsString()
            );

            if (direction == null) {
                throw new IllegalArgumentException(
                        "Invalid direction in neighbor_block condition"
                );
            }

            Identifier blockId = Identifier.parse(
                    json.get("block").getAsString()
            );

            Block block = BuiltInRegistries.BLOCK.get(blockId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unknown block in neighbor_block condition: " + blockId
                    ))
                    .value();

            return new NeighborBlockCondition(direction, block);
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