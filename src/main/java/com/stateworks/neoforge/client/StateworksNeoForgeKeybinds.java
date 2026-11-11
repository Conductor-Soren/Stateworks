
package com.stateworks.neoforge.client;

import com.stateworks.neoforge.StateworksNeoForge;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(
        modid = StateworksNeoForge.MODID,
        value = Dist.CLIENT
)
public final class StateworksNeoForgeKeybinds {

    public static final KeyMapping INSPECT_KEY =
            new KeyMapping(
                    "key.stateworks.inspect",
                    InputConstants.KEY_I,
                    KeyMapping.Category.MISC
            );

    private StateworksNeoForgeKeybinds() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(
            RegisterKeyMappingsEvent event
    ) {
        event.register(INSPECT_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(
            ClientTickEvent.Post event
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        while (INSPECT_KEY.consumeClick()) {

            if (minecraft.player == null
                    || minecraft.level == null) {
                continue;
            }

            HitResult hit = minecraft.hitResult;

            if (!(hit instanceof BlockHitResult blockHit)) {
                continue;
            }

            com.stateworks.client.StateworksBlockInspector.inspectBlock(
                    minecraft,
                    blockHit.getBlockPos()
            );
        }
    }
}
