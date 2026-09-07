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

import net.minecraft.world.level.block.Block;

public final class BlockCondition implements Condition {

    private final Block block;

    public BlockCondition(Block block) {
        this.block = block;
    }

    @Override
    public boolean evaluate(StateContext context) {
        return context.getBlockState().is(block);
    }

    @Override
    public String describe() {
        return "block=" + block;
    }
}