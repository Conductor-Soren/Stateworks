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

import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.Registries;

public final class BlockTagCondition implements Condition {

    private final TagKey<Block> tag;

    public BlockTagCondition(TagKey<Block> tag) {
        this.tag = tag;
    }

    @Override
    public boolean evaluate(StateContext context) {
        return context.getBlockState().is(tag);
    }

    @Override
    public String describe() {
        return "block_tag=" + tag.location();
    }
}