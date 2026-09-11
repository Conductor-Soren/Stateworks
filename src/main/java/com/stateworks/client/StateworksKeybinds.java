package com.stateworks.client;

import com.stateworks.Stateworks;
import com.stateworks.visual.*;

import com.stateworks.context.*;
import com.stateworks.condition.*;
import com.stateworks.state.*;
import com.stateworks.transition.*;
import com.stateworks.signal.*;
import com.stateworks.output.*;
import com.stateworks.network.*;

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
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
        modid = Stateworks.MODID,
        value = Dist.CLIENT
)
public final class StateworksKeybinds {

    public static final KeyMapping INSPECT_KEY =
            new KeyMapping(
                    "key.stateworks.inspect",
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_I,
                    KeyMapping.Category.MISC
            );

    private StateworksKeybinds() {
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
        Minecraft minecraft =
                Minecraft.getInstance();

        while (INSPECT_KEY.consumeClick()) {

            if (minecraft.player == null
                    || minecraft.level == null) {
                continue;
            }

            HitResult hit =
                    minecraft.hitResult;

            if (!(hit instanceof BlockHitResult blockHit)) {
                continue;
            }

            Stateworks.inspectBlock(
                    minecraft,
                    blockHit.getBlockPos()
            );
        }
    }
}