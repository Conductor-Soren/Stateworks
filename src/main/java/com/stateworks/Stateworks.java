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
import com.stateworks.block.StateworksTestBlock;
import com.stateworks.network.StateworksNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
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
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import net.minecraft.client.Minecraft;

@Mod(Stateworks.MODID)
public class Stateworks {

    public static final String MODID = "stateworks";

    private static final String TEST_STATE =
            "example:repeater_power";

    private static final String TEST_BLOCK_STATE =
            "example:test_block";

    private static final StateMachineManager STATE_MACHINES =
            new StateMachineManager();

    public static final StateRegistry STATES =
            StateRegistry.INSTANCE;

    public static final Logger LOGGER =
            LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MODID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    MODID
            );

    /*
     * The block must be registered through registerBlock().
     *
     * NeoForge 26.1 requires the block's ResourceKey to be
     * assigned to BlockBehaviour.Properties before the Block
     * constructor runs. registerBlock() handles that for us.
     */
    public static final DeferredBlock<StateworksTestBlock> STATEWORKS_BLOCK =
            BLOCKS.registerBlock(
                    "stateworks_block",
                    StateworksTestBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
            );

    public static final DeferredItem<BlockItem> STATEWORKS_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(
                    "stateworks_block",
                    STATEWORKS_BLOCK
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB =
            CREATIVE_MODE_TABS.register(
                    "example_tab",
                    () -> CreativeModeTab.builder()
                            .title(
                                    Component.translatable(
                                            "itemGroup.stateworks"
                                    )
                            )
                            .withTabsBefore(
                                    CreativeModeTabs.COMBAT
                            )
                            .icon(
                                    () -> STATEWORKS_BLOCK_ITEM
                                            .get()
                                            .getDefaultInstance()
                            )
                            .displayItems(
                                    (parameters, output) ->
                                            output.accept(
                                                    STATEWORKS_BLOCK_ITEM
                                            )
                            )
                            .build()
            );

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
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        BuiltinConditions.register();

        modEventBus.addListener(
                this::addCreative
        );

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

    private void addCreative(
            BuildCreativeModeTabContentsEvent event
    ) {

        if (event.getTabKey()
                == CreativeModeTabs.BUILDING_BLOCKS) {

            event.accept(
                    STATEWORKS_BLOCK_ITEM
            );
        }
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

        if (event.getPlacedBlock().getBlock() instanceof RepeaterBlock) {
            trackRepeater(level, event.getPos());
        }
    }

    @SubscribeEvent
    public void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // The event identifies the block whose neighbors are about to be
        // notified. Any repeater on those notified positions may have had
        // its input changed, so make sure Stateworks is watching it.
        if (event.getState().getBlock() instanceof RepeaterBlock) {
            trackRepeater(level, event.getPos());
        }

        for (Direction direction : event.getNotifiedSides()) {
            BlockPos neighbor = event.getPos().relative(direction);
            if (level.getBlockState(neighbor).getBlock() instanceof RepeaterBlock) {
                trackRepeater(level, neighbor);
            }
        }
    }

    /*
     * There is no BlockEvent.BreakEvent in the NeoForge 26.1 API used by
     * this project. We do not need a dedicated break hook anyway: the
     * StateMachineManager already removes a machine on its next tick when
     * StateRegistry.applies(...) becomes false because the repeater is gone.
     */

    private static void trackRepeater(
            net.minecraft.world.level.Level level,
            BlockPos pos
    ) {
        if (level == null || level.isClientSide()) {
            return;
        }

        if (!(level.getBlockState(pos).getBlock() instanceof RepeaterBlock)) {
            return;
        }

        STATE_MACHINES.update(
                level,
                pos,
                TEST_STATE,
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
                TEST_BLOCK_STATE,
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
                        TEST_BLOCK_STATE,
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
                        + context
                        .getBlockState()
                        .getBlock()
        );

        for (Direction direction :
                Direction.values()) {

            System.out.println(
                    direction.getName()
                            + ": "
                            + context
                            .getNeighbor(direction)
                            .getBlock()
                            + " | neighbor="
                            + context.hasNeighbor(
                            direction
                    )
                            + " | solid="
                            + context.isSolid(
                            direction
                    )
                            + " | air="
                            + context.isAir(
                            direction
                    )
            );
        }

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