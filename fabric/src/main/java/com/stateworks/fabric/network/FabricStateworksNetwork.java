package com.stateworks.fabric.network;

import com.stateworks.minecraft.network.VisualStatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class FabricStateworksNetwork {
    private FabricStateworksNetwork() {
    }

    public static void sendToLevel(ServerLevel level, VisualStatePayload payload) {
        if (level == null || payload == null) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
