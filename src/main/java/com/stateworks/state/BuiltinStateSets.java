package com.stateworks.state;

import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import com.stateworks.condition.Condition;
import com.stateworks.transition.StateTransitionDefinition;
import com.stateworks.transition.TransitionDuration;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Built-in Stateworks state sets and simple Java registration helpers. */
public final class BuiltinStateSets {

    public static final String REPEATER_POWER = "example:repeater_power";
    public static final String FURNACE_HEAT = "example:furnace_heat";
    public static final String REDSTONE_POWER = "example:redstone_power";

    private BuiltinStateSets() {}

    public static void register() {
        registerRepeater(Blocks.REPEATER);
        registerRedstoneDust(REDSTONE_POWER, Blocks.REDSTONE_WIRE);

        /* Furnaces */
        registerBooleanBlocks(
                "",
                FurnaceBlock.LIT,
                "cold", "hot", "Heating", "Cooling", 4L,
                Blocks.FURNACE,
                Blocks.BLAST_FURNACE,
                Blocks.SMOKER
        );

        /* Buttons */
        registerBooleanBlocks(
                "_pressed",
                ButtonBlock.POWERED,
                "off", "on", "Pressed", "Unpressed", 4L,
                Blocks.OAK_BUTTON,
                Blocks.SPRUCE_BUTTON,
                Blocks.BIRCH_BUTTON,
                Blocks.JUNGLE_BUTTON,
                Blocks.ACACIA_BUTTON,
                Blocks.DARK_OAK_BUTTON,
                Blocks.MANGROVE_BUTTON,
                Blocks.CHERRY_BUTTON,
                Blocks.PALE_OAK_BUTTON,
                Blocks.BAMBOO_BUTTON,
                Blocks.CRIMSON_BUTTON,
                Blocks.WARPED_BUTTON,
                Blocks.STONE_BUTTON,
                Blocks.POLISHED_BLACKSTONE_BUTTON
        );

        /* Saplings */
        registerPropertyStagesBlocks(
                "",
                SaplingBlock.STAGE,
                0,
                1,
                4L,
                Blocks.OAK_SAPLING,
                Blocks.SPRUCE_SAPLING,
                Blocks.BIRCH_SAPLING,
                Blocks.JUNGLE_SAPLING,
                Blocks.ACACIA_SAPLING,
                Blocks.DARK_OAK_SAPLING,
                Blocks.CHERRY_SAPLING,
                Blocks.PALE_OAK_SAPLING
        );

        /* Doors */
        registerBooleanBlocks(
                "",
                DoorBlock.OPEN,
                "open", "closed", "opening", "closing", 4L,
                Blocks.OAK_DOOR,
                Blocks.SPRUCE_DOOR,
                Blocks.BIRCH_DOOR,
                Blocks.JUNGLE_DOOR,
                Blocks.ACACIA_DOOR,
                Blocks.DARK_OAK_DOOR,
                Blocks.MANGROVE_DOOR,
                Blocks.CHERRY_DOOR,
                Blocks.PALE_OAK_DOOR,
                Blocks.BAMBOO_DOOR,
                Blocks.CRIMSON_DOOR,
                Blocks.WARPED_DOOR,
                Blocks.IRON_DOOR
        );
        registerBooleanBlocks(
                "",
                DoorBlock.OPEN,
                "open", "closed", "opening", "closing", 4L,
                Blocks.COPPER_DOOR.asList().toArray(new Block[0])
        );

        /* TrapDoors */
        registerBooleanBlocks(
                "",
                TrapDoorBlock.OPEN,
                "open", "closed", "opening", "closing", 4L,
                Blocks.OAK_TRAPDOOR,
                Blocks.SPRUCE_TRAPDOOR,
                Blocks.BIRCH_TRAPDOOR,
                Blocks.JUNGLE_TRAPDOOR,
                Blocks.ACACIA_TRAPDOOR,
                Blocks.DARK_OAK_TRAPDOOR,
                Blocks.MANGROVE_TRAPDOOR,
                Blocks.CHERRY_TRAPDOOR,
                Blocks.PALE_OAK_TRAPDOOR,
                Blocks.BAMBOO_TRAPDOOR,
                Blocks.CRIMSON_TRAPDOOR,
                Blocks.WARPED_TRAPDOOR,
                Blocks.IRON_TRAPDOOR
        );
        registerBooleanBlocks(
                "",
                TrapDoorBlock.OPEN,
                "open", "closed", "opening", "closing", 4L,
                Blocks.COPPER_TRAPDOOR.asList().toArray(new Block[0])
        );

        /* Crops */

        /* Bamboo */
        registerBamboo(Blocks.BAMBOO);

        /* 0-7 Crops */
        registerPropertyStagesBlocks(
                "",
                CropBlock.AGE,
                0,
                7,
                4L,
                Blocks.WHEAT,
                Blocks.CARROTS,
                Blocks.POTATOES,
                Blocks.MELON_STEM,
                Blocks.PUMPKIN_STEM
        );
        /* 0-15 Crops */
        registerPropertyStagesBlocks(
                "",
                CropBlock.AGE,
                0,
                15,
                4L,
                Blocks.SUGAR_CANE,
                Blocks.CACTUS
        );
        registerPropertyStages(
                "example:torchflower",
                Blocks.TORCHFLOWER,
                CropBlock.AGE,
                0,
                7,
                4L
        );
    }

    /**
     * Registers the same boolean transition system for multiple blocks.
     * Each block receives an ID of example:<block_path><idSuffix>.
     */
    public static void registerBooleanBlocks(
            String idSuffix,
            BooleanProperty property,
            String offState,
            String onState,
            String turningOnTransition,
            String turningOffTransition,
            long transitionTicks,
            Block... blocks
    ) {
        for (Block block : blocks) {
            String id = "example:" + BuiltInRegistries.BLOCK.getKey(block).getPath() + idSuffix;
            registerBooleanBlock(
                    id,
                    block,
                    property,
                    offState,
                    onState,
                    turningOnTransition,
                    turningOffTransition,
                    transitionTicks
            );
        }
    }

    /**
     * Registers the same integer-property stage system for multiple blocks.
     * Each block receives an ID of example:<block_path><idSuffix>.
     */
    public static void registerPropertyStagesBlocks(
            String idSuffix,
            IntegerProperty property,
            int minValue,
            int maxValue,
            long transitionTicks,
            Block... blocks
    ) {
        for (Block block : blocks) {
            String id = "example:" + BuiltInRegistries.BLOCK.getKey(block).getPath() + idSuffix;
            registerPropertyStages(
                    id,
                    block,
                    property,
                    minValue,
                    maxValue,
                    transitionTicks
            );
        }
    }

    /** Registers a simple two-state block driven by a BooleanProperty. */
    public static void registerBooleanBlock(
            String id,
            Block block,
            BooleanProperty property,
            String offState,
            String onState,
            String turningOnTransition,
            String turningOffTransition,
            long transitionTicks
    ) {
        Condition applies = context -> context.getBlockState().getBlock() == block;
        Condition offCondition = context -> applies.evaluate(context)
                && !context.getBlockState().getValue(property);
        Condition onCondition = context -> applies.evaluate(context)
                && context.getBlockState().getValue(property);

        StateDefinition<Boolean> off = new StateDefinition<>(offState, context -> false, offCondition, null);
        StateDefinition<Boolean> on = new StateDefinition<>(onState, context -> true, onCondition, null);

        StateTransitionDefinition<Boolean> onTransition = new StateTransitionDefinition<>(
                offState, onState, context -> true,
                TransitionDuration.fixedTicks(transitionTicks), false,
                turningOnTransition
        );
        StateTransitionDefinition<Boolean> offTransition = new StateTransitionDefinition<>(
                onState, offState, context -> true,
                TransitionDuration.fixedTicks(transitionTicks), false,
                turningOffTransition
        );

        StateRegistry.INSTANCE.registerSet(new StateDefinitionSet<>(
                id, List.of(off, on), off, List.of(onTransition, offTransition), null
        ));
    }



    /**
     * Registers a block driven by an integer property as visual stages.
     * Creates stage_0..stage_N and adjacent transitions in both directions.
     */
    public static void registerPropertyStages(
            String id,
            Block block,
            IntegerProperty property,
            int minValue,
            int maxValue,
            long transitionTicks
    ) {
        Condition applies = context -> context.getBlockState().getBlock() == block;

        List<StateDefinition<Integer>> states = new java.util.ArrayList<>();
        List<StateTransitionDefinition<Integer>> transitions =
                new java.util.ArrayList<>();

        for (int value = minValue; value <= maxValue; value++) {
            final int stage = value;

            Condition condition = context -> applies.evaluate(context)
                    && context.getBlockState().getValue(property) == stage;

            states.add(new StateDefinition<>(
                    "stage_" + stage,
                    context -> stage,
                    condition,
                    null
            ));
        }

        for (int value = minValue; value < maxValue; value++) {
            String current = "stage_" + value;
            String next = "stage_" + (value + 1);

            transitions.add(new StateTransitionDefinition<>(
                    current, next, context -> true,
                    TransitionDuration.fixedTicks(transitionTicks), false
            ));
            transitions.add(new StateTransitionDefinition<>(
                    next, current, context -> true,
                    TransitionDuration.fixedTicks(transitionTicks), false
            ));
        }

        StateRegistry.INSTANCE.registerSet(new StateDefinitionSet<>(
                id,
                states,
                states.get(0),
                transitions,
                null
        ));
    }

    /**
     * Registers bamboo using its AGE, LEAVES, and STAGE properties.
     * Creates a visual state for every valid property combination and
     * transitions when one bamboo property changes at a time.
     */
    public static void registerBamboo(Block bamboo) {
        Condition applies = context -> context.getBlockState().getBlock() == bamboo;

        List<StateDefinition<String>> states = new java.util.ArrayList<>();
        List<StateTransitionDefinition<String>> transitions =
                new java.util.ArrayList<>();

        // Bamboo has AGE 0-1, LEAVES NONE/SMALL/LARGE, and STAGE 0-1.
        for (int age = 0; age <= 1; age++) {
            final int currentAge = age;
            for (BambooLeaves leaves : BambooLeaves.values()) {
                final BambooLeaves currentLeaves = leaves;
                for (int stage = 0; stage <= 1; stage++) {
                    final int currentStage = stage;
                    String name = bambooStateName(
                            currentAge, currentLeaves, currentStage
                    );

                    Condition condition = context -> applies.evaluate(context)
                            && context.getBlockState().getValue(BambooStalkBlock.AGE) == currentAge
                            && context.getBlockState().getValue(BambooStalkBlock.LEAVES) == currentLeaves
                            && context.getBlockState().getValue(BambooStalkBlock.STAGE) == currentStage;

                    states.add(new StateDefinition<>(
                            name,
                            context -> name,
                            condition,
                            null
                    ));
                }
            }
        }

        // AGE: 0 <-> 1, preserving LEAVES and STAGE.
        for (int age = 0; age <= 1; age++) {
            int nextAge = 1 - age;
            for (BambooLeaves leaves : BambooLeaves.values()) {
                for (int stage = 0; stage <= 1; stage++) {
                    transitions.add(new StateTransitionDefinition<>(
                            bambooStateName(age, leaves, stage),
                            bambooStateName(nextAge, leaves, stage),
                            context -> true,
                            TransitionDuration.fixedTicks(4L),
                            false
                    ));
                }
            }
        }

        // STAGE: 0 <-> 1, preserving AGE and LEAVES.
        for (int stage = 0; stage <= 1; stage++) {
            int nextStage = 1 - stage;
            for (int age = 0; age <= 1; age++) {
                for (BambooLeaves leaves : BambooLeaves.values()) {
                    transitions.add(new StateTransitionDefinition<>(
                            bambooStateName(age, leaves, stage),
                            bambooStateName(age, leaves, nextStage),
                            context -> true,
                            TransitionDuration.fixedTicks(4L),
                            false
                    ));
                }
            }
        }

        // LEAVES: allow every direct change between NONE, SMALL, and LARGE.
        for (int age = 0; age <= 1; age++) {
            for (int stage = 0; stage <= 1; stage++) {
                for (BambooLeaves from : BambooLeaves.values()) {
                    for (BambooLeaves to : BambooLeaves.values()) {
                        if (from == to) {
                            continue;
                        }

                        transitions.add(new StateTransitionDefinition<>(
                                bambooStateName(age, from, stage),
                                bambooStateName(age, to, stage),
                                context -> true,
                                TransitionDuration.fixedTicks(4L),
                                false
                        ));
                    }
                }
            }
        }

        String defaultState = bambooStateName(
                0, BambooLeaves.NONE, 0
        );
        StateDefinition<String> defaultDefinition = states.stream()
                .filter(state -> state.name().equals(defaultState))
                .findFirst()
                .orElse(states.get(0));

        StateRegistry.INSTANCE.registerSet(new StateDefinitionSet<>(
                "example:bamboo",
                states,
                defaultDefinition,
                transitions,
                null
        ));
    }

    private static String bambooStateName(
            int age,
            BambooLeaves leaves,
            int stage
    ) {
        return "age_" + age
                + "_leaves_" + leaves.name().toLowerCase(java.util.Locale.ROOT)
                + "_stage_" + stage;
    }

    /**
     * Registers redstone dust POWER (0-15) as visual stages.
     * Each adjacent strength change uses a quick 1-tick visual transition.
     */
    public static void registerRedstoneDust(String id, Block redstone) {
        Condition applies =
                context -> context.getBlockState().getBlock() == redstone;

        List<StateDefinition<Integer>> states = new java.util.ArrayList<>();
        List<StateTransitionDefinition<Integer>> transitions =
                new java.util.ArrayList<>();

        for (int power = 0; power <= 15; power++) {
            final int strength = power;

            Condition condition =
                    context -> applies.evaluate(context)
                            && context.getBlockState().getValue(
                            net.minecraft.world.level.block.RedStoneWireBlock.POWER
                    ) == strength;

            states.add(new StateDefinition<>(
                    "stage_" + strength,
                    context -> strength,
                    condition,
                    null
            ));
        }

        for (int power = 0; power < 15; power++) {
            String current = "stage_" + power;
            String next = "stage_" + (power + 1);

            transitions.add(new StateTransitionDefinition<>(
                    current, next, context -> true,
                    TransitionDuration.fixedTicks(1L), false
            ));
            transitions.add(new StateTransitionDefinition<>(
                    next, current, context -> true,
                    TransitionDuration.fixedTicks(1L), false
            ));
        }

        StateRegistry.INSTANCE.registerSet(new StateDefinitionSet<>(
                id,
                states,
                states.get(0),
                transitions,
                null
        ));
    }

    /**
     * Registers the repeater using its vanilla 1-4 tick DELAY property.
     * Resource-pack visuals can use Powering_1..4 and PoweringOff_1..4.
     */
    public static void registerRepeater(Block repeater) {
        Condition applies = context -> context.getBlockState().getBlock() == repeater;

        /*
         * A repeater's POWERED property changes only after vanilla's delay.
         * Stateworks needs the visual transition to represent that delay, so
         * the desired state must be based on the signal entering the repeater,
         * not on the already-delayed POWERED property.
         *
         * FACING points from input -> output. Therefore the input block is on
         * FACING.getOpposite(), and its signal enters the repeater along FACING.
         */
        Condition onCondition = context -> {
            if (!applies.evaluate(context)) {
                return false;
            }

            BlockState state = context.getBlockState();
            Direction facing = state.getValue(RepeaterBlock.FACING);

            /*
             * FACING is the direction used by the repeater's redstone
             * connection. The source we care about is the block on that side.
             * Query that neighbor for the signal it sends back toward the
             * repeater. POWERED is never consulted.
             */
            BlockPos sourcePos =
                    context.getPos().relative(facing);

            return context.getLevel().getSignal(
                    sourcePos,
                    facing.getOpposite()
            ) > 0;
        };

        Condition offCondition = context -> {
            if (!applies.evaluate(context)) {
                return false;
            }

            BlockState state = context.getBlockState();
            Direction facing = state.getValue(RepeaterBlock.FACING);
            BlockPos sourcePos =
                    context.getPos().relative(facing);

            return context.getLevel().getSignal(
                    sourcePos,
                    facing.getOpposite()
            ) <= 0;
        };

        StateDefinition<Boolean> off = new StateDefinition<>("off", context -> false, offCondition, null);
        StateDefinition<Boolean> on = new StateDefinition<>("active", context -> true, onCondition, null);

        StateTransitionDefinition<Boolean> powering = new StateTransitionDefinition<>(
                "off", "active", context -> true,
                TransitionDuration.blockPropertyTicks("delay", 2L), false,
                "Powering"
        );
        StateTransitionDefinition<Boolean> poweringOff = new StateTransitionDefinition<>(
                "active", "off", context -> true,
                TransitionDuration.blockPropertyTicks("delay", 2L), false,
                "PoweringOff"
        );

        StateRegistry.INSTANCE.registerSet(new StateDefinitionSet<>(
                REPEATER_POWER, List.of(off, on), off, List.of(powering, poweringOff), null
        ));
    }
}
