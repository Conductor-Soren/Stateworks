package com.stateworks.condition;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;
import com.stateworks.block.*;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

public final class NeighborBlockCondition implements Condition {

    private final Direction direction;
    private final Block block;

    public NeighborBlockCondition(Direction direction, Block block) {
        this.direction = direction;
        this.block = block;
    }

    @Override
    public boolean evaluate(StateContext context) {
        return context.getNeighbor(direction).getBlock() == block;
    }

    @Override
    public String describe() {
        return "neighbor_block(direction="
                + direction.getName()
                + ", block="
                + block
                + ")";
    }
}