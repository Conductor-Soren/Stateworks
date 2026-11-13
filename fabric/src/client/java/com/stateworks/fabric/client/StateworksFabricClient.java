package com.stateworks.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.stateworks.client.StateworksBlockInspector;
import com.stateworks.client.StateworksVisualStateManager;
import com.stateworks.minecraft.network.VisualStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class StateworksFabricClient implements ClientModInitializer {
    private static final KeyMapping INSPECT_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.stateworks.inspect",
                    InputConstants.Type.KEYBOARD,
                    InputConstants.KEY_I,
                    KeyMapping.Category.MISC
            )
    );

    @Override
    public void onInitializeClient() {
        FabricVisualModelPlugin.register();
        StateworksVisualStateManager.setVisualDurationResolver(
                FabricVisualModelPlugin::getTransitionDuration
        );

        ClientPlayNetworking.registerGlobalReceiver(
                VisualStatePayload.TYPE,
                (payload, context) -> context.client().execute(() -> applyVisualState(payload))
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            StateworksVisualStateManager.tick();
            while (INSPECT_KEY.consumeClick()) {
                if (client.player == null || client.level == null) {
                    continue;
                }
                HitResult hit = client.hitResult;
                if (hit instanceof BlockHitResult blockHit) {
                    StateworksBlockInspector.inspectBlock(client, blockHit.getBlockPos());
                }
            }
        });
    }

    private static void applyVisualState(VisualStatePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        StateworksVisualStateManager.set(
                minecraft.level.dimension(),
                payload.position(),
                payload.stateSetId(),
                payload.stateName(),
                payload.previousStateName(),
                payload.visualTransitionName(),
                payload.transitionElapsed(),
                payload.transitionDuration(),
                payload.previousSignals(),
                payload.signals()
        );

        if (minecraft.levelRenderer != null) {
            minecraft.levelExtractor.setBlocksDirty(
                    payload.position().getX(),
                    payload.position().getY(),
                    payload.position().getZ(),
                    payload.position().getX(),
                    payload.position().getY(),
                    payload.position().getZ()
            );
        }
    }
}
