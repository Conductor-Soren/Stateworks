package com.stateworks.output;

import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.network.*;
import com.stateworks.client.*;
import com.stateworks.block.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

public final class StateOutputTargets {

    private StateOutputTargets() {
    }

    public static StateOutputTargetFactory createFactory() {

        return (
                definition,
                level,
                sourcePosition
        ) -> {

            if (definition == null) {
                throw new IllegalArgumentException(
                        "Stateworks output definition cannot be null"
                );
            }

            if (!"block_property".equals(
                    definition.type()
            )) {
                throw new IllegalArgumentException(
                        "Unsupported Stateworks output type: "
                                + definition.type()
                );
            }

            List<Integer> offset =
                    definition.offset();

            BlockPos targetPosition;

            if (definition.relativeToFacing()) {

                BlockState sourceState =
                        level.getBlockState(
                                sourcePosition
                        );

                /*
                 * Not every Minecraft block has a horizontal
                 * facing property.
                 *
                 * For example, logs use "axis" instead.
                 *
                 * If the source block cannot provide the
                 * requested facing information, there is no
                 * valid relative output target.
                 */
                if (!sourceState.hasProperty(
                        BlockStateProperties
                                .HORIZONTAL_FACING
                )) {
                    return null;
                }

                Direction facing =
                        sourceState.getValue(
                                BlockStateProperties
                                        .HORIZONTAL_FACING
                        );

                targetPosition =
                        sourcePosition.relative(
                                facing,
                                offset.get(0)
                        );

                targetPosition =
                        targetPosition.offset(
                                0,
                                offset.get(1),
                                offset.get(2)
                        );

            } else {

                targetPosition =
                        sourcePosition.offset(
                                offset.get(0),
                                offset.get(1),
                                offset.get(2)
                        );
            }

            BlockState targetState =
                    level.getBlockState(
                            targetPosition
                    );

            var property =
                    BlockStatePropertyResolver.tryResolve(
                            targetState,
                            definition.property()
                    );

            /*
             * The target block may not contain the requested
             * property. This is normal for an arbitrary block
             * inspected by the player, so simply don't create
             * an output target.
             */
            if (property == null) {
                return null;
            }

            return new MinecraftBlockOutputTarget(
                    level,
                    targetPosition,
                    property
            );
        };
    }
}