package com.stateworks.client;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

public final class StateInspector {

    private StateInspector() {
        // Utility class — no instances needed.
    }

    public static void inspect(BlockState state, Player player) {
        {
            System.out.println("========== STATEWORKS INSPECTOR ==========");
            System.out.println("Block: " + state.getBlock());

            for (Property<?> property : state.getProperties()) {
                System.out.println(
                        "Property: " + property.getName()
                                + " = " + state.getValue(property)
                );
            }

            player.sendSystemMessage(Component.literal("=== Stateworks Inspector ==="));
            player.sendSystemMessage(Component.literal("Block: " + state.getBlock()));

            for (Property<?> property : state.getProperties()) {
                player.sendSystemMessage(
                        Component.literal(
                                property.getName() + " = " + state.getValue(property)
                        )
                );
            }

            System.out.println("==========================================");
        }
    }

    public static void inspectPossibleStates(BlockState state, Player player) {
        player.sendSystemMessage(Component.literal("=== Possible Values ==="));

        for (Property<?> property : state.getProperties()) {
            String values = property.getPossibleValues()
                    .stream()
                    .map(Object::toString)
                    .collect(java.util.stream.Collectors.joining(", "));

            player.sendSystemMessage(
                    Component.literal(property.getName() + ": " + values)
            );
        }
    }
}