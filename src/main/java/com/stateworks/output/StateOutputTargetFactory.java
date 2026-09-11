package com.stateworks.output;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.network.*;
import com.stateworks.client.*;

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