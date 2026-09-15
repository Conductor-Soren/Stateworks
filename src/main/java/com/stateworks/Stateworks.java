package com.stateworks;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;


import com.mojang.logging.LogUtils;
import com.stateworks.network.StateworksNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import net.minecraft.client.Minecraft;

@Mod(Stateworks.MODID)
public class Stateworks {

    public static final String MODID = "stateworks";


    private static final StateMachineManager STATE_MACHINES =
            new StateMachineManager();

    public static final StateRegistry STATES =
            StateRegistry.INSTANCE;

    public static final Logger LOGGER =
            LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MODID);

    public Stateworks(
            IEventBus modEventBus,
            ModContainer modContainer
    ) {

        modEventBus.addListener(
                this::commonSetup
        );

        /*
         * Register the Stateworks network payloads directly
         * with the mod event bus.
         *
         * RegisterPayloadHandlersEvent is a mod-bus event,
         * so it should not be registered through
         * @SubscribeEvent on this class.
         */
        modEventBus.addListener(
                StateworksNetwork::registerCommon
        );

        BLOCKS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        BuiltinConditions.register();
        BuiltinStateSets.register();
        modContainer.registerConfig(
                ModConfig.Type.COMMON,
                Config.SPEC
        );
    }

    private void commonSetup(
            FMLCommonSetupEvent event
    ) {

        LOGGER.info(
                "HELLO FROM COMMON SETUP"
        );

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info(
                    "DIRT BLOCK >> {}",
                    BuiltInRegistries.BLOCK.getKey(
                            Blocks.DIRT
                    )
            );
        }

        LOGGER.info(
                "{}{}",
                Config.MAGIC_NUMBER_INTRODUCTION.get(),
                Config.MAGIC_NUMBER.getAsInt()
        );

        Config.ITEM_STRINGS.get().forEach(
                item ->
                        LOGGER.info(
                                "ITEM >> {}",
                                item
                        )
        );
    }

    @SubscribeEvent
    public void onServerStarting(
            ServerStartingEvent event
    ) {

        LOGGER.info(
                "HELLO from server starting"
        );
    }

    @SubscribeEvent
    public void onServerTick(
            ServerTickEvent.Post event
    ) {

        STATE_MACHINES.tick(
                System.currentTimeMillis()
        );
    }

    @SubscribeEvent
    public void onServerStopping(
            ServerStoppingEvent event
    ) {
        /*
         * StateMachineManager is static and therefore survives the
         * integrated server instance. Drop every machine before the
         * old ServerLevel objects become invalid.
         */
        STATE_MACHINES.clear();
    }

    /**
     * Runs the Stateworks inspector against the block
     * currently being looked at by the player.
     */
    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        trackBlock(level, event.getPos());
    }

    @SubscribeEvent
    public void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        /*
         * The block that generated the notification may itself have changed
         * state (for example a Furnace changing LIT). Track that block so
         * ordinary Stateworks definitions can observe the new vanilla state
         * through their normal evaluator.
         *
         * Repeaters are additionally tracked on their input side because
         * their POWERED property intentionally changes later than the input
         * signal. The repeater-specific edge logic remains entirely inside
         * StateMachineManager.
         */
        trackBlock(level, event.getPos());

        for (Direction direction : event.getNotifiedSides()) {
            BlockPos neighborPos = event.getPos().relative(direction);

            if (level.getBlockState(neighborPos).getBlock()
                    == Blocks.REPEATER) {
                trackBlock(level, neighborPos);
            }
        }
    }

    /*
     * There is no BlockEvent.BreakEvent in the NeoForge 26.1 API used by
     * this project. We do not need a dedicated break hook anyway: the
     * StateMachineManager already removes a machine on its next tick when
     * StateRegistry.applies(...) becomes false because the repeater is gone.
     */

    private static void trackBlock(
            net.minecraft.world.level.Level level,
            BlockPos pos
    ) {
        if (level == null || pos == null || level.isClientSide()) {
            return;
        }

        STATE_MACHINES.update(
                level,
                pos,
                System.currentTimeMillis()
        );
    }

    public static void updateTrackedBlock(
            net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos
    ) {
        if (level == null || pos == null || level.isClientSide()) {
            return;
        }

        STATE_MACHINES.update(
                level,
                pos,
                System.currentTimeMillis()
        );
    }

    public static void inspectBlock(
            Minecraft minecraft,
            net.minecraft.core.BlockPos pos
    ) {

        if (minecraft.level == null
                || minecraft.player == null) {
            return;
        }

        BlockState state =
                minecraft.level.getBlockState(pos);

        StateInspector.inspect(
                state,
                minecraft.player
        );

        StateInspector.inspectPossibleStates(
                state,
                minecraft.player
        );

        StateContext context =
                new StateContext(
                        minecraft.level,
                        pos
                );

        long currentTime =
                System.currentTimeMillis();

        StateMachine machine =
                STATE_MACHINES.update(
                        minecraft.level,
                        pos,
                        currentTime
                );

        /*
         * A block that does not match the configured Stateworks
         * state set does not get a machine.
         */
        if (machine == null) {

            System.out.println(
                    "Virtual State: <not applicable>"
            );

            System.out.println(
                    "========== STATEWORKS MACHINE =========="
            );

            System.out.println(
                    "Stateworks Machine: <not applicable>"
            );

            System.out.println(
                    "Tracked Machines: "
                            + STATE_MACHINES.size()
            );

            System.out.println(
                    "========================================"
            );

        } else {

            VirtualState<?> result =
                    machine.desiredState();

            if (result != null) {

                StateVisual visual =
                        STATES.getSet(machine.stateSetId()) != null
                                ? STATES.getSet(machine.stateSetId()).visual(result.name())
                                : null;

                System.out.println(
                        "Visual Model: "
                                + (
                                visual != null
                                        ? visual.model()
                                        : "<none>"
                        )
                );

                System.out.println(
                        "Virtual State: "
                                + result.name()
                                + " = "
                                + result.value()
                );

            } else {

                System.out.println(
                        "Virtual State: <not applicable>"
                );
            }

            System.out.println(
                    "========== STATEWORKS MACHINE =========="
            );

            System.out.println(
                    "Dimension: "
                            + minecraft.level.dimension()
            );

            System.out.println(
                    "Position: "
                            + pos
            );

            System.out.println(
                    "Tracked Machines: "
                            + STATE_MACHINES.size()
            );

            System.out.println(
                    "========================================"
            );

            StateTransition transition =
                    machine.transition();

            if (transition != null) {

                System.out.println(
                        "========== STATEWORKS TRANSITION =========="
                );

                System.out.println(
                        "Previous: "
                                + transition.previous().name()
                                + " = "
                                + transition.previous().value()
                );

                System.out.println(
                        "Next: "
                                + transition.next().name()
                                + " = "
                                + transition.next().value()
                );

                System.out.println(
                        "Progress: "
                                + transition.getProgress(
                                currentTime
                        )
                );

                System.out.println(
                        "Complete: "
                                + transition.isComplete(
                                currentTime
                        )
                );

                System.out.println(
                        "=========================================="
                );

                Object interpolated =
                        machine.interpolatedValue(
                                currentTime
                        );

                System.out.println(
                        "Interpolated: "
                                + interpolated
                                + " | progress="
                                + transition.getProgress(
                                currentTime
                        )
                );
            }
        }

        System.out.println(
                "========== STATEWORKS CONTEXT =========="
        );

        System.out.println(
                "Block: "
                        + context.getBlockState().getBlock()
        );

        System.out.println(
                "========================================"
        );

        System.out.println(
                "========== STATEWORKS OUTPUT =========="
        );

        if (machine == null) {

            System.out.println(
                    "Output State: <not applicable>"
            );

            System.out.println(
                    "Output Value: <not applicable>"
            );

            System.out.println(
                    "Output Active: false"
            );

        } else {

            StateOutput output =
                    machine.output();

            VirtualState<?> outputState =
                    output.state(currentTime);

            Object outputValue =
                    output.mappedValue(currentTime);

            boolean active =
                    output.isActive(currentTime);

            System.out.println(
                    "Output State: "
                            + (
                            outputState != null
                                    ? outputState.name()
                                    : "null"
                    )
            );

            System.out.println(
                    "Output Value: "
                            + outputValue
            );

            System.out.println(
                    "Output Active: "
                            + active
            );
        }

        System.out.println(
                "======================================="
        );
    }
}
