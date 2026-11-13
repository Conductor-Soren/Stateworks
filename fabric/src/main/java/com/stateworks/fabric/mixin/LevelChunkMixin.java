package com.stateworks.fabric.mixin;

import com.stateworks.core.registry.StateRegistry;
import com.stateworks.fabric.event.FabricServerEvents;
import com.stateworks.minecraft.context.MinecraftStateContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Observes block-state writes for blocks registered with Stateworks.
 *
 * The previous implementation filtered this hook to furnace-family blocks.
 * That allowed furnaces, smokers, and blast furnaces to update, but prevented
 * buttons, repeaters, jukeboxes, and any other Stateworks-defined block from
 * ever reaching the server-side state machine.
 */
@Mixin(LevelChunk.class)
abstract class LevelChunkMixin {

    @Inject(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("TAIL")
    )
    private void stateworks$afterSetBlockState(
            BlockPos pos,
            BlockState newState,
            int flags,
            CallbackInfoReturnable<BlockState> callback
    ) {
        Level level = ((LevelChunk) (Object) this).getLevel();
        if (!(level instanceof ServerLevel serverLevel) || pos == null || newState == null) {
            return;
        }

        // Only enqueue blocks for which a built-in or data-driven Stateworks
        // state set currently applies. This keeps ordinary world updates out
        // of the queue while supporting every registered target block.
        MinecraftStateContext context = new MinecraftStateContext(serverLevel, pos);
        String stateSetId = StateRegistry.INSTANCE.findApplicableSet(context);
        if (stateSetId == null) {
            return;
        }

        BlockState oldState = callback.getReturnValue();
        FabricServerEvents.enqueueBlockUpdate(serverLevel, pos);
    }
}
