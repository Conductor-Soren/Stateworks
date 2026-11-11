package com.stateworks.minecraft.transition;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import com.stateworks.transition.StateMachine;
import com.stateworks.minecraft.network.VisualStateTransport;

/**
 * Owns the Minecraft-backed state-machine manager and exposes the small set
 * of operations needed by event and inspection adapters.
 */
public final class MinecraftStateMachineService {

    private static StateMachineManager manager = new StateMachineManager(null);

    /** Installs the active loader's packet transport before the server starts. */
    public static synchronized void installTransport(VisualStateTransport transport) {
        manager.clear();
        manager = new StateMachineManager(transport);
    }

    private MinecraftStateMachineService() {
    }

    public static StateMachine update(Level level, BlockPos pos, long currentTime) {
        if (level == null || pos == null) {
            return null;
        }
        return manager.update(level, pos, currentTime);
    }

    public static void updateTrackedBlock(Level level, BlockPos pos) {
        if (level == null || pos == null || level.isClientSide()) {
            return;
        }
        manager.update(level, pos, System.currentTimeMillis());
    }

    public static void tick(long currentTime) {
        manager.tick(currentTime);
    }

    public static void clear() {
        manager.clear();
    }

    public static int size() {
        return manager.size();
    }
}
