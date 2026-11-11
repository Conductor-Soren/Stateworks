package com.stateworks.client;

import com.stateworks.core.registry.StateRegistry;
import com.stateworks.minecraft.context.MinecraftStateContext;
import com.stateworks.minecraft.transition.MinecraftStateMachineService;
import com.stateworks.output.StateOutput;
import com.stateworks.state.VirtualState;
import com.stateworks.transition.StateMachine;
import com.stateworks.transition.StateTransition;
import com.stateworks.visual.StateVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Client-only block inspection and diagnostic output. */
public final class StateworksBlockInspector {

    private StateworksBlockInspector() {
    }

    public static void inspectBlock(
            Minecraft minecraft,
            net.minecraft.core.BlockPos pos
    ) {

        if (minecraft.level == null
                || minecraft.player == null) {
            return;
        }

        BlockState state =
                minecraft.level.getBlockState(pos);

        StateInspector.inspect(
                state,
                minecraft.player
        );

        StateInspector.inspectPossibleStates(
                state,
                minecraft.player
        );

        MinecraftStateContext context =
                new MinecraftStateContext(
                        minecraft.level,
                        pos
                );

        long currentTime =
                System.currentTimeMillis();

        StateMachine machine =
                MinecraftStateMachineService.update(
                        minecraft.level,
                        pos,
                        currentTime
                );

        /*
         * A block that does not match the configured Stateworks
         * state set does not get a machine.
         */
        if (machine == null) {

            System.out.println(
                    "Virtual State: <not applicable>"
            );

            System.out.println(
                    "========== STATEWORKS MACHINE =========="
            );

            System.out.println(
                    "Stateworks Machine: <not applicable>"
            );

            System.out.println(
                    "Tracked Machines: "
                            + MinecraftStateMachineService.size()
            );

            System.out.println(
                    "========================================"
            );

        } else {

            VirtualState<?> result =
                    machine.desiredState();

            if (result != null) {

                StateVisual visual =
                        StateRegistry.INSTANCE.getSet(machine.stateSetId()) != null
                                ? StateRegistry.INSTANCE.getSet(machine.stateSetId()).visual(result.name())
                                : null;

                System.out.println(
                        "Visual Model: "
                                + (
                                visual != null
                                        ? visual.model()
                                        : "<none>"
                        )
                );

                System.out.println(
                        "Virtual State: "
                                + result.name()
                                + " = "
                                + result.value()
                );

            } else {

                System.out.println(
                        "Virtual State: <not applicable>"
                );
            }

            System.out.println(
                    "========== STATEWORKS MACHINE =========="
            );

            System.out.println(
                    "Dimension: "
                            + minecraft.level.dimension()
            );

            System.out.println(
                    "Position: "
                            + pos
            );

            System.out.println(
                    "Tracked Machines: "
                            + MinecraftStateMachineService.size()
            );

            System.out.println(
                    "========================================"
            );

            StateTransition transition =
                    machine.transition();

            if (transition != null) {

                System.out.println(
                        "========== STATEWORKS TRANSITION =========="
                );

                System.out.println(
                        "Previous: "
                                + transition.previous().name()
                                + " = "
                                + transition.previous().value()
                );

                System.out.println(
                        "Next: "
                                + transition.next().name()
                                + " = "
                                + transition.next().value()
                );

                System.out.println(
                        "Progress: "
                                + transition.getProgress(
                                currentTime
                        )
                );

                System.out.println(
                        "Complete: "
                                + transition.isComplete(
                                currentTime
                        )
                );

                System.out.println(
                        "=========================================="
                );

                Object interpolated =
                        machine.interpolatedValue(
                                currentTime
                        );

                System.out.println(
                        "Interpolated: "
                                + interpolated
                                + " | progress="
                                + transition.getProgress(
                                currentTime
                        )
                );
            }
        }

        System.out.println(
                "========== STATEWORKS CONTEXT =========="
        );

        System.out.println(
                "Block: "
                        + context.getBlockState().getBlock()
        );

        System.out.println(
                "========================================"
        );

        System.out.println(
                "========== STATEWORKS OUTPUT =========="
        );

        if (machine == null) {

            System.out.println(
                    "Output State: <not applicable>"
            );

            System.out.println(
                    "Output Value: <not applicable>"
            );

            System.out.println(
                    "Output Active: false"
            );

        } else {

            StateOutput output =
                    machine.output();

            VirtualState<?> outputState =
                    output.state(currentTime);

            Object outputValue =
                    output.mappedValue(currentTime);

            boolean active =
                    output.isActive(currentTime);

            System.out.println(
                    "Output State: "
                            + (
                            outputState != null
                                    ? outputState.name()
                                    : "null"
                    )
            );

            System.out.println(
                    "Output Value: "
                            + outputValue
            );

            System.out.println(
                    "Output Active: "
                            + active
            );
        }

        System.out.println(
                "======================================="
        );
    }
}
