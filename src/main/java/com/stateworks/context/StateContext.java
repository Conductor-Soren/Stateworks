package com.stateworks.context;

import com.stateworks.visual.*;

import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;
import com.stateworks.block.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class StateContext {

    private final Level level;
    private final BlockPos pos;
    private final BlockState state;

    public StateContext(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
        this.state = level.getBlockState(pos);
    }

    public Level getLevel() {
        return level;
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getBlockState() {
        return state;
    }

    public BlockState getNeighbor(Direction direction) {
        return level.getBlockState(pos.relative(direction));
    }

    public boolean isSolid(Direction direction) {
        return getNeighbor(direction).isSolid();
    }

    public boolean isAir(Direction direction) {
        return getNeighbor(direction).isAir();
    }

    public boolean hasNeighbor(Direction direction) {
        return !isAir(direction);
    }
}