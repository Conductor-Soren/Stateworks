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
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class NeighborTagCondition implements Condition {

    private final Direction direction;
    private final TagKey<Block> tag;

    public NeighborTagCondition(
            Direction direction,
            TagKey<Block> tag
    ) {
        this.direction = direction;
        this.tag = tag;
    }

    @Override
    public boolean evaluate(StateContext context) {
        return context.getNeighbor(direction).is(tag);
    }

    @Override
    public String describe() {
        return "neighbor_tag(direction="
                + direction.getName()
                + ", tag=" + tag.location()
                + ")";
    }
}