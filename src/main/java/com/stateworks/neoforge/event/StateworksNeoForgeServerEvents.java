package com.stateworks.neoforge.event;

import com.stateworks.minecraft.resource.MinecraftStateDefinitionLoader;
import com.stateworks.minecraft.transition.MinecraftStateMachineService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * NeoForge server event adapter for Stateworks.
 *
 * This class translates NeoForge lifecycle, tick, and block events into calls
 * to the Minecraft state-machine service. It contains no transition logic.
 */
public final class StateworksNeoForgeServerEvents {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public void onAddServerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(
                MinecraftStateDefinitionLoader.ID,
                MinecraftStateDefinitionLoader.INSTANCE
        );
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftStateMachineService.tick(System.currentTimeMillis());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        // Drop retained ServerLevel references before the old server is torn down.
        MinecraftStateMachineService.clear();
    }

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

        // Track the notifying block itself so ordinary state definitions see
        // vanilla property changes such as a furnace changing LIT.
        trackBlock(level, event.getPos());

        // Repeaters are tracked on their input side because POWERED changes later.
        for (Direction direction : event.getNotifiedSides()) {
            BlockPos neighborPos = event.getPos().relative(direction);
            if (level.getBlockState(neighborPos).getBlock() == Blocks.REPEATER) {
                trackBlock(level, neighborPos);
            }
        }
    }

    private static void trackBlock(ServerLevel level, BlockPos pos) {
        MinecraftStateMachineService.updateTrackedBlock(level, pos);
    }
}
