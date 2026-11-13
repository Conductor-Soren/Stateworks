package com.stateworks.fabric.event;

import com.stateworks.minecraft.transition.MinecraftStateMachineService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import com.stateworks.fabric.StateworksFabric;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Fabric lifecycle bridge. Block changes are queued by LevelMixin and drained on the server thread. */
public final class FabricServerEvents {
    private static final ConcurrentLinkedQueue<BlockUpdate> PENDING = new ConcurrentLinkedQueue<>();

    private FabricServerEvents() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(FabricServerEvents::onEndServerTick);
        ServerLifecycleEvents.SERVER_STOPPING.register(FabricServerEvents::onServerStopping);
    }

    public static void enqueueBlockUpdate(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel && pos != null) {
            BlockStateSnapshot.logIfFurnace(serverLevel, pos, "queued");
            PENDING.add(new BlockUpdate(serverLevel, pos.immutable()));
        }
    }

    private static void onEndServerTick(MinecraftServer server) {
        // Coalesce repeated updates to the same block during one tick.
        Map<BlockUpdateKey, BlockUpdate> updates = new LinkedHashMap<>();
        BlockUpdate update;
        while ((update = PENDING.poll()) != null) {
            if (update.level().getServer() == server) {
                BlockStateSnapshot.logIfFurnace(update.level(), update.position(), "drained");
                updates.put(
                        new BlockUpdateKey(update.level(), update.position()),
                        update
                );
            }
        }

        long now = System.currentTimeMillis();
        for (BlockUpdate pending : updates.values()) {
            MinecraftStateMachineService.updateTrackedBlock(
                    pending.level(),
                    pending.position()
            );
        }
        MinecraftStateMachineService.tick(now);
    }

    private static void onServerStopping(MinecraftServer server) {
        PENDING.removeIf(update -> update.level().getServer() == server);
        MinecraftStateMachineService.clear();
    }

    private static final class BlockStateSnapshot {
        private static void logIfFurnace(ServerLevel level, BlockPos pos, String stage) {
            var state = level.getBlockState(pos);
            if (state.is(Blocks.FURNACE)
                    || state.is(Blocks.BLAST_FURNACE)
                    || state.is(Blocks.SMOKER)) {
                StateworksFabric.LOGGER.info(
                        "[Stateworks TRACE] FabricServerEvents {} furnace update: block={} lit={} pos={}",
                        stage,
                        state.getBlock(),
                        state.hasProperty(net.minecraft.world.level.block.FurnaceBlock.LIT)
                                ? state.getValue(net.minecraft.world.level.block.FurnaceBlock.LIT)
                                : "n/a",
                        pos
                );
            }
        }
    }

    private record BlockUpdate(ServerLevel level, BlockPos position) {
    }

    private record BlockUpdateKey(ServerLevel level, BlockPos position) {
    }
}
