package com.stateworks.condition;

import com.stateworks.core.context.StateContext;

public final class BlockTagCondition implements Condition {

    private final String tagId;

    public BlockTagCondition(String tagId) {
        if (tagId == null || tagId.isBlank()) {
            throw new IllegalArgumentException("Block tag ID cannot be blank");
        }
        this.tagId = tagId;
    }

    @Override
    public boolean evaluate(StateContext context) {
        return context.isInBlockTag(tagId);
    }

    @Override
    public String describe() {
        return "block_tag=" + tagId;
    }
}