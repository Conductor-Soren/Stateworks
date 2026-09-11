package com.stateworks.network;

import com.stateworks.Stateworks;
import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.client.*;

import com.stateworks.client.StateworksVisualStateManager;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class StateworksNetwork {

    private StateworksNetwork() {
    }

    public static void registerCommon(
            RegisterPayloadHandlersEvent event
    ) {
        System.out.println(
                "STATEWORKS: Registering visual_state payload"
        );

        event.registrar("1")
                .playToClient(
                        VisualStatePayload.TYPE,
                        VisualStatePayload.STREAM_CODEC
                );
    }

    public static void registerClient(
            RegisterClientPayloadHandlersEvent event
    ) {
        System.out.println(
                "STATEWORKS: Registering visual_state client handler"
        );

        event.register(
                VisualStatePayload.TYPE,
                (payload, context) ->
                        context.enqueueWork(() -> {

                            StateworksVisualStateManager.set(
                                    context.player()
                                            .level()
                                            .dimension(),

                                    payload.position(),

                                    payload.stateSetId(),

                                    payload.stateName(),

                                    payload.previousStateName(),

                                    payload.transitionElapsed(),

                                    payload.transitionDuration(),
                                    payload.previousSignals(),
                                    payload.signals()
                            );

                            var minecraft =
                                    net.minecraft.client.Minecraft
                                            .getInstance();

                            if (minecraft.levelRenderer != null) {

                                minecraft.levelRenderer.setBlocksDirty(
                                        payload.position().getX(),
                                        payload.position().getY(),
                                        payload.position().getZ(),
                                        payload.position().getX(),
                                        payload.position().getY(),
                                        payload.position().getZ()
                                );
                            }
                        })
        );
    }
}