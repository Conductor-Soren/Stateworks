package com.stateworks.minecraft.network;

import net.minecraft.server.level.ServerLevel;

/**
 * Loader-independent Minecraft transport contract for authoritative visual
 * state updates. A loader adapter supplies the actual packet registration
 * and delivery implementation.
 */
@FunctionalInterface
public interface VisualStateTransport {
    void sendToLevel(ServerLevel level, VisualStatePayload payload);
}
