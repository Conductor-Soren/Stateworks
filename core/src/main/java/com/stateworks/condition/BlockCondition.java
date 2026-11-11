package com.stateworks.condition;

import com.stateworks.core.context.StateContext;

public final class BlockCondition implements Condition {

    private final String blockId;

    public BlockCondition(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            throw new IllegalArgumentException("Block condition ID cannot be blank");
        }
        this.blockId = blockId;
    }

    @Override
    public boolean evaluate(StateContext context) {
        return blockId.equals(context.blockId());
    }

    @Override
    public String describe() {
        return "block=" + blockId;
    }
}