package com.stateworks.minecraft.output;

import com.stateworks.output.OutputDefinition;
import com.stateworks.output.StateOutputTarget;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface StateOutputTargetFactory {

    StateOutputTarget create(
            OutputDefinition definition,
            Level level,
            BlockPos sourcePosition
    );
}