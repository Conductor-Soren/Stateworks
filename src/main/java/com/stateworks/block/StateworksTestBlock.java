package com.stateworks.block;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;
import com.stateworks.client.*;


import com.mojang.serialization.MapCodec;
import com.stateworks.Stateworks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class StateworksTestBlock extends Block {

    public static final MapCodec<StateworksTestBlock> CODEC =
            simpleCodec(StateworksTestBlock::new);

    public static final BooleanProperty LIT =
            BooleanProperty.create("lit");

    public StateworksTestBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(LIT, false)
        );
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(LIT);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        /*
         * Establish the current Minecraft BlockState as the
         * Stateworks machine's baseline BEFORE changing it.
         *
         * This is important because StateTransitionTracker
         * treats the first observed state as the baseline
         * and does not create a transition for it.
         */
        Stateworks.updateTrackedBlock(
                level,
                pos
        );

        /*
         * Now change the actual Minecraft BlockState.
         */
        BlockState next =
                state.cycle(LIT);

        level.setBlock(
                pos,
                next,
                Block.UPDATE_ALL
        );

        /*
         * Stateworks now sees the actual state change:
         *
         * off -> active
         * active -> off
         *
         * and creates the configured 500 ms transition.
         */
        Stateworks.updateTrackedBlock(
                level,
                pos
        );

        return InteractionResult.CONSUME;
    }
}