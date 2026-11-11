package com.stateworks.neoforge.network;

import com.stateworks.minecraft.network.VisualStatePayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
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

    /** Sends an authoritative visual-state update to every player in the level. */
    public static void sendToLevel(
            ServerLevel level,
            VisualStatePayload payload
    ) {
        if (level == null || payload == null) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }


}