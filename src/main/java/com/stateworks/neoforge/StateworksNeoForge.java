
package com.stateworks.neoforge;

import com.stateworks.neoforge.event.StateworksNeoForgeServerEvents;
import com.stateworks.minecraft.state.BuiltinStateSets;
import com.stateworks.minecraft.transition.MinecraftStateMachineService;
import com.stateworks.core.registry.StateRegistry;
import com.stateworks.core.StateworksConstants;
import com.stateworks.condition.BuiltinConditions;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.BlockPos;
import com.stateworks.neoforge.config.NeoForgeConfig;
import com.stateworks.neoforge.network.StateworksNetwork;
import org.slf4j.Logger;

@Mod(StateworksNeoForge.MODID)
public class StateworksNeoForge {

    public static final String MODID = StateworksConstants.MOD_ID;

    public static final StateRegistry STATES =
            StateRegistry.INSTANCE;

    public static final Logger LOGGER =
            LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MODID);

    public StateworksNeoForge(
            IEventBus modEventBus,
            ModContainer modContainer
    ) {

        modEventBus.addListener(
                this::commonSetup
        );

        // Networking is registered by the dedicated NeoForge network adapter.
        modEventBus.addListener(
                com.stateworks.neoforge.network.StateworksNetwork::registerCommon
        );

        BLOCKS.register(modEventBus);

        // Supply the NeoForge packet implementation through the Minecraft
        // adapter contract. The state machine itself knows no loader APIs.
        MinecraftStateMachineService.installTransport(StateworksNetwork::sendToLevel);

        NeoForge.EVENT_BUS.register(new StateworksNeoForgeServerEvents());

        BuiltinConditions.register();
        BuiltinStateSets.register();

        modContainer.registerConfig(
                ModConfig.Type.LOCAL,
                NeoForgeConfig.SPEC
        );
    }

    private void commonSetup(
            FMLCommonSetupEvent event
    ) {

        LOGGER.info(
                "HELLO FROM COMMON SETUP"
        );

        if (NeoForgeConfig.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info(
                    "DIRT BLOCK >> {}",
                    BuiltInRegistries.BLOCK.getKey(
                            Blocks.DIRT
                    )
            );
        }

        LOGGER.info(
                "{}{}",
                NeoForgeConfig.MAGIC_NUMBER_INTRODUCTION.get(),
                NeoForgeConfig.MAGIC_NUMBER.getAsInt()
        );

        NeoForgeConfig.ITEM_STRINGS.get().forEach(
                item ->
                        LOGGER.info(
                                "ITEM >> {}",
                                item
                        )
        );
    }

    public static void updateTrackedBlock(
            net.minecraft.world.level.Level level,
            BlockPos pos
    ) {
        MinecraftStateMachineService.updateTrackedBlock(level, pos);
    }


}
