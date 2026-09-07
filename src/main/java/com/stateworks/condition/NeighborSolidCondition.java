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

public final class NeighborSolidCondition implements Condition {

    private final Direction direction;

    public NeighborSolidCondition(Direction direction) {
        this.direction = direction;
    }

    @Override
    public boolean evaluate(StateContext context) {
        return context.isSolid(direction);
    }

    @Override
    public String describe() {
        return "neighbor_solid(direction=" + direction.getName() + ")";
    }
}