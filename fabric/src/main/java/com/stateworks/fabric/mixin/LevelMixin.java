package com.stateworks.fabric.mixin;

import com.stateworks.fabric.event.FabricServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import com.stateworks.fabric.StateworksFabric;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes successful vanilla block-state writes without changing their behavior. */
@Mixin(Level.class)
abstract class LevelMixin {
    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("TAIL")
    )
    private void stateworks$afterSetBlock(
            BlockPos pos,
            BlockState state,
            int flags,
            int updateLimit,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (callback.getReturnValueZ()) {
            Level level = (Level) (Object) this;
            if (state.is(Blocks.FURNACE)
                    || state.is(Blocks.BLAST_FURNACE)
                    || state.is(Blocks.SMOKER)) {
                StateworksFabric.LOGGER.info(
                        "[Stateworks TRACE] LevelMixin caught furnace write: block={} lit={} pos={}",
                        state.getBlock(),
                        state.hasProperty(net.minecraft.world.level.block.FurnaceBlock.LIT)
                                ? state.getValue(net.minecraft.world.level.block.FurnaceBlock.LIT)
                                : "n/a",
                        pos
                );
            }
            FabricServerEvents.enqueueBlockUpdate(level, pos);
        }
    }
}
